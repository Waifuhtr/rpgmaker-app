<?php
/**
 * PixelStore_Auth için WordPress'siz duman testi.
 *
 * Jeton üretimi/doğrulama/iptali ve süre dolumu, güvenliğin en kritik parçası. Bu betik gereken
 * WordPress fonksiyonlarını taklit ederek mantığı doğrular; tam bir WordPress kurulumu gerekmez.
 *
 * Çalıştırma: php wordpress-plugin/tests/auth-smoke.php
 */

define( 'ABSPATH', __DIR__ );

$GLOBALS['ps_user_meta'] = array();
$GLOBALS['ps_users']     = array();
$GLOBALS['ps_current']   = 0;

class WP_Error {
	public $code;
	public $message;
	public $data;
	public function __construct( $code = '', $message = '', $data = array() ) {
		$this->code    = $code;
		$this->message = $message;
		$this->data    = $data;
	}
	public function get_error_message() {
		return $this->message;
	}
}

function is_wp_error( $thing ) {
	return $thing instanceof WP_Error;
}

class WP_User {
	public $ID;
	public $user_login;
	public $display_name;
	public $caps;
	public function __construct( $id, $login, $name, $caps = array() ) {
		$this->ID           = $id;
		$this->user_login   = $login;
		$this->display_name = $name;
		$this->caps         = $caps;
	}
}

class WP_REST_Request {
	private $headers = array();
	private $params  = array();
	public function __construct( array $headers = array(), array $params = array() ) {
		$this->headers = $headers;
		$this->params  = $params;
	}
	public function get_header( $key ) {
		$key = strtolower( str_replace( '-', '_', $key ) );
		return $this->headers[ $key ] ?? null;
	}
	public function get_param( $key ) {
		return $this->params[ $key ] ?? null;
	}
}

function get_user_meta( $user_id, $key, $single = false ) {
	return $GLOBALS['ps_user_meta'][ $user_id ][ $key ] ?? '';
}

function update_user_meta( $user_id, $key, $value ) {
	$GLOBALS['ps_user_meta'][ $user_id ][ $key ] = $value;
	return true;
}

function get_user_by( $field, $value ) {
	return $GLOBALS['ps_users'][ $value ] ?? false;
}

function wp_set_current_user( $id ) {
	$GLOBALS['ps_current'] = $id;
}

function user_can( $user, $cap ) {
	return in_array( $cap, $user->caps, true );
}

function wp_authenticate( $username, $password ) {
	foreach ( $GLOBALS['ps_users'] as $user ) {
		if ( $user->user_login === $username && 'dogru-parola' === $password ) {
			return $user;
		}
	}
	return new WP_Error( 'invalid', 'hata' );
}

require_once __DIR__ . '/../pixelstore-api/includes/class-pixelstore-auth.php';

/* ---- Test altyapısı --------------------------------------------------------------------------- */

$passed = 0;
$failed = 0;

function check( $label, $condition ) {
	global $passed, $failed;
	if ( $condition ) {
		++$passed;
		echo "  ok   $label\n";
	} else {
		++$failed;
		echo "  FAIL $label\n";
	}
}

$GLOBALS['ps_users'][7]  = new WP_User( 7, 'admin', 'Yönetici', array( 'manage_options', 'edit_others_posts' ) );
$GLOBALS['ps_users'][11] = new WP_User( 11, 'user', 'Gezgin', array( 'read' ) );

echo "PixelStore_Auth duman testi\n";

/* 1. Hatalı parola reddedilir ve kullanıcı adının varlığını sızdırmaz. */
$bad = PixelStore_Auth::login( 'admin', 'yanlis' );
check( 'hatalı parola WP_Error döner', is_wp_error( $bad ) );
check( 'hata mesajı kullanıcı adının varlığını sızdırmıyor', 'Kullanıcı adı veya parola hatalı.' === $bad->message );

$missing = PixelStore_Auth::login( 'olmayan', 'dogru-parola' );
check( 'olmayan kullanıcı aynı mesajı alır', is_wp_error( $missing ) && $missing->message === $bad->message );

/* 2. Doğru parola jeton üretir. */
$ok = PixelStore_Auth::login( 'admin', 'dogru-parola' );
check( 'doğru parola jeton döner', is_array( $ok ) && ! empty( $ok['token'] ) );
check( 'jeton "<id>.<rastgele>" biçiminde', (bool) preg_match( '/^7\.[0-9a-f]{48}$/', $ok['token'] ) );
check( 'oturum yükü admin rolünü taşır', 'admin' === $ok['session']['role'] && true === $ok['session']['isAdmin'] );

