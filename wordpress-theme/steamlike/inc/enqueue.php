<?php
/**
 * Enqueue scripts and styles
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Enqueue {

	public function __construct() {
		add_action( 'wp_enqueue_scripts', array( $this, 'enqueue_scripts' ) );
		add_action( 'admin_enqueue_scripts', array( $this, 'admin_scripts' ) );
	}

	public function enqueue_scripts() {
		$version = STEAMLIKE_VERSION;

		// CSS Dosyaları
		wp_enqueue_style( 'steamlike-style', get_stylesheet_uri(), array(), $version );
		wp_enqueue_style( 'steamlike-main', STEAMLIKE_URI . 'assets/css/main.css', array(), $version );
		wp_enqueue_style( 'steamlike-animations', STEAMLIKE_URI . 'assets/css/animations.css', array(), $version );

		// JS Dosyaları
		wp_enqueue_script( 'steamlike-main', STEAMLIKE_URI . 'assets/js/main.js', array( 'jquery' ), $version, true );
		wp_enqueue_script( 'steamlike-ajax', STEAMLIKE_URI . 'assets/js/ajax-filters.js', array( 'jquery' ), $version, true );
		
		// Önceden yanlış yerde olan JS dosyaları doğru yere taşındı ve version hatası giderildi
		wp_enqueue_script( 'steamlike-ratings', STEAMLIKE_URI . 'assets/js/ratings.js', array( 'jquery' ), $version, true );
		wp_enqueue_script( 'steamlike-favorites', STEAMLIKE_URI . 'assets/js/favorites.js', array( 'jquery' ), $version, true );
		wp_enqueue_script( 'steamlike-downloads', STEAMLIKE_URI . 'assets/js/downloads.js', array( 'jquery' ), $version, true );
		wp_enqueue_script( 'steamlike-navigation', STEAMLIKE_URI . 'assets/js/navigation.js', array( 'jquery' ), $version, true );
		wp_enqueue_script( 'steamlike-live-search', STEAMLIKE_URI . 'assets/js/live-search.js', array( 'jquery' ), $version, true );

		// AJAX ve Nonce Güvenlik Verilerini JavaScript'e Aktar
		wp_localize_script( 'steamlike-ajax', 'steamlike_data', array(
			'ajax_url'   => admin_url( 'admin-ajax.php' ),
			'nonce'      => wp_create_nonce( 'steamlike_ajax_nonce' ),
			'error_msg'  => esc_html__( 'Bir hata oluştu, lütfen tekrar deneyin.', 'steamlike' )
		) );
	}

	public function admin_scripts() {
		wp_enqueue_style( 'steamlike-admin', STEAMLIKE_URI . 'assets/css/admin.css', array(), STEAMLIKE_VERSION );
		wp_enqueue_script( 'steamlike-admin', STEAMLIKE_URI . 'assets/js/admin.js', array( 'jquery' ), STEAMLIKE_VERSION, true );
	}
}
