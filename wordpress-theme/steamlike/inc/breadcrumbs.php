<?php
/**
 * SteamLike Breadcrumb (Sayfa Yolu) Sistemi
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

function sl_breadcrumb() {
	// Sadece ana sayfa değilse göster
	if ( ! is_front_page() ) {
		echo '<nav class="sl-breadcrumb" aria-label="Breadcrumb">';
		
		// Ana Sayfa Linki
		echo '<a href="' . esc_url( home_url( '/' ) ) . '"><span class="dashicons dashicons-admin-home" style="vertical-align: text-top; font-size: 16px;"></span> ' . esc_html__( 'Ana Sayfa', 'steamlike' ) . '</a>';
		echo '<span class="sl-breadcrumb-sep"> / </span>';

		if ( is_post_type_archive( 'game' ) ) {
			// Oyun Kütüphanesi Arşivi
			echo '<span class="sl-breadcrumb-current">' . esc_html__( 'Oyun Kütüphanesi', 'steamlike' ) . '</span>';
		} 
		elseif ( is_search() ) {
			// Arama Sonuçları
			echo '<span class="sl-breadcrumb-current">' . esc_html__( 'Arama Sonuçları', 'steamlike' ) . '</span>';
		} 
		elseif ( is_singular( 'game' ) ) {
			// Oyun Detay Sayfası
			echo '<a href="' . esc_url( get_post_type_archive_link( 'game' ) ) . '">' . esc_html__( 'Oyunlar', 'steamlike' ) . '</a>';
			echo '<span class="sl-breadcrumb-sep"> / </span>';

			// Eğer oyunun türü varsa ilk türü de araya ekleyelim
			$genres = get_the_terms( get_the_ID(), 'game_genre' );
			if ( $genres && ! is_wp_error( $genres ) ) {
				$first_genre = $genres[0];
				$genre_link = add_query_arg( array( 'game_genre' => $first_genre->slug ), get_post_type_archive_link('game') );
				echo '<a href="' . esc_url( $genre_link ) . '">' . esc_html( $first_genre->name ) . '</a>';
				echo '<span class="sl-breadcrumb-sep"> / </span>';
			}

			echo '<span class="sl-breadcrumb-current">' . get_the_title() . '</span>';
		} 
		elseif ( is_page() ) {
			// Standart Sayfa (İndirme sayfası vb.)
			echo '<span class="sl-breadcrumb-current">' . get_the_title() . '</span>';
		}
		elseif ( is_author() ) {
			// Geliştirici / Kullanıcı Profili
			echo '<span class="sl-breadcrumb-current">' . esc_html( get_the_author() ) . '</span>';
		}

		echo '</nav>';
	}
}
