<?php
/**
 * AJAX Filtreleme ve Anlık Arama İşlemleri
 */
if ( ! defined( 'ABSPATH' ) ) exit;

class SteamLike_AJAX {

	public function __construct() {
		add_action( 'wp_ajax_sl_filter_games', array( $this, 'filter_games_callback' ) );
		add_action( 'wp_ajax_nopriv_sl_filter_games', array( $this, 'filter_games_callback' ) );
		add_action( 'wp_ajax_sl_live_search', array( $this, 'live_search_callback' ) );
		add_action( 'wp_ajax_nopriv_sl_live_search', array( $this, 'live_search_callback' ) );
	}

	public function live_search_callback() {
		check_ajax_referer( 'steamlike_ajax_nonce', 'security' );
		$keyword = isset( $_POST['keyword'] ) ? sanitize_text_field( $_POST['keyword'] ) : '';
		if ( strlen( $keyword ) < 2 ) wp_die();

		$args = array( 'post_type' => 'game', 'post_status' => 'publish', 'posts_per_page' => 5, 's' => $keyword );
		$query = new WP_Query( $args );

		if ( $query->have_posts() ) {
			echo '<ul class="sl-live-search-list">';
			while ( $query->have_posts() ) {
				$query->the_post();
				$thumb = get_the_post_thumbnail_url( get_the_ID(), 'thumbnail' ) ?: STEAMLIKE_URI . 'assets/images/placeholder-cover.jpg';
				echo '<li><a href="' . esc_url( get_permalink() ) . '"><img src="' . esc_url( $thumb ) . '" alt="Kapak">';
				echo '<div class="sl-ls-info"><span class="sl-ls-title">' . get_the_title() . '</span>';
				$platform = strip_tags( get_the_term_list( get_the_ID(), 'game_platform', '', ', ' ) );
				if ( $platform ) echo '<span class="sl-ls-meta">' . esc_html( $platform ) . '</span>';
				echo '</div></a></li>';
			}
			echo '</ul><div class="sl-ls-all-results"><a href="' . esc_url( home_url( '/?s=' . $keyword . '&post_type=game' ) ) . '">' . esc_html__( 'Tüm Sonuçları Gör &rarr;', 'steamlike' ) . '</a></div>';
		} else {
			echo '<div class="sl-ls-no-results">' . esc_html__( 'Oyun bulunamadı.', 'steamlike' ) . '</div>';
		}
		wp_die();
	}

	public function filter_games_callback() {
		check_ajax_referer( 'steamlike_ajax_nonce', 'security' );
		
		$args = array( 'post_type' => 'game', 'post_status' => 'publish', 'posts_per_page' => 12 );

		if ( isset( $_POST['search_text'] ) && ! empty( $_POST['search_text'] ) ) {
			$args['s'] = sanitize_text_field( $_POST['search_text'] );
		}

		$tax_query = array( 'relation' => 'AND' );
		$taxonomies = array( 'game_genre', 'game_platform', 'game_language', 'game_status', 'post_tag' );
		
		foreach ( $taxonomies as $tax ) {
			if ( isset( $_POST[ $tax ] ) && ! empty( $_POST[ $tax ] ) ) {
				// HATA ÇÖZÜMÜ BURADA: Gelen veri dizi ise array_map kullan, metin ise direkt sanitize et
				$terms = is_array( $_POST[ $tax ] ) ? array_map( 'sanitize_text_field', $_POST[ $tax ] ) : sanitize_text_field( $_POST[ $tax ] );
				$tax_query[] = array( 
                    'taxonomy' => $tax, 
                    'field' => 'slug', 
                    'terms' => $terms 
                );
			}
		}
		if ( count( $tax_query ) > 1 ) $args['tax_query'] = $tax_query;

		if ( isset( $_POST['sort_by'] ) && ! empty( $_POST['sort_by'] ) ) {
			switch ( sanitize_text_field( $_POST['sort_by'] ) ) {
				case 'newest': $args['orderby'] = 'date'; $args['order'] = 'DESC'; break;
				case 'oldest': $args['orderby'] = 'date'; $args['order'] = 'ASC'; break;
				case 'rating_high': $args['meta_key'] = 'sl_user_rating_avg'; $args['orderby'] = 'meta_value_num'; $args['order'] = 'DESC'; break;
			}
		}

		$query = new WP_Query( $args );
		if ( $query->have_posts() ) {
			while ( $query->have_posts() ) { 
                $query->the_post(); 
                get_template_part( 'template-parts/cards/card-game' ); 
            }
			wp_reset_postdata();
		} else {
			echo '<div class="sl-no-results" style="grid-column: 1 / -1; text-align: center; padding: 40px;"><span class="dashicons dashicons-warning" style="font-size: 48px; width:48px; height:48px; margin-bottom:15px; color:#64748b;"></span><br><span style="color:#94a3b8; font-size:16px;">Aradığınız filtrelere uygun oyun bulunamadı.</span></div>';
		}
		wp_die();
	}
}
