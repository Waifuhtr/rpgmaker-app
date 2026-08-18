<?php
/**
 * REST uçları: /wp-json/pixelstore/v2/...
 *
 * Yetki kuralları:
 *   - `health` herkese açık (bağlantı sınaması).
 *   - Diğer her uç oturum ister; jeton bir WordPress kullanıcısına bağlıdır.
 *   - Yönetim uçları ayrıca `manage_options`/`edit_others_posts` yeteneği ister.
 *   - Kullanıcı rolünde taslak kayıtlar listelere hiç girmez.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_REST {

	/** İstek başına bir kez doğrula: her uçta jetonu yeniden çözmeyelim. */
	private static $current_user = null;

	public static function init() {
		add_action( 'rest_api_init', array( __CLASS__, 'register_routes' ) );
	}

	public static function register_routes() {
		$ns = PSB_NAMESPACE;

		self::route( $ns, '/health', 'GET', 'open', 'health' );
		self::route( $ns, '/auth/login', 'POST', 'open', 'login' );
		self::route( $ns, '/auth/logout', 'POST', 'open', 'logout' );

		self::route( $ns, '/auth/me', 'GET', 'session', 'me' );
		self::route( $ns, '/auth/avatar', 'POST', 'session', 'upload_avatar' );
		self::route( $ns, '/auth/avatar', 'DELETE', 'session', 'clear_avatar' );

		self::route( $ns, '/games', 'GET', 'session', 'list_games' );
		self::route( $ns, '/games', 'POST', 'admin', 'create_game' );
		self::route( $ns, '/search', 'GET', 'session', 'search' );
		self::route( $ns, '/taxonomies', 'GET', 'session', 'taxonomies' );
		self::route( $ns, '/favorites', 'GET', 'session', 'favorites' );

		$id = '(?P<id>[A-Za-z0-9\-_%\.]+)';
		self::route( $ns, "/games/$id", 'GET', 'session', 'get_game' );
		self::route( $ns, "/games/$id", 'PUT, PATCH', 'admin', 'update_game' );
		self::route( $ns, "/games/$id", 'DELETE', 'admin', 'delete_game' );
		self::route( $ns, "/games/$id/published", 'POST', 'admin', 'set_published' );
		self::route( $ns, "/games/$id/download", 'POST', 'session', 'download' );
		self::route( $ns, "/games/$id/view", 'POST', 'session', 'view' );
		self::route( $ns, "/games/$id/favorite", 'POST', 'session', 'toggle_favorite' );
		self::route( $ns, "/games/$id/rate", 'POST', 'session', 'rate' );
		self::route( $ns, "/games/$id/reviews", 'GET', 'session', 'list_reviews' );
		self::route( $ns, "/games/$id/reviews", 'POST', 'session', 'add_review' );
		self::route( $ns, "/games/$id/report", 'POST', 'session', 'add_report' );
		self::route( $ns, "/games/$id/cover", 'POST', 'admin', 'upload_cover' );
		self::route( $ns, "/games/$id/screenshots", 'POST', 'admin', 'upload_screenshot' );
		self::route( $ns, "/games/$id/screenshots/(?P<attachment>[0-9]+)", 'DELETE', 'admin', 'delete_screenshot' );

		self::route( $ns, '/reviews/(?P<review>[0-9]+)', 'DELETE', 'session', 'delete_review' );
		self::route( $ns, '/reviews/(?P<review>[0-9]+)/vote', 'POST', 'session', 'vote_review' );

		self::route( $ns, '/stats', 'GET', 'admin', 'stats' );
		self::route( $ns, '/users', 'GET', 'admin', 'users' );
	}

	private static function route( $namespace, $path, $methods, $access, $callback ) {
		register_rest_route(
			$namespace,
			$path,
			array(
				'methods'             => $methods,
				'callback'            => array( __CLASS__, $callback ),
				'permission_callback' => array( __CLASS__, 'permission_' . $access ),
			)
		);
	}

	/* ---- İzin geri çağrıları ------------------------------------------------------------------ */

	public static function permission_open() {
		return true;
	}

	public static function permission_session( WP_REST_Request $request ) {
		$user = PSB_Auth::authenticate( $request );
		if ( is_wp_error( $user ) ) {
			return $user;
		}
		self::$current_user = $user;
		return true;
	}

	public static function permission_admin( WP_REST_Request $request ) {
		$user = PSB_Auth::authenticate( $request );
		if ( is_wp_error( $user ) ) {
			return $user;
		}
		if ( ! PSB_Auth::is_admin( $user ) ) {
			return new WP_Error( 'psb_forbidden', 'Bu işlem için yönetici yetkisi gerekir.', array( 'status' => 403 ) );
		}
		self::$current_user = $user;
		return true;
	}

	private static function user() {
		return self::$current_user;
	}

	private static function user_id() {
		$user = self::user();
		return $user ? (int) $user->ID : 0;
	}

	private static function is_admin_request() {
		$user = self::user();
		return $user ? PSB_Auth::is_admin( $user ) : false;
	}

	private static function ok( array $data = array() ) {
		return rest_ensure_response( array_merge( array( 'ok' => true ), $data ) );
	}

	private static function body( WP_REST_Request $request ) {
		$json = $request->get_json_params();
		if ( is_array( $json ) ) {
			return $json;
		}
		$params = $request->get_body_params();
		return is_array( $params ) ? $params : array();
	}

	/** Yol parametresindeki oyunu bulur; yoksa 404. */
	private static function require_game( WP_REST_Request $request ) {
		$post = PSB_Query::find( urldecode( (string) $request['id'] ), self::is_admin_request() );
		if ( ! $post ) {
			return new WP_Error( 'psb_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		return $post;
	}

	private static function file( WP_REST_Request $request ) {
		$files = $request->get_file_params();
		if ( empty( $files ) ) {
			return null;
		}
		// Alan adı "file" bekleniyor; farklı isim geldiyse ilk dosyayı al.
		return isset( $files['file'] ) ? $files['file'] : reset( $files );
	}

	/* ---- Açık uçlar --------------------------------------------------------------------------- */

	public static function health() {
		$theme_ok = psb_theme_active();
		return self::ok(
			array(
				'plugin'      => PSB_VERSION,
				'site'        => get_bloginfo( 'name' ),
				'themeActive' => $theme_ok,
				'games'       => $theme_ok ? (int) wp_count_posts( PSB_POST_TYPE )->publish : 0,
				'notice'      => $theme_ok ? '' : 'SteamLike teması etkin değil; oyun kayıtları okunamıyor.',
			)
		);
	}

	public static function login( WP_REST_Request $request ) {
		$body   = self::body( $request );
		$result = PSB_Auth::login( $body['username'] ?? '', $body['password'] ?? '' );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	public static function logout( WP_REST_Request $request ) {
		PSB_Auth::logout( $request );
		return self::ok();
	}

	/* ---- Oturum ------------------------------------------------------------------------------- */

	public static function me() {
		return self::ok( array( 'session' => PSB_Auth::session_payload( self::user() ) ) );
	}

	public static function upload_avatar( WP_REST_Request $request ) {
		$result = PSB_Media::set_avatar( self::user(), self::file( $request ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result + array( 'session' => PSB_Auth::session_payload( self::user() ) ) );
	}

	public static function clear_avatar() {
		$result = PSB_Media::clear_avatar( self::user() );
		return self::ok( $result + array( 'session' => PSB_Auth::session_payload( self::user() ) ) );
	}

	/* ---- Oyunlar ------------------------------------------------------------------------------ */

	public static function list_games( WP_REST_Request $request ) {
		if ( ! psb_theme_active() ) {
			return new WP_Error( 'psb_no_theme', 'SteamLike teması etkin değil.', array( 'status' => 503 ) );
		}
		return self::ok( PSB_Query::games( $request, self::is_admin_request(), self::user_id() ) );
	}

	public static function get_game( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		return self::ok( array( 'game' => PSB_Mapper::detail( $post, self::user_id() ) ) );
	}

	public static function search( WP_REST_Request $request ) {
		return self::ok( array( 'results' => PSB_Query::live_search( $request->get_param( 'q' ), $request->get_param( 'limit' ) ) ) );
	}

	public static function taxonomies() {
		return self::ok( array( 'taxonomies' => PSB_Query::taxonomies() ) );
	}

	public static function create_game( WP_REST_Request $request ) {
		$result = PSB_Write::save( self::body( $request ), null, self::user_id() );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( array( 'game' => $result ) );
	}

	public static function update_game( WP_REST_Request $request ) {
		$result = PSB_Write::save( self::body( $request ), urldecode( (string) $request['id'] ), self::user_id() );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( array( 'game' => $result ) );
	}

	public static function delete_game( WP_REST_Request $request ) {
		$result = PSB_Write::delete( urldecode( (string) $request['id'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok();
	}

	public static function set_published( WP_REST_Request $request ) {
		$body   = self::body( $request );
		$result = PSB_Write::set_published( urldecode( (string) $request['id'] ), ! empty( $body['published'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( array( 'published' => $result ) );
	}

	/* ---- Etkileşim ---------------------------------------------------------------------------- */

	public static function download( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$body   = self::body( $request );
		$result = PSB_Social::record_download( $post->ID, ! empty( $body['mirror'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	public static function view( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		return self::ok( array( 'viewCount' => PSB_Social::record_view( $post->ID ) ) );
	}

	public static function toggle_favorite( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		return self::ok( PSB_Social::toggle_favorite( self::user_id(), $post->ID ) );
	}

	public static function favorites() {
		return self::ok( array( 'games' => PSB_Social::favorite_games( self::user_id() ) ) );
	}

	public static function rate( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$body   = self::body( $request );
		$result = PSB_Social::rate( self::user_id(), $post->ID, $body['rating'] ?? 0 );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	public static function list_reviews( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		return self::ok(
			array(
				'reviews'     => PSB_Social::reviews( $post->ID, self::user_id() ),
				'hasReviewed' => PSB_Social::has_reviewed( self::user_id(), $post->ID ),
			)
		);
	}

	public static function add_review( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$body   = self::body( $request );
		$result = PSB_Social::add_review(
			self::user(),
			$post->ID,
			$body['content'] ?? '',
			! empty( $body['recommended'] )
		);
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok(
			array(
				'reviews' => PSB_Social::reviews( $post->ID, self::user_id() ),
			)
		);
	}

	public static function delete_review( WP_REST_Request $request ) {
		$result = PSB_Social::delete_review( self::user(), (int) $request['review'] );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok();
	}

	public static function vote_review( WP_REST_Request $request ) {
		$body      = self::body( $request );
		$direction = ( 'down' === ( $body['direction'] ?? 'up' ) ) ? 'down' : 'up';
		$result    = PSB_Social::vote_review( self::user_id(), (int) $request['review'], $direction );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	public static function add_report( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$body   = self::body( $request );
		$result = PSB_Social::add_report( self::user(), $post->ID, $body['message'] ?? '' );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	/* ---- Medya (yönetici) --------------------------------------------------------------------- */

	public static function upload_cover( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$result = PSB_Media::set_cover( $post->ID, self::file( $request ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result );
	}

	public static function upload_screenshot( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$result = PSB_Media::add_screenshot( $post->ID, self::file( $request ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( $result + array( 'game' => PSB_Mapper::detail( get_post( $post->ID ), self::user_id() ) ) );
	}

	public static function delete_screenshot( WP_REST_Request $request ) {
		$post = self::require_game( $request );
		if ( is_wp_error( $post ) ) {
			return $post;
		}
		$result = PSB_Media::remove_screenshot( $post->ID, (int) $request['attachment'] );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return self::ok( array( 'game' => PSB_Mapper::detail( get_post( $post->ID ), self::user_id() ) ) );
	}

	/* ---- Yönetim ------------------------------------------------------------------------------ */

	public static function stats() {
		return self::ok( array( 'stats' => PSB_Query::stats() ) );
	}

	public static function users( WP_REST_Request $request ) {
		return self::ok( array( 'users' => PSB_Query::users( $request->get_param( 'limit' ) ?: 100 ) ) );
	}
}
