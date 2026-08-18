<?php
/**
 * Otomatik WebP Dönüştürücü
 *
 * Siteye yüklenen tüm JPG / JPEG görsellerini, yükleme anında otomatik olarak
 * WebP formatına dönüştürür. PNG (ve diğer formatlar) bu işlemin dışında tutulur,
 * oldukları gibi kalır. Bu dönüştürme; medya kütüphanesi, kapak/oyun görselleri,
 * avatar / profil kapağı ve rehber / rapor ekran görüntüsü yüklemeleri dahil,
 * WordPress'in standart yükleme mekanizmasını (wp_handle_upload) kullanan HER
 * yükleme noktasında geçerlidir.
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * WordPress'in eski sürümlerinde webp mime tipinin yüklenebilir listede
 * olduğundan emin olur (WP 5.8+ zaten destekler, burada güvenlik amaçlı).
 */
add_filter( 'upload_mimes', 'sl_webp_allow_mime' );
function sl_webp_allow_mime( $mimes ) {
	$mimes['webp'] = 'image/webp';
	return $mimes;
}

/**
 * Bazı sunucularda dosya türü kontrolü mime type ile eşleşmeyebiliyor,
 * webp'nin doğru tanınması için ek güvence.
 */
add_filter( 'wp_check_filetype_and_ext', 'sl_webp_check_filetype', 10, 5 );
function sl_webp_check_filetype( $data, $file, $filename, $mimes, $real_mime = '' ) {
	if ( empty( $data['ext'] ) && preg_match( '/\.webp$/i', $filename ) ) {
		$data['ext']             = 'webp';
		$data['type']            = 'image/webp';
		$data['proper_filename'] = $filename;
	}
	return $data;
}

/**
 * Ana dönüştürme işlemi. wp_handle_upload tüm standart yükleme akışlarında
 * (medya kütüphanesi + media_handle_upload kullanan ön yüz formları) çalışır.
 */
add_filter( 'wp_handle_upload', 'sl_convert_jpeg_to_webp_on_upload' );
function sl_convert_jpeg_to_webp_on_upload( $upload ) {

	// Sadece JPG / JPEG dosyalarını hedef al. PNG ve diğer formatlar dokunulmadan geçer.
	if ( empty( $upload['type'] ) || ! in_array( $upload['type'], array( 'image/jpeg', 'image/jpg' ), true ) ) {
		return $upload;
	}

	if ( empty( $upload['file'] ) || ! file_exists( $upload['file'] ) ) {
		return $upload;
	}

	$source_path = $upload['file'];
	$webp_path   = preg_replace( '/\.(jpe?g)$/i', '.webp', $source_path );

	// Aynı isimde webp zaten varsa (nadiren), üzerine yazmamak için benzersiz bir isim üret.
	if ( file_exists( $webp_path ) ) {
		$webp_path = preg_replace( '/\.(jpe?g)$/i', '-' . wp_generate_password( 6, false ) . '.webp', $source_path );
	}

	$converted = false;

	// Öncelik: GD kütüphanesi (çoğu paylaşımlı barındırmada mevcuttur).
	if ( function_exists( 'imagecreatefromjpeg' ) && function_exists( 'imagewebp' ) ) {
		$converted = sl_convert_with_gd( $source_path, $webp_path );
	}

	// GD mevcut değilse veya başarısız olursa Imagick ile dene.
	if ( ! $converted && class_exists( 'Imagick' ) ) {
		$converted = sl_convert_with_imagick( $source_path, $webp_path );
	}

	if ( $converted && file_exists( $webp_path ) ) {
		// Orijinal JPG dosyasını sil, WordPress'e artık webp dosyasını kullanmasını söyle.
		@unlink( $source_path );

		$upload['file'] = $webp_path;
		$upload['url']  = preg_replace( '/\.(jpe?g)$/i', '', $upload['url'] ) . '.webp';
		$upload['type'] = 'image/webp';

		// Dosya adı benzersizleştirildiyse url'nin de doğru şekilde güncellendiğinden emin ol.
		$upload['url'] = trailingslashit( dirname( $upload['url'] ) ) . basename( $webp_path );
	}

	return $upload;
}

/**
 * GD kütüphanesi ile JPG -> WebP dönüştürme. EXIF döndürme bilgisi varsa uygular.
 */
function sl_convert_with_gd( $source_path, $webp_path ) {
	$image = @imagecreatefromjpeg( $source_path );
	if ( ! $image ) {
		return false;
	}

	// Telefon kameralarından gelen EXIF döndürme bilgisini uygula (varsa).
	if ( function_exists( 'exif_read_data' ) ) {
		$exif = @exif_read_data( $source_path );
		if ( ! empty( $exif['Orientation'] ) ) {
			switch ( (int) $exif['Orientation'] ) {
				case 3:
					$image = imagerotate( $image, 180, 0 );
					break;
				case 6:
					$image = imagerotate( $image, -90, 0 );
					break;
				case 8:
					$image = imagerotate( $image, 90, 0 );
					break;
			}
		}
	}

	$success = imagewebp( $image, $webp_path, 85 );
	imagedestroy( $image );

	return (bool) $success;
}

/**
 * Imagick ile JPG -> WebP dönüştürme (GD kullanılamadığı durumlar için yedek yol).
 */
function sl_convert_with_imagick( $source_path, $webp_path ) {
	try {
		$imagick = new Imagick( $source_path );
		$imagick->setImageFormat( 'webp' );
		$imagick->setImageCompressionQuality( 85 );
		$imagick->stripImage(); // Gereksiz meta veriyi temizler, dosya boyutunu küçültür.
		$result = $imagick->writeImage( $webp_path );
		$imagick->clear();
		$imagick->destroy();
		return (bool) $result;
	} catch ( Exception $e ) {
		return false;
	}
}