/* 3. Sunucuda düz jeton saklanmıyor. */
$stored = get_user_meta( 7, '_ps_tokens', true );
$raw    = explode( '.', $ok['token'], 2 )[1];
$dump   = wp_json_encode_fallback( $stored );
check( 'meta içinde düz jeton yok', false === strpos( $dump, $raw ) );
check( 'meta içinde SHA-256 özeti var', false !== strpos( $dump, hash( 'sha256', $raw ) ) );

/* 4. Jeton doğrulaması. */
$request = new WP_REST_Request( array( 'authorization' => 'Bearer ' . $ok['token'] ) );
$user    = PixelStore_Auth::authenticate( $request );
check( 'geçerli jeton kullanıcıyı döner', $user instanceof WP_User && 7 === $user->ID );

$tampered = new WP_REST_Request( array( 'authorization' => 'Bearer 7.' . str_repeat( 'a', 48 ) ) );
check( 'uydurma jeton reddedilir', is_wp_error( PixelStore_Auth::authenticate( $tampered ) ) );

$wrongUser = new WP_REST_Request( array( 'authorization' => 'Bearer 11.' . $raw ) );
check( 'jeton başka kullanıcıya devredilemez', is_wp_error( PixelStore_Auth::authenticate( $wrongUser ) ) );

$malformed = new WP_REST_Request( array( 'authorization' => 'Bearer bozuk' ) );
check( 'bozuk biçim reddedilir', is_wp_error( PixelStore_Auth::authenticate( $malformed ) ) );

$noToken = new WP_REST_Request();
check( 'jeton yokken reddedilir', is_wp_error( PixelStore_Auth::authenticate( $noToken ) ) );

/* 5. Authorization başlığı düşerse yedek başlık çalışır. */
$fallback = new WP_REST_Request( array( 'x_pixelstore_token' => $ok['token'] ) );
check( 'X-PixelStore-Token yedeği çalışır', PixelStore_Auth::authenticate( $fallback ) instanceof WP_User );

/* 6. Rol tespiti. */
check( 'admin yönetici sayılır', PixelStore_Auth::is_admin( $GLOBALS['ps_users'][7] ) );
check( 'standart kullanıcı yönetici sayılmaz', ! PixelStore_Auth::is_admin( $GLOBALS['ps_users'][11] ) );

$userLogin = PixelStore_Auth::login( 'user', 'dogru-parola' );
check( 'kullanıcı oturumu user rolü taşır', 'user' === $userLogin['session']['role'] );

/* 7. Süresi dolmuş jeton reddedilir. */
$expired = array( array( 'hash' => hash( 'sha256', 'eski' ), 'expires' => time() - 10 ) );
update_user_meta( 7, '_ps_tokens', $expired );
$expiredRequest = new WP_REST_Request( array( 'authorization' => 'Bearer 7.eski' ) );
check( 'süresi dolmuş jeton reddedilir', is_wp_error( PixelStore_Auth::authenticate( $expiredRequest ) ) );

/* 8. Çıkış jetonu iptal eder, diğer cihazları etkilemez. */
$first  = PixelStore_Auth::login( 'admin', 'dogru-parola' );
$second = PixelStore_Auth::login( 'admin', 'dogru-parola' );
PixelStore_Auth::logout( new WP_REST_Request( array( 'authorization' => 'Bearer ' . $first['token'] ) ) );
check(
	'çıkış yapılan jeton geçersiz',
	is_wp_error( PixelStore_Auth::authenticate( new WP_REST_Request( array( 'authorization' => 'Bearer ' . $first['token'] ) ) ) )
);
check(
	'diğer cihazın jetonu geçerli kalır',
	PixelStore_Auth::authenticate( new WP_REST_Request( array( 'authorization' => 'Bearer ' . $second['token'] ) ) ) instanceof WP_User
);

/* 9. Eşzamanlı cihaz sınırı. */
update_user_meta( 7, '_ps_tokens', array() );
$tokens = array();
for ( $i = 0; $i < 8; $i++ ) {
	$tokens[] = PixelStore_Auth::login( 'admin', 'dogru-parola' )['token'];
}
$kept = get_user_meta( 7, '_ps_tokens', true );
check( 'en fazla 5 jeton tutulur', 5 === count( $kept ) );
check(
	'en yeni jeton hâlâ geçerli',
	PixelStore_Auth::authenticate( new WP_REST_Request( array( 'authorization' => 'Bearer ' . end( $tokens ) ) ) ) instanceof WP_User
);
check(
	'en eski jeton düşmüş',
	is_wp_error( PixelStore_Auth::authenticate( new WP_REST_Request( array( 'authorization' => 'Bearer ' . $tokens[0] ) ) ) )
);

echo "\n$passed geçti, $failed başarısız\n";
exit( $failed > 0 ? 1 : 0 );

function wp_json_encode_fallback( $value ) {
	return json_encode( $value );
}
