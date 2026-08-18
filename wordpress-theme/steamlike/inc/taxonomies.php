<?php
/**
 * Custom Taxonomies (Özel Sınıflandırmalar)
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Taxonomies {

	public function __construct() {
		add_action( 'init', array( $this, 'register_taxonomies' ), 0 );
	}

	public function register_taxonomies() {
		// Oluşturulacak taksonomilerin listesi ve ayarları
		$taxonomies = array(
			'game_genre'     => array( 'singular' => 'Tür', 'plural' => 'Türler', 'hierarchical' => true ),
			'game_platform'  => array( 'singular' => 'Platform', 'plural' => 'Platformlar', 'hierarchical' => true ),
			'game_language'  => array( 'singular' => 'Dil', 'plural' => 'Diller', 'hierarchical' => true ),
			'game_publisher' => array( 'singular' => 'Yayıncı', 'plural' => 'Yayıncılar', 'hierarchical' => false ),
			'game_developer' => array( 'singular' => 'Geliştirici', 'plural' => 'Geliştiriciler', 'hierarchical' => false ),
			'game_features'  => array( 'singular' => 'Özellik', 'plural' => 'Özellikler', 'hierarchical' => true ),
			'game_status'    => array( 'singular' => 'Durum', 'plural' => 'Durumlar', 'hierarchical' => true ),
			'game_version'   => array( 'singular' => 'Sürüm', 'plural' => 'Sürümler', 'hierarchical' => false ),
		);

		foreach ( $taxonomies as $slug => $data ) {
			$labels = array(
				'name'              => _x( $data['plural'], 'taxonomy general name', 'steamlike' ),
				'singular_name'     => _x( $data['singular'], 'taxonomy singular name', 'steamlike' ),
				'search_items'      => __( 'Ara: ' . $data['plural'], 'steamlike' ),
				'all_items'         => __( 'Tüm ' . $data['plural'], 'steamlike' ),
				'parent_item'       => __( 'Ebeveyn ' . $data['singular'], 'steamlike' ),
				'parent_item_colon' => __( 'Ebeveyn ' . $data['singular'] . ':', 'steamlike' ),
				'edit_item'         => __( 'Düzenle: ' . $data['singular'], 'steamlike' ),
				'update_item'       => __( 'Güncelle: ' . $data['singular'], 'steamlike' ),
				'add_new_item'      => __( 'Yeni ' . $data['singular'] . ' Ekle', 'steamlike' ),
				'new_item_name'     => __( 'Yeni ' . $data['singular'] . ' Adı', 'steamlike' ),
				'menu_name'         => __( $data['plural'], 'steamlike' ),
			);

			$args = array(
				'hierarchical'      => $data['hierarchical'], // True: Kategori gibi, False: Etiket gibi davranır
				'labels'            => $labels,
				'show_ui'           => true,
				'show_admin_column' => true,
				'query_var'         => true,
				'rewrite'           => array( 'slug' => str_replace( '_', '-', $slug ) ), // game_genre -> game-genre
				'show_in_rest'      => true, // Gutenberg paneline ekler
			);

			register_taxonomy( $slug, array( 'game' ), $args );
		}
	}
}
