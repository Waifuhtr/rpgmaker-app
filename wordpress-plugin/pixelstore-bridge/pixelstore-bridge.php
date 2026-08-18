<?php
/**
 * Plugin Name:       PixelStore Bridge
 * Plugin URI:        https://github.com/Waifuhtr/rpgmaker-app
 * Description:       SteamLike temasındaki oyunları, kullanıcıları, istek listesini, puanları ve incelemeleri PixelStore Android uygulamasına açar. Kendi veri şeması yoktur; temanın mevcut verisini okur ve yazar.
 * Version:           1.0.0
 * Requires at least: 6.0
 * Requires PHP:      7.4
 * Author:            PixelStore
 * License:           MIT
 * Text Domain:       pixelstore-bridge
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

define( 'PSB_VERSION', '1.0.0' );
define( 'PSB_NAMESPACE', 'pixelstore/v2' );
define( 'PSB_DIR', plugin_dir_path( __FILE__ ) );

/**
 * SteamLike temasının kayıt tipi ve taksonomileri.
 *
 * Bu eklenti kendi içerik tipini KAYDETMEZ. Sitede zaten yayınlanmış oyunlar hiçbir taşıma
 * yapılmadan uygulamada görünür; uygulamadan eklenen kayıt da sitede normal bir oyun olur.
 */
define( 'PSB_POST_TYPE', 'game' );
define( 'PSB_REPORT_POST_TYPE', 'sl_report' );

require_once PSB_DIR . 'includes/class-psb-auth.php';
require_once PSB_DIR . 'includes/class-psb-mapper.php';
require_once PSB_DIR . 'includes/class-psb-query.php';
require_once PSB_DIR . 'includes/class-psb-social.php';
require_once PSB_DIR . 'includes/class-psb-write.php';
require_once PSB_DIR . 'includes/class-psb-media.php';
require_once PSB_DIR . 'includes/class-psb-rest.php';
require_once PSB_DIR . 'includes/class-psb-admin.php';

function psb_bootstrap() {
	PSB_REST::init();
	PSB_Admin::init();
}
add_action( 'plugins_loaded', 'psb_bootstrap' );

/** SteamLike teması etkin mi? Uçlar buna göre uyarı verir. */
function psb_theme_active() {
	return post_type_exists( PSB_POST_TYPE );
}
