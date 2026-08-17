<?php
/**
 * Kayıt tipi ve tür taksonomisi.
 *
 * Uygulama kayıtları `pixelstore_app` özel yazı tipinde tutulur. Yazı tipi herkese açık değildir
 * (public=false): kayıtlar site ön yüzünde tek tek sayfa olarak açılmaz, yalnızca REST üzerinden
 * uygulamaya sunulur.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_CPT {

	/** Uygulama alanları için meta anahtarları. */
	const META = array(
		'developer'        => '_ps_developer',
		'version'          => '_ps_version',
		'sizeMb'           => '_ps_size_mb',
		'rating'           => '_ps_rating',
		'ratingCount'      => '_ps_rating_count',
		'installs'         => '_ps_installs',
		'contentRating'    => '_ps_content_rating',
		'iconSeed'         => '_ps_icon_seed',
		'palette'          => '_ps_palette',
		'shortDescription' => '_ps_short_description',
		'downloadUrl'      => '_ps_download_url',
		'screenshots'      => '_ps_screenshots',
		'tags'             => '_ps_tags',
	);

	public static function init() {
		add_action( 'init', array( __CLASS__, 'register_types' ) );
	}

	public static function register_types() {
		register_post_type(
			PIXELSTORE_POST_TYPE,
			array(
				'labels'          => array(
					'name'          => 'PixelStore Kayıtları',
					'singular_name' => 'PixelStore Kaydı',
					'add_new_item'  => 'Yeni kayıt ekle',
					'edit_item'     => 'Kaydı düzenle',
					'search_items'  => 'Kayıt ara',
					'not_found'     => 'Kayıt bulunamadı',
				),
				'public'          => false,
				'show_ui'         => true,
				'show_in_menu'    => 'pixelstore',
				'show_in_rest'    => false,
				'supports'        => array( 'title', 'editor', 'excerpt', 'revisions' ),
				'has_archive'     => false,
				'rewrite'         => false,
				'capability_type' => 'post',
				'map_meta_cap'    => true,
				'menu_icon'       => 'dashicons-games',
			)
		);

		register_taxonomy(
			PIXELSTORE_TAXONOMY,
			PIXELSTORE_POST_TYPE,
			array(
				'labels'            => array(
					'name'          => 'Türler',
					'singular_name' => 'Tür',
				),
				'public'            => false,
				'show_ui'           => true,
				'show_in_rest'      => false,
				'hierarchical'      => true,
				'show_admin_column' => true,
			)
		);

		// Meta alanları kayıtlı olsun: WordPress tarafında da düzenlenebilir kalsınlar.
		foreach ( self::META as $key ) {
			register_post_meta(
				PIXELSTORE_POST_TYPE,
				$key,
				array(
					'type'          => 'string',
					'single'        => true,
					'show_in_rest'  => false,
					'auth_callback' => function () {
						return current_user_can( 'edit_posts' );
					},
				)
			);
		}
	}

	/** Varsayılan türleri (bir kez) oluşturur. */
	public static function ensure_default_terms() {
		$defaults = array(
			'RPG'         => 'sword',
			'Aksiyon'     => 'flame',
			'Bulmaca'     => 'gem',
			'Simülasyon'  => 'potion',
			'Macera'      => 'map',
			'Araçlar'     => 'gear',
		);

		foreach ( $defaults as $name => $glyph ) {
			$term = term_exists( $name, PIXELSTORE_TAXONOMY );
			if ( ! $term ) {
				$term = wp_insert_term( $name, PIXELSTORE_TAXONOMY );
			}
			if ( ! is_wp_error( $term ) && isset( $term['term_id'] ) ) {
				update_term_meta( $term['term_id'], '_ps_glyph', $glyph );
			}
		}
	}

	/** Uygulamaya gönderilecek tür listesi. */
	public static function categories() {
		$terms = get_terms(
			array(
				'taxonomy'   => PIXELSTORE_TAXONOMY,
				'hide_empty' => false,
			)
		);
		if ( is_wp_error( $terms ) ) {
			return array();
		}
		$out = array();
		foreach ( $terms as $term ) {
			$glyph = get_term_meta( $term->term_id, '_ps_glyph', true );
			$out[] = array(
				'id'    => $term->slug,
				'name'  => $term->name,
				'glyph' => $glyph ? $glyph : 'star',
			);
		}
		return $out;
	}
}
