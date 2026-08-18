<?php
/**
 * Custom Post Types kaydı
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_CPT {

	public function __construct() {
		add_action( 'init', array( $this, 'register_game_cpt' ) );
	}

	public function register_game_cpt() {
		$labels = array(
			'name'                  => _x( 'Oyunlar', 'Post Type General Name', 'steamlike' ),
			'singular_name'         => _x( 'Oyun', 'Post Type Singular Name', 'steamlike' ),
			'menu_name'             => __( 'Oyunlar', 'steamlike' ),
			'name_admin_bar'        => __( 'Oyun', 'steamlike' ),
			'archives'              => __( 'Oyun Arşivi', 'steamlike' ),
			'all_items'             => __( 'Tüm Oyunlar', 'steamlike' ),
			'add_new_item'          => __( 'Yeni Oyun Ekle', 'steamlike' ),
			'add_new'               => __( 'Yeni Ekle', 'steamlike' ),
			'new_item'              => __( 'Yeni Oyun', 'steamlike' ),
			'edit_item'             => __( 'Oyunu Düzenle', 'steamlike' ),
			'update_item'           => __( 'Oyunu Güncelle', 'steamlike' ),
			'view_item'             => __( 'Oyunu Görüntüle', 'steamlike' ),
			'search_items'          => __( 'Oyun Ara', 'steamlike' ),
			'not_found'             => __( 'Bulunamadı', 'steamlike' ),
			'not_found_in_trash'    => __( 'Çöp kutusunda bulunamadı', 'steamlike' ),
			'featured_image'        => __( 'Kapak Görseli', 'steamlike' ),
			'set_featured_image'    => __( 'Kapak görselini belirle', 'steamlike' ),
			'remove_featured_image' => __( 'Kapak görselini kaldır', 'steamlike' ),
			'use_featured_image'    => __( 'Kapak görseli olarak kullan', 'steamlike' ),
		);

		$args = array(
			'label'                 => __( 'Oyun', 'steamlike' ),
			'description'           => __( 'Oyun, yazılım ve dijital içerikler', 'steamlike' ),
			'labels'                => $labels,
			'supports'              => array( 'title', 'editor', 'excerpt', 'thumbnail', 'comments', 'custom-fields' ),
			'taxonomies'            => array( 'category', 'post_tag' ),
			'hierarchical'          => false,
			'public'                => true,
			'show_ui'               => true,
			'show_in_menu'          => true,
			'menu_position'         => 5,
			'menu_icon'             => 'dashicons-games', // WordPress'in kendi oyun ikonu
			'show_in_admin_bar'     => true,
			'show_in_nav_menus'     => true,
			'can_export'            => true,
			'has_archive'           => 'games', // steamlike.com/games linkinde arşivi açar
			'exclude_from_search'   => false,
			'publicly_queryable'    => true,
			'capability_type'       => 'post',
			'show_in_rest'          => true, // Gutenberg editörü desteği
		);

		register_post_type( 'game', $args );
	}
}
