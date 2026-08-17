<?php
/**
 * Plugin Name:       PixelStore API
 * Plugin URI:        https://github.com/Waifuhtr/rpgmaker-app
 * Description:       PixelStore Android uygulamasının veritabanı ve REST arka ucu. Uygulama kayıtlarını özel yazı tipinde saklar, jeton tabanlı oturum ve rol denetimi sağlar.
 * Version:           1.0.0
 * Requires at least: 6.0
 * Requires PHP:      7.4
 * Author:            PixelStore
 * License:           MIT
 * Text Domain:       pixelstore-api
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

define( 'PIXELSTORE_VERSION', '1.0.0' );
define( 'PIXELSTORE_NAMESPACE', 'pixelstore/v1' );
define( 'PIXELSTORE_POST_TYPE', 'pixelstore_app' );
define( 'PIXELSTORE_TAXONOMY', 'pixelstore_category' );
define( 'PIXELSTORE_PLUGIN_FILE', __FILE__ );
define( 'PIXELSTORE_PLUGIN_DIR', plugin_dir_path( __FILE__ ) );

require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-cpt.php';
require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-auth.php';
require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-repository.php';
require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-rest.php';
require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-seed.php';
require_once PIXELSTORE_PLUGIN_DIR . 'includes/class-pixelstore-admin.php';

/**
 * Eklentiyi başlatır.
 */
function pixelstore_bootstrap() {
	PixelStore_CPT::init();
	PixelStore_REST::init();
	PixelStore_Admin::init();
}
add_action( 'plugins_loaded', 'pixelstore_bootstrap' );

/**
 * Etkinleştirmede kayıt tiplerini kaydedip kalıcı bağlantıları tazeler.
 * Demo katalog otomatik kurulmaz; yönetici wp-admin'den bilinçli olarak kurar.
 */
function pixelstore_activate() {
	PixelStore_CPT::register_types();
	PixelStore_CPT::ensure_default_terms();
	flush_rewrite_rules();
}
register_activation_hook( __FILE__, 'pixelstore_activate' );

function pixelstore_deactivate() {
	flush_rewrite_rules();
}
register_deactivation_hook( __FILE__, 'pixelstore_deactivate' );
