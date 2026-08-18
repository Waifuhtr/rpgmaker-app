<?php
/**
 * AJAX Oyun Puanlama Sistemi
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Ratings {

	public function __construct() {
		add_action( 'wp_ajax_sl_rate_game', array( $this, 'rate_game_callback' ) );
		add_action( 'wp_ajax_nopriv_sl_rate_game', array( $this, 'rate_game_callback' ) );
	}

	public function rate_game_callback() {
		// Güvenlik kontrolü
		check_ajax_referer( 'steamlike_ajax_nonce', 'security' );

		$post_id = isset( $_POST['post_id'] ) ? intval( $_POST['post_id'] ) : 0;
		$rating  = isset( $_POST['rating'] ) ? intval( $_POST['rating'] ) : 0;

		if ( $post_id <= 0 || $rating < 1 || $rating > 5 ) {
			wp_send_json_error( __( 'Geçersiz veri.', 'steamlike' ) );
		}

		// Aynı tarayıcıdan/kullanıcıdan tekrar oy verilmesini engellemek için basit bir çerez kontrolü
		$cookie_name = 'sl_voted_' . $post_id;
		if ( isset( $_COOKIE[ $cookie_name ] ) ) {
			wp_send_json_error( __( 'Bu oyuna zaten puan verdiniz.', 'steamlike' ) );
		}

		// Mevcut verileri çek
		$current_rating_sum   = (int) get_post_meta( $post_id, 'sl_user_rating_sum', true );
		$current_rating_count = (int) get_post_meta( $post_id, 'sl_user_rating_count', true );

		// Yeni verileri hesapla
		$new_sum   = $current_rating_sum + $rating;
		$new_count = $current_rating_count + 1;
		$new_avg   = round( $new_sum / $new_count, 1 );

		// Veritabanını güncelle
		update_post_meta( $post_id, 'sl_user_rating_sum', $new_sum );
		update_post_meta( $post_id, 'sl_user_rating_count', $new_count );
		update_post_meta( $post_id, 'sl_user_rating_avg', $new_avg );

		// Çerezi 30 günlüğüne ayarla
		setcookie( $cookie_name, '1', time() + ( 30 * DAY_IN_SECONDS ), COOKIEPATH, COOKIE_DOMAIN );

		// Başarılı yanıt ve yeni ortalamayı gönder
		wp_send_json_success( array(
			'new_avg'   => $new_avg,
			'new_count' => $new_count,
			'message'   => __( 'Puanınız kaydedildi, teşekkürler!', 'steamlike' )
		) );
	}
}
