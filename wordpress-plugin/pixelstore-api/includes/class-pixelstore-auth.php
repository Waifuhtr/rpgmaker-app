<?php
/**
 * Jeton tabanlı oturum.
 *
 * Neden çerez/nonce değil: Android istemcisi tarayıcı değil, çerez oturumu taşımıyor. Neden hazır
 * bir JWT eklentisi değil: bağımlılık eklemeden, WordPress'in kendi kullanıcı tablosuna dayanan
 * küçük bir çözüm yeterli.
 *
 * Jeton biçimi: "<kullanıcı_id>.<rastgele>". Sunucuda yalnızca rastgele kısmın SHA-256 özeti
 * kullanıcı metasında saklanır; jetonun kendisi veritabanında düz metin olarak durmaz.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_Auth {

	const META_KEY   = '_ps_tokens';
	const TTL        = 2592000; // 30 gün
	const MAX_TOKENS = 5;       // kullanıcı başına eşzamanlı cihaz

	/**
	 * Kullanıcı adı/parola doğrular ve yeni jeton üretir.
	 *
	 * @return array|WP_Error
	 */
	public static function login( $username, $password ) {
		$username = trim( (string) $username );
		if ( '' === $username || '' === (string) $password ) {
			return new WP_Error( 'ps_missing', 'Kullanıcı adı ve parola gerekli.', array( 'status' => 400 ) );
		}

		$user = wp_authenticate( $username, $password );
		if ( is_wp_error( $user ) ) {
			// WordPress'in ayrıntılı hatası kullanıcı adının varlığını sızdırabilir; tek mesaj döneriz.
			return new WP_Error( 'ps_bad_credentials', 'Kullanıcı adı veya parola hatalı.', array( 'status' => 401 ) );
		}

		$raw   = bin2hex( random_bytes( 24 ) );
		$store = self::tokens( $user->ID );

		$store[] = array(
			'hash'    => hash( 'sha256', $raw ),
			'expires' => time() + self::TTL,
		);

		// En yeni MAX_TOKENS jetonu tut; eskiler düşer.
		$store = array_slice( self::prune( $store ), -self::MAX_TOKENS );
		update_user_meta( $user->ID, self::META_KEY, $store );

		return array(
			'token'   => $user->ID . '.' . $raw,
			'session' => self::session_payload( $user ),
		);
	}

	/**
	 * İstekteki jetonu doğrular.
	 *
	 * @return WP_User|WP_Error
	 */
	public static function authenticate( WP_REST_Request $request ) {
		$token = self::extract_token( $request );
		if ( '' === $token ) {
			return new WP_Error( 'ps_no_token', 'Oturum açılmamış.', array( 'status' => 401 ) );
		}

		$parts = explode( '.', $token, 2 );
		if ( 2 !== count( $parts ) || ! ctype_digit( $parts[0] ) ) {
			return new WP_Error( 'ps_bad_token', 'Oturum geçersiz.', array( 'status' => 401 ) );
		}

		$user = get_user_by( 'id', (int) $parts[0] );
		if ( ! $user ) {
			return new WP_Error( 'ps_bad_token', 'Oturum geçersiz.', array( 'status' => 401 ) );
		}

		$needle  = hash( 'sha256', $parts[1] );
		$store   = self::prune( self::tokens( $user->ID ) );
		$matched = false;
		foreach ( $store as $entry ) {
			if ( hash_equals( $entry['hash'], $needle ) ) {
				$matched = true;
				break;
			}
		}

		if ( ! $matched ) {
			return new WP_Error( 'ps_bad_token', 'Oturum süresi dolmuş, yeniden giriş yap.', array( 'status' => 401 ) );
		}

		// REST isteği boyunca yetenek kontrolleri bu kullanıcıya göre yapılsın.
		wp_set_current_user( $user->ID );
		return $user;
	}

	/** Jetonu iptal eder (çıkış). */
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
		$store   = array();
		foreach ( self::prune( self::tokens( $user_id ) ) as $entry ) {
			if ( ! hash_equals( $entry['hash'], $needle ) ) {
				$store[] = $entry;
			}
		}
		update_user_meta( $user_id, self::META_KEY, $store );
		return true;
	}

	/** Yönetici mi? WordPress yeteneklerinden karar verilir, istemcinin iddiasından değil. */
	public static function is_admin( WP_User $user ) {
		return user_can( $user, 'manage_options' ) || user_can( $user, 'edit_others_posts' );
	}

	public static function session_payload( WP_User $user ) {
		$name = $user->display_name ? $user->display_name : $user->user_login;
		return array(
			'username'   => $user->user_login,
			'displayName' => $name,
			'role'       => self::is_admin( $user ) ? 'admin' : 'user',
			'avatarSeed' => 'wp-' . $user->ID . '-' . substr( md5( $user->user_login ), 0, 6 ),
			'isAdmin'    => self::is_admin( $user ),
		);
	}

	/**
	 * Jetonu istekten çıkarır.
	 *
	 * Bazı sunucular `Authorization` başlığını PHP'ye geçirmiyor; bu yüzden özel başlık ve sorgu
	 * parametresi de kabul edilir (yalnızca https üzerinden kullanılması önerilir).
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
		$param = $request->get_param( 'ps_token' );
		return $param ? trim( (string) $param ) : '';
	}

	private static function tokens( $user_id ) {
		$store = get_user_meta( $user_id, self::META_KEY, true );
		return is_array( $store ) ? $store : array();
	}

	/** Süresi geçmiş jetonları atar. */
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
