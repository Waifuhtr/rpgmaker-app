<?php
/**
 * Oyun Raporlama Sistemi (Hata Bildirimi)
 */
if ( ! defined( 'ABSPATH' ) ) exit;

class SteamLike_Reports {

	public function __construct() {
		add_action( 'init', array( $this, 'register_report_cpt' ) );
		add_action( 'template_redirect', array( $this, 'handle_report_submission' ) );
		// Admin panelindeki Raporlar listesi sütunlarını özelleştir
		add_filter( 'manage_sl_report_posts_columns', array( $this, 'custom_columns' ) );
		add_action( 'manage_sl_report_posts_custom_column', array( $this, 'custom_column_data' ), 10, 2 );
	}

	// 1. Admin Paneline "Raporlar" Menüsünü (Custom Post Type) Ekle
	public function register_report_cpt() {
		register_post_type( 'sl_report', array(
			'labels' => array(
				'name'          => __( 'Raporlar', 'steamlike' ),
				'singular_name' => __( 'Rapor', 'steamlike' ),
				'all_items'     => __( 'Tüm Raporlar', 'steamlike' ),
				'view_item'     => __( 'Raporu İncele', 'steamlike' ),
			),
			'public'              => false,
			'show_ui'             => true,
			'show_in_menu'        => true,
			'menu_position'       => 26,
			'menu_icon'           => 'dashicons-warning', // Uyarı ikonu
			'supports'            => array( 'title', 'editor' ), // Title: Oyun Adı, Editor: Sorun Açıklaması
			'capabilities'        => array( 'create_posts' => 'do_not_allow' ), // Adminlerin manuel rapor oluşturmasını engeller
			'map_meta_cap'        => true,
		));
	}

	// 2. Kullanıcı Formu Gönderdiğinde Çalışacak İşlem (Frontend Yükleme)
	public function handle_report_submission() {
		if ( isset( $_POST['sl_submit_report'] ) && $_SERVER['REQUEST_METHOD'] == 'POST' ) {
			
			$game_id = intval( $_POST['report_game_id'] );
			$message = sanitize_textarea_field( $_POST['report_message'] );
			
			if ( empty( $message ) ) return;

			$user_name = is_user_logged_in() ? wp_get_current_user()->display_name : __( 'Ziyaretçi', 'steamlike' );
			$game_title = get_the_title( $game_id );

			// Raporu veritabanına kaydet
			$report_id = wp_insert_post( array(
				'post_title'   => sprintf( __( '[%s] Hata Raporu - %s', 'steamlike' ), $game_title, $user_name ),
				'post_content' => $message,
				'post_type'    => 'sl_report',
				'post_status'  => 'publish'
			) );

			if ( $report_id ) {
				update_post_meta( $report_id, 'reported_game_id', $game_id );

				// Görsel Yüklendiyse İşle
				if ( ! empty( $_FILES['report_image']['name'] ) ) {
					require_once( ABSPATH . 'wp-admin/includes/image.php' );
					require_once( ABSPATH . 'wp-admin/includes/file.php' );
					require_once( ABSPATH . 'wp-admin/includes/media.php' );
					
					$attach_id = media_handle_upload( 'report_image', $report_id );
					if ( ! is_wp_error( $attach_id ) ) {
						update_post_meta( $report_id, 'report_screenshot', wp_get_attachment_url( $attach_id ) );
					}
				}

				// Başarı mesajıyla sayfayı yenile
				wp_safe_redirect( add_query_arg( 'rapor', 'basarili', get_permalink( $game_id ) ) );
				exit;
			}
		}
	}

	// 3. Admin Paneli Sütun Ayarları
	public function custom_columns( $columns ) {
		$new_columns = array(
			'cb'         => $columns['cb'],
			'title'      => __( 'Rapor Başlığı', 'steamlike' ),
			'game_link'  => __( 'İlgili Oyun', 'steamlike' ),
			'screenshot' => __( 'Ekran Görüntüsü', 'steamlike' ),
			'date'       => $columns['date']
		);
		return $new_columns;
	}

	public function custom_column_data( $column, $post_id ) {
		if ( $column === 'game_link' ) {
			$game_id = get_post_meta( $post_id, 'reported_game_id', true );
			if ( $game_id ) {
				echo '<a href="' . esc_url( get_permalink( $game_id ) ) . '" target="_blank"><strong>' . esc_html( get_the_title( $game_id ) ) . '</strong></a>';
			} else { echo '-'; }
		}
		if ( $column === 'screenshot' ) {
			$image_url = get_post_meta( $post_id, 'report_screenshot', true );
			if ( $image_url ) {
				echo '<a href="' . esc_url( $image_url ) . '" target="_blank"><img src="' . esc_url( $image_url ) . '" style="max-width:80px; border-radius:4px;"></a>';
			} else { echo '<span style="color:#999;">Görsel Yok</span>'; }
		}
	}
}
new SteamLike_Reports();
