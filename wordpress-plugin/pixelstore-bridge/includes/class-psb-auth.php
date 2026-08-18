<?php
/**
 * Jeton tabanlı oturum ve profil fotoğrafı.
 *
 * Kullanıcı veritabanı sitenin kendi WordPress kullanıcı tablosudur; bu eklenti parola saklamaz.
 * Android istemcisi çerez taşımadığı için oturum jetonla yürür: jetonun yalnızca SHA-256 özeti
 * kullanıcı metasında durur.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Auth {

	const META_TOKENS = '_psb_tokens';
	const TTL         = 2592000; // 30 gün
	const MAX_TOKENS  = 5;       // kullanıcı başına eşzamanlı cihaz

	/**
	 * @return array|WP_Error
	 */
	public static function login( $username, $password ) {
		$username = trim( (string) $username );
		if ( '' === $username || '' === (string) $password ) {
			return new WP_Error( 'psb_missing', 'Kullanıcı adı ve parola gerekli.', array( 'status' => 400 ) );
		}

		$user = wp_authenticate( $username, $password );
		if ( is_wp_error( $user ) ) {
			// Tek tip mesaj: kullanıcı adının var olup olmadığını sızdırmayız.
			return new WP_Error( 'psb_bad_credentials', 'Kullanıcı adı veya parola hatalı.', array( 'status' => 401 ) );
		}

		$raw   = bin2hex( random_bytes( 24 ) );
		$store = self::prune( self::tokens( $user->ID ) );
		$store[] = array(
			'hash'    => hash( 'sha256', $raw ),
			'expires' => time() + self::TTL,
		);
		update_user_meta( $user->ID, self::META_TOKENS, array_slice( $store, -self::MAX_TOKENS ) );

		return array(
			'token'   => $user->ID . '.' . $raw,
			'session' => self::session_payload( $user ),
		);
	}

	/**
	 * @return WP_User|WP_Error
	 */
	public static function authenticate( WP_REST_Request $request ) {
		$token = self::extract_token( $request );
		if ( '' === $token ) {
			return new WP_Error( 'psb_no_token', 'Oturum açılmamış.', array( 'status' => 401 ) );
		}

		$parts = explode( '.', $token, 2 );
		if ( 2 !== count( $parts ) || ! ctype_digit( $parts[0] ) ) {
			return new WP_Error( 'psb_bad_token', 'Oturum geçersiz.', array( 'status' => 401 ) );
		}

		$user = get_user_by( 'id', (int) $parts[0] );
		if ( ! $user ) {
			return new WP_Error( 'psb_bad_token', 'Oturum geçersiz.', array( 'status' => 401 ) );
		}

		$needle  = hash( 'sha256', $parts[1] );
		$matched = false;
		foreach ( self::prune( self::tokens( $user->ID ) ) as $entry ) {
			if ( hash_equals( $entry['hash'], $needle ) ) {
				$matched = true;
				break;
			}
		}
		if ( ! $matched ) {
			return new WP_Error( 'psb_expired', 'Oturum süresi dolmuş, yeniden giriş yap.', array( 'status' => 401 ) );
		}

		// İstek boyunca yetenek kontrolleri bu kullanıcıya göre yapılsın.
		wp_set_current_user( $user->ID );
		return $user;
	}

	public static function logout( WP_REST_Request $request ) {
		$token = self::extract_token( $request );
		if ( '' === $token ) {
			return true;
		}
		$parts = explode( '.', $token, 2 );
		if ( 2 !== count( $parts ) || ! ctype_digit( $parts[0] ) ) {
			return true;
		}
		$user_id = (int) $parts[0];
		$needle  = hash( 'sha256', $parts[1] );
		$keep    = array();
		foreach ( self::prune( self::tokens( $user_id ) ) as $entry ) {
			if ( ! hash_equals( $entry['hash'], $needle ) ) {
				$keep[] = $entry;
			}
		}
		update_user_meta( $user_id, self::META_TOKENS, $keep );
		return true;
	}

	/** Yönetim yetkisi: uygulamadaki yönetim paneli bu kullanıcılara açılır. */
	public static function is_admin( WP_User $user ) {
		return user_can( $user, 'manage_options' ) || user_can( $user, 'edit_others_posts' );
	}

	public static function avatar_url( $user_id, $size = 160 ) {
		// Tema kendi avatarını sl_custom_avatar meta'sında tutuyor; önce ona bakılır.
		$custom = get_user_meta( $user_id, 'sl_custom_avatar', true );
		if ( $custom ) {
			return esc_url_raw( $custom );
		}
		return get_avatar_url( $user_id, array( 'size' => $size ) );
	}

	public static function session_payload( WP_User $user ) {
		$favorites = get_user_meta( $user->ID, 'sl_favorites', true );
		$favorites = is_array( $favorites ) ? $favorites : array();

		$reviews = get_comments(
			array(
				'user_id' => $user->ID,
				'type'    => 'review',
				'count'   => true,
				'status'  => 'approve',
			)
		);

		return array(
			'username'      => $user->user_login,
			'displayName'   => $user->display_name ? $user->display_name : $user->user_login,
			'email'         => $user->user_email,
			'role'          => self::is_admin( $user ) ? 'admin' : 'user',
			'isAdmin'       => self::is_admin( $user ),
			'avatarUrl'     => self::avatar_url( $user->ID ),
			'coverUrl'      => (string) get_user_meta( $user->ID, 'sl_cover_image', true ),
			'joinedAt'      => mysql2date( 'Y-m-d', $user->user_registered ),
			'favoriteCount' => count( $favorites ),
			'reviewCount'   => (int) $reviews,
			'badges'        => self::badges( $user->ID ),
		);
	}

	/**
	 * Temanın rozet motoru. Tema etkin değilse boş döner; eklenti rozet mantığını kopyalamaz.
	 */
	public static function badges( $user_id ) {
		if ( ! function_exists( 'sl_get_user_badges' ) ) {
			return array();
		}
		$out = array();
		foreach ( (array) sl_get_user_badges( $user_id ) as $badge ) {
			$out[] = array(
				'name'  => isset( $badge['name'] ) ? (string) $badge['name'] : '',
				'color' => isset( $badge['color'] ) ? (string) $badge['color'] : '#3b82f6',
			);
		}
		return $out;
	}

	/**
	 * Bazı sunucular Authorization başlığını PHP'ye geçirmez; özel başlık ve sorgu parametresi
	 * yedek olarak kabul edilir.
	 */
	private static function extract_token( WP_REST_Request $request ) {
		$header = $request->get_header( 'authorization' );
		if ( $header && preg_match( '/Bearer\s+(.+)/i', $header, $matches ) ) {
			return trim( $matches[1] );
		}
		$custom = $request->get_header( 'x_pixelstore_token' );
		if ( $custom ) {
			return trim( $custom );
		}
		$param = $request->get_param( 'psb_token' );
		return $param ? trim( (string) $param ) : '';
	}

	private static function tokens( $user_id ) {
		$store = get_user_meta( $user_id, self::META_TOKENS, true );
		return is_array( $store ) ? $store : array();
	}

	private static function prune( array $store ) {
		$now = time();
		$out = array();
		foreach ( $store as $entry ) {
			if ( isset( $entry['hash'], $entry['expires'] ) && (int) $entry['expires'] > $now ) {
				$out[] = $entry;
			}
		}
		return $out;
	}
}
