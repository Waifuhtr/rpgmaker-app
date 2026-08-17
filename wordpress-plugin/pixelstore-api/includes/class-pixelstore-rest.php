<?php
/**
 * REST uçları: /wp-json/pixelstore/v1/...
 *
 * Yetki kararı burada verilir. Uygulama "ben yöneticiyim" diyemez; jeton bir WordPress kullanıcısına
 * bağlıdır ve yönetim uçları o kullanıcının yeteneklerini kontrol eder. Kullanıcı rolünde:
 *   - katalog yükünde taslak kayıtlar hiç yer almaz
 *   - yönetim uçları 403 döner
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_REST {

	public static function init() {
		add_action( 'rest_api_init', array( __CLASS__, 'register_routes' ) );
	}

	public static function register_routes() {
		$ns = PIXELSTORE_NAMESPACE;

		// Herkese açık: bağlantı sınaması.
		register_rest_route(
			$ns,
			'/health',
			array(
				'methods'             => 'GET',
				'permission_callback' => '__return_true',
				'callback'            => array( __CLASS__, 'health' ),
			)
		);

		register_rest_route(
			$ns,
			'/auth/login',
			array(
				'methods'             => 'POST',
				'permission_callback' => '__return_true',
				'callback'            => array( __CLASS__, 'login' ),
				'args'                => array(
					'username' => array( 'required' => true ),
					'password' => array( 'required' => true ),
				),
			)
		);

		// Oturum gerektiren uçlar.
		register_rest_route(
			$ns,
			'/auth/me',
			array(
				'methods'             => 'GET',
				'permission_callback' => array( __CLASS__, 'require_session' ),
				'callback'            => array( __CLASS__, 'me' ),
			)
		);

		register_rest_route(
			$ns,
			'/auth/logout',
			array(
				'methods'             => 'POST',
				'permission_callback' => '__return_true',
				'callback'            => array( __CLASS__, 'logout' ),
			)
		);

		register_rest_route(
			$ns,
			'/catalog',
			array(
				'methods'             => 'GET',
				'permission_callback' => array( __CLASS__, 'require_session' ),
				'callback'            => array( __CLASS__, 'catalog' ),
			)
		);

		register_rest_route(
			$ns,
			'/apps',
			array(
				array(
					'methods'             => 'POST',
					'permission_callback' => array( __CLASS__, 'require_admin' ),
					'callback'            => array( __CLASS__, 'create_app' ),
				),
			)
		);

		register_rest_route(
			$ns,
			'/apps/(?P<id>[A-Za-z0-9\-_%]+)',
			array(
				array(
					'methods'             => 'GET',
					'permission_callback' => array( __CLASS__, 'require_session' ),
					'callback'            => array( __CLASS__, 'get_app' ),
				),
				array(
					'methods'             => 'PUT, PATCH',
					'permission_callback' => array( __CLASS__, 'require_admin' ),
					'callback'            => array( __CLASS__, 'update_app' ),
				),
				array(
					'methods'             => 'DELETE',
					'permission_callback' => array( __CLASS__, 'require_admin' ),
					'callback'            => array( __CLASS__, 'delete_app' ),
				),
			)
		);

		register_rest_route(
			$ns,
			'/apps/(?P<id>[A-Za-z0-9\-_%]+)/install',
			array(
				'methods'             => 'POST',
				'permission_callback' => array( __CLASS__, 'require_session' ),
				'callback'            => array( __CLASS__, 'record_install' ),
			)
		);

		register_rest_route(
			$ns,
			'/apps/(?P<id>[A-Za-z0-9\-_%]+)/published',
			array(
				'methods'             => 'POST',
				'permission_callback' => array( __CLASS__, 'require_admin' ),
				'callback'            => array( __CLASS__, 'set_published' ),
			)
		);

		// Yalnızca yönetici.
		register_rest_route(
			$ns,
			'/users',
			array(
				'methods'             => 'GET',
				'permission_callback' => array( __CLASS__, 'require_admin' ),
				'callback'            => array( __CLASS__, 'users' ),
			)
		);

		register_rest_route(
			$ns,
			'/stats',
			array(
				'methods'             => 'GET',
				'permission_callback' => array( __CLASS__, 'require_admin' ),
				'callback'            => array( __CLASS__, 'stats' ),
			)
		);

		register_rest_route(
			$ns,
			'/seed',
			array(
				'methods'             => 'POST',
				'permission_callback' => array( __CLASS__, 'require_admin' ),
				'callback'            => array( __CLASS__, 'seed' ),
			)
		);
	}

	/* ---- İzin geri çağrıları ------------------------------------------------------------------ */

	public static function require_session( WP_REST_Request $request ) {
		$user = PixelStore_Auth::authenticate( $request );
		return is_wp_error( $user ) ? $user : true;
	}

	public static function require_admin( WP_REST_Request $request ) {
		$user = PixelStore_Auth::authenticate( $request );
		if ( is_wp_error( $user ) ) {
			return $user;
		}
		if ( ! PixelStore_Auth::is_admin( $user ) ) {
			return new WP_Error(
				'ps_forbidden',
				'Bu işlem için yönetici yetkisi gerekir.',
				array( 'status' => 403 )
			);
		}
		return true;
	}

	/** İzin geri çağrısı zaten doğruladı; burada kullanıcıyı yeniden alırız. */
	private static function current( WP_REST_Request $request ) {
		$user = PixelStore_Auth::authenticate( $request );
		return is_wp_error( $user ) ? null : $user;
	}

	private static function is_admin_request( WP_REST_Request $request ) {
		$user = self::current( $request );
		return $user && PixelStore_Auth::is_admin( $user );
	}

	/* ---- Uçlar -------------------------------------------------------------------------------- */

	public static function health() {
		$count = (int) wp_count_posts( PIXELSTORE_POST_TYPE )->publish;
		return rest_ensure_response(
			array(
				'ok'     => true,
				'plugin' => PIXELSTORE_VERSION,
				'site'   => get_bloginfo( 'name' ),
				'apps'   => $count,
			)
		);
	}

	public static function login( WP_REST_Request $request ) {
		$result = PixelStore_Auth::login(
			$request->get_param( 'username' ),
			$request->get_param( 'password' )
		);
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array_merge( array( 'ok' => true ), $result ) );
	}

	public static function me( WP_REST_Request $request ) {
		$user = self::current( $request );
		if ( ! $user ) {
			return new WP_Error( 'ps_no_token', 'Oturum açılmamış.', array( 'status' => 401 ) );
		}
		return rest_ensure_response(
			array(
				'ok'      => true,
				'session' => PixelStore_Auth::session_payload( $user ),
			)
		);
	}

	public static function logout( WP_REST_Request $request ) {
		PixelStore_Auth::logout( $request );
		return rest_ensure_response( array( 'ok' => true ) );
	}

	public static function catalog( WP_REST_Request $request ) {
		$is_admin = self::is_admin_request( $request );
		return rest_ensure_response(
			array(
				'ok'         => true,
				'role'       => $is_admin ? 'admin' : 'user',
				'apps'       => PixelStore_Repository::catalog( $is_admin ),
				'categories' => PixelStore_CPT::categories(),
			)
		);
	}

	public static function get_app( WP_REST_Request $request ) {
		$is_admin = self::is_admin_request( $request );
		$post     = PixelStore_Repository::find( urldecode( $request['id'] ), $is_admin );
		if ( ! $post ) {
			return new WP_Error( 'ps_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		return rest_ensure_response(
			array(
				'ok'  => true,
				'app' => PixelStore_Repository::to_payload( $post, $is_admin ),
			)
		);
	}

	public static function create_app( WP_REST_Request $request ) {
		$payload = self::body( $request );
		$result  = PixelStore_Repository::save( $payload, null );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array( 'ok' => true, 'app' => $result ) );
	}

	public static function update_app( WP_REST_Request $request ) {
		$payload = self::body( $request );
		$result  = PixelStore_Repository::save( $payload, urldecode( $request['id'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array( 'ok' => true, 'app' => $result ) );
	}

	public static function delete_app( WP_REST_Request $request ) {
		$result = PixelStore_Repository::delete( urldecode( $request['id'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array( 'ok' => true ) );
	}

	public static function record_install( WP_REST_Request $request ) {
		$result = PixelStore_Repository::record_install( urldecode( $request['id'] ) );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array( 'ok' => true, 'installs' => $result ) );
	}

	public static function set_published( WP_REST_Request $request ) {
		$body      = self::body( $request );
		$published = ! empty( $body['published'] );
		$result    = PixelStore_Repository::set_published( urldecode( $request['id'] ), $published );
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		return rest_ensure_response( array( 'ok' => true, 'published' => $result ) );
	}

	public static function users() {
		return rest_ensure_response(
			array(
				'ok'    => true,
				'users' => PixelStore_Repository::users(),
			)
		);
	}

	public static function stats() {
		return rest_ensure_response(
			array(
				'ok'    => true,
				'stats' => PixelStore_Repository::stats(),
			)
		);
	}

	public static function seed() {
		$count = PixelStore_Seed::install( true );
		return rest_ensure_response(
			array(
				'ok'      => true,
				'seeded'  => $count,
				'message' => "$count kayıt yüklendi.",
			)
		);
	}

	private static function body( WP_REST_Request $request ) {
		$json = $request->get_json_params();
		if ( is_array( $json ) ) {
			return $json;
		}
		$params = $request->get_body_params();
		return is_array( $params ) ? $params : array();
	}
}
