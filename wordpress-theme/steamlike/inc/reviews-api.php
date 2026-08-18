<?php
/**
 * API ve İnceleme (Review) Yönlendirme Motoru
 */

if ( ! defined( 'ABSPATH' ) ) exit;

class SteamLike_Reviews {

	public function __construct() {
		add_action( 'init', array( $this, 'add_rewrite_endpoint' ) );
		add_filter( 'template_include', array( $this, 'reviews_template' ) );
	}

	public function add_rewrite_endpoint() { add_rewrite_endpoint( 'inceleme', EP_PERMALINK ); }

	public function reviews_template( $template ) {
		global $wp_query;
		if ( is_singular( 'game' ) && isset( $wp_query->query_vars['inceleme'] ) ) {
			$new_template = STEAMLIKE_DIR . 'single-game-reviews.php';
			if ( file_exists( $new_template ) ) return $new_template;
		}
		return $template;
	}

	public static function get_playtime( $user_id, $post_id ) {
		$api_username = get_user_meta( $user_id, 'sl_api_username', true );
		$game_api_id  = get_post_meta( $post_id, 'game_api_id', true );

		if ( ! $api_username || ! $game_api_id ) return false;

		$url = "https://riaslink.fun/wp-json/lisans/v1/oyun-suresi?kullanici_adi=" . urlencode( $api_username ) . "&oyun_id=" . urlencode( $game_api_id );
		$response = wp_remote_get( $url, array( 'timeout' => 5 ) );

		if ( is_wp_error( $response ) ) return false;

		$body = wp_remote_retrieve_body( $response );
		$data = json_decode( $body, true );

		if ( $data && isset( $data['durum'] ) && $data['durum'] === 'basarili' ) {
			
			// 1. Oynanan Oyunlara Ekle
			$played = get_user_meta( $user_id, 'sl_played_games', true );
			if ( ! is_array( $played ) ) $played = array();
			if ( ! in_array( $post_id, $played ) ) {
				$played[] = $post_id;
				update_user_meta( $user_id, 'sl_played_games', $played );
			}

			// 2. Oynama Süresini Kaydet (PROFİL İÇİN)
			$playtimes = get_user_meta( $user_id, 'sl_game_playtimes', true );
			if ( ! is_array( $playtimes ) ) $playtimes = array();
			$playtimes[$post_id] = $data['steam_format']; // Örn: "45 dakika kayıtlarda"
			update_user_meta( $user_id, 'sl_game_playtimes', $playtimes );

			// 3. BAŞARIMLAR İÇİN SANİYELERİ KAYDET (YENİ EKLENDİ)
			$seconds = get_user_meta( $user_id, 'sl_game_seconds', true );
			if ( ! is_array( $seconds ) ) $seconds = array();
			$seconds[$post_id] = intval( $data['saniye'] ); 
			update_user_meta( $user_id, 'sl_game_seconds', $seconds );

			return $data['steam_format'];
		}
		return false;
	}
}
new SteamLike_Reviews();
