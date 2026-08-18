<?php
/**
 * Uygulamadan görsel yükleme: profil fotoğrafı, oyun kapağı ve ekran görüntüleri.
 *
 * Yüklenen dosya WordPress medya kütüphanesine girer; kapak öne çıkan görsel olur, ekran
 * görüntüleri temanın `game_screenshots` meta'sına (virgülle ayrılmış medya ID'leri) eklenir.
 * Böylece uygulamadan yüklenen görsel sitede de görünür.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Media {

	const MAX_BYTES     = 8388608; // 8 MB
	const ALLOWED_TYPES = array( 'image/jpeg', 'image/png', 'image/webp', 'image/gif' );

	/**
	 * İstekteki dosyayı medya kütüphanesine alır.
	 *
	 * @param array    $file    $request->get_file_params()['file'] yapısı.
	 * @param int      $post_id Eklenti hangi kayda bağlanacak (0 = bağımsız).
	 * @return int|WP_Error Attachment ID.
	 */
	public static function sideload( $file, $post_id = 0 ) {
		if ( empty( $file ) || ! is_array( $file ) || empty( $file['tmp_name'] ) ) {
			return new WP_Error( 'psb_no_file', 'Dosya alınamadı.', array( 'status' => 400 ) );
		}
		if ( ! empty( $file['error'] ) ) {
			return new WP_Error( 'psb_upload_error', 'Yükleme hatası (kod ' . (int) $file['error'] . ').', array( 'status' => 400 ) );
		}
		if ( isset( $file['size'] ) && (int) $file['size'] > self::MAX_BYTES ) {
			return new WP_Error( 'psb_too_large', 'Görsel 8 MB sınırını aşıyor.', array( 'status' => 413 ) );
		}

		// İçeriğe göre tür doğrulaması: uzantıya güvenmiyoruz.
		$check = wp_check_filetype_and_ext( $file['tmp_name'], $file['name'] );
		$type  = $check['type'] ? $check['type'] : '';
		if ( ! in_array( $type, self::ALLOWED_TYPES, true ) ) {
			return new WP_Error( 'psb_bad_type', 'Yalnızca JPEG, PNG, WebP veya GIF yüklenebilir.', array( 'status' => 415 ) );
		}

		require_once ABSPATH . 'wp-admin/includes/file.php';
		require_once ABSPATH . 'wp-admin/includes/media.php';
		require_once ABSPATH . 'wp-admin/includes/image.php';

		$attachment_id = media_handle_sideload(
			array(
				'name'     => sanitize_file_name( $file['name'] ),
				'type'     => $type,
				'tmp_name' => $file['tmp_name'],
				'error'    => 0,
				'size'     => isset( $file['size'] ) ? (int) $file['size'] : filesize( $file['tmp_name'] ),
			),
			(int) $post_id
		);

		if ( is_wp_error( $attachment_id ) ) {
			return $attachment_id;
		}
		return (int) $attachment_id;
	}

	/** Profil fotoğrafı: temanın `sl_custom_avatar` meta'sını güncelle. */
	public static function set_avatar( WP_User $user, $file ) {
		$attachment_id = self::sideload( $file, 0 );
		if ( is_wp_error( $attachment_id ) ) {
			return $attachment_id;
		}
		$url = wp_get_attachment_url( $attachment_id );
		if ( ! $url ) {
			return new WP_Error( 'psb_no_url', 'Yüklenen görselin adresi alınamadı.', array( 'status' => 500 ) );
		}
		update_user_meta( $user->ID, 'sl_custom_avatar', esc_url_raw( $url ) );
		update_user_meta( $user->ID, 'psb_avatar_attachment', $attachment_id );

		return array(
			'avatarUrl' => esc_url_raw( $url ),
		);
	}

	public static function clear_avatar( WP_User $user ) {
		delete_user_meta( $user->ID, 'sl_custom_avatar' );
		delete_user_meta( $user->ID, 'psb_avatar_attachment' );
		return array( 'avatarUrl' => PSB_Auth::avatar_url( $user->ID ) );
	}

	/** Oyun kapağı: öne çıkan görsel olarak ayarla. */
	public static function set_cover( $post_id, $file ) {
		$attachment_id = self::sideload( $file, (int) $post_id );
		if ( is_wp_error( $attachment_id ) ) {
			return $attachment_id;
		}
		set_post_thumbnail( (int) $post_id, $attachment_id );
		return array(
			'coverUrl' => PSB_Mapper::cover_url( (int) $post_id ),
		);
	}

	/** Ekran görüntüsü: `game_screenshots` listesine ekle. */
	public static function add_screenshot( $post_id, $file ) {
		$attachment_id = self::sideload( $file, (int) $post_id );
		if ( is_wp_error( $attachment_id ) ) {
			return $attachment_id;
		}
		$ids = self::screenshot_ids( $post_id );
		if ( count( $ids ) >= 24 ) {
			return new WP_Error( 'psb_too_many', 'En fazla 24 ekran görüntüsü eklenebilir.', array( 'status' => 400 ) );
		}
		$ids[] = $attachment_id;
		self::save_screenshot_ids( $post_id, $ids );

		return array( 'screenshotId' => $attachment_id );
	}

	public static function remove_screenshot( $post_id, $attachment_id ) {
		$attachment_id = (int) $attachment_id;
		$ids           = self::screenshot_ids( $post_id );
		if ( ! in_array( $attachment_id, $ids, true ) ) {
			return new WP_Error( 'psb_not_found', 'Ekran görüntüsü bu kayıtta bulunamadı.', array( 'status' => 404 ) );
		}
		self::save_screenshot_ids( $post_id, array_values( array_diff( $ids, array( $attachment_id ) ) ) );
		// Medya dosyası kütüphanede kalır: başka kayıtta kullanılıyor olabilir.
		return true;
	}

	private static function screenshot_ids( $post_id ) {
		$raw = (string) get_post_meta( (int) $post_id, 'game_screenshots', true );
		if ( '' === trim( $raw ) ) {
			return array();
		}
		$ids = array();
		foreach ( explode( ',', $raw ) as $piece ) {
			$piece = trim( $piece );
			if ( ctype_digit( $piece ) ) {
				$ids[] = (int) $piece;
			}
		}
		return array_values( array_unique( $ids ) );
	}

	private static function save_screenshot_ids( $post_id, array $ids ) {
		update_post_meta( (int) $post_id, 'game_screenshots', implode( ',', array_map( 'intval', $ids ) ) );
	}
}
