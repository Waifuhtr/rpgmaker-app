<?php
/**
 * Tema Kurulum Ayarları
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Setup {

	public function __construct() {
		add_action( 'after_setup_theme', array( $this, 'setup' ) );
	}

	public function setup() {
		// Çeviri desteği
		load_theme_textdomain( 'steamlike', STEAMLIKE_DIR . 'languages' );

		// Başlık etiketi desteği
		add_theme_support( 'title-tag' );

		// Öne çıkan görsel desteği
		add_theme_support( 'post-thumbnails' );
		add_image_size( 'steamlike-cover', 300, 450, true ); // Oyun kapak boyutu

		// Özel Logo Desteği (YENİ EKLENDİ)
		add_theme_support( 'custom-logo', array(
			'height'      => 80,
			'width'       => 250,
			'flex-width'  => true,
			'flex-height' => true,
		) );

		// Menü destekleri
		register_nav_menus(
			array(
				'primary' => esc_html__( 'Ana Menü', 'steamlike' ),
				'footer'  => esc_html__( 'Alt Menü', 'steamlike' ),
			)
		);

		// HTML5 desteği
		add_theme_support(
			'html5',
			array(
				'search-form',
				'comment-form',
				'comment-list',
				'gallery',
				'caption',
				'style',
				'script',
			)
		);

		// Fragman / video gömmelerinin mobilde otomatik olarak orantılı boyutlanması
		add_theme_support( 'responsive-embeds' );

		// RSS feed bağlantılarının <head> içine otomatik eklenmesi
		add_theme_support( 'automatic-feed-links' );
	}
}
