<?php
/**
 * Görüntülenme ve İndirme Sayaçları
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Counters {

	public function __construct() {
		// Sayfa yüklendiğinde görüntülenmeyi artır
		add_action( 'template_redirect', array( $this, 'track_page_views' ) );
		
		// İndirme butonuna tıklandığında AJAX ile artır
		add_action( 'wp_ajax_sl_track_download', array( $this, 'track_download_callback' ) );
		add_action( 'wp_ajax_nopriv_sl_track_download', array( $this, 'track_download_callback' ) );
	}

	public function track_page_views() {
		// Sadece tekil oyun sayfasındaysak çalışır
		if ( is_singular( 'game' ) ) {
			global $post;
			$views = (int) get_post_meta( $post->ID, 'game_view_count', true );
			update_post_meta( $post->ID, 'game_view_count', $views + 1 );
		}
	}

	public function track_download_callback() {
		check_ajax_referer( 'steamlike_ajax_nonce', 'security' );
		
		$post_id = isset( $_POST['post_id'] ) ? intval( $_POST['post_id'] ) : 0;
		if ( $post_id ) {
			$downloads = (int) get_post_meta( $post_id, 'game_download_count', true );
			$new_count = $downloads + 1;
			update_post_meta( $post_id, 'game_download_count', $new_count );
			
			wp_send_json_success( array( 'new_count' => $new_count ) );
		}
		wp_send_json_error();
	}
}
