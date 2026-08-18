<?php
/**
 * AJAX İstek Listesi (Favoriler) Sistemi
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Favorites {

	public function __construct() {
		add_action( 'wp_ajax_sl_toggle_favorite', array( $this, 'toggle_favorite_callback' ) );
	}

	public function toggle_favorite_callback() {
		check_ajax_referer( 'steamlike_ajax_nonce', 'security' );

		$post_id = isset( $_POST['post_id'] ) ? intval( $_POST['post_id'] ) : 0;
		if ( ! $post_id ) {
			wp_send_json_error( __( 'Geçersiz işlem.', 'steamlike' ) );
		}

		if ( is_user_logged_in() ) {
			$user_id = get_current_user_id();
			$favorites = get_user_meta( $user_id, 'sl_favorites', true );
			
			if ( ! is_array( $favorites ) ) {
				$favorites = array();
			}

			// Eğer oyun zaten favorilerdeyse çıkar, değilse ekle
			if ( in_array( $post_id, $favorites ) ) {
				$favorites = array_diff( $favorites, array( $post_id ) );
				$status = 'removed';
			} else {
				$favorites[] = $post_id;
				$status = 'added';
			}

			update_user_meta( $user_id, 'sl_favorites', array_unique( $favorites ) );
			wp_send_json_success( array( 'status' => $status ) );
		}

		wp_send_json_error( __( 'Giriş yapmalısınız.', 'steamlike' ) );
	}
}
