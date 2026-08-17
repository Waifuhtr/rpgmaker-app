<?php
/**
 * Demo kataloğunu kurar.
 *
 * Tohum veri `data/catalog-seed.json` dosyasından okunur; bu dosya Android projesindeki
 * `app/src/main/assets/catalog_seed.json` ile aynı içeriktir, böylece yerel kip ile WordPress kipi
 * aynı demo kataloğu gösterir.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_Seed {

	/**
	 * @param bool $replace true ise mevcut PixelStore kayıtları silinir.
	 * @return int Yüklenen kayıt sayısı.
	 */
	public static function install( $replace = false ) {
		$data = self::read();
		if ( ! $data ) {
			return 0;
		}

		PixelStore_CPT::ensure_default_terms();

		if ( $replace ) {
			self::purge();
		}

		// Türleri tohum verideki glifleriyle güncelle.
		if ( ! empty( $data['categories'] ) && is_array( $data['categories'] ) ) {
			foreach ( $data['categories'] as $category ) {
				if ( empty( $category['name'] ) ) {
					continue;
				}
				$term = term_exists( $category['name'], PIXELSTORE_TAXONOMY );
				if ( ! $term ) {
					$term = wp_insert_term( $category['name'], PIXELSTORE_TAXONOMY );
				}
				if ( ! is_wp_error( $term ) && isset( $term['term_id'] ) && ! empty( $category['glyph'] ) ) {
					update_term_meta( $term['term_id'], '_ps_glyph', sanitize_key( $category['glyph'] ) );
				}
			}
		}

		$count = 0;
		foreach ( (array) ( $data['apps'] ?? array() ) as $app ) {
			if ( empty( $app['title'] ) ) {
				continue;
			}
			$existing = ! empty( $app['id'] ) ? PixelStore_Repository::find( $app['id'], true ) : null;
			$result   = PixelStore_Repository::save( $app, $existing ? $app['id'] : null );
			if ( is_wp_error( $result ) ) {
				continue;
			}

			// Yeni kayıtta slug'ı tohumdaki id ile hizala; uygulama id'leri sabit kalsın.
			if ( ! $existing && ! empty( $app['id'] ) ) {
				$post = PixelStore_Repository::find( $result['id'], true );
				if ( $post && $post->post_name !== $app['id'] ) {
					wp_update_post(
						array(
							'ID'        => $post->ID,
							'post_name' => sanitize_title( $app['id'] ),
						)
					);
				}
			}

			// İndirme sayacı tohum veriden gelir; /install ucu bunun üstüne ekler.
			$post = PixelStore_Repository::find( $app['id'] ?? $result['id'], true );
			if ( $post && isset( $app['installs'] ) ) {
				update_post_meta( $post->ID, PixelStore_CPT::META['installs'], (int) $app['installs'] );
			}

			++$count;
		}

		update_option( 'pixelstore_seeded_at', current_time( 'mysql' ) );
		return $count;
	}

	/** Tüm PixelStore kayıtlarını siler. Diğer içerik türlerine dokunmaz. */
	public static function purge() {
		$posts = get_posts(
			array(
				'post_type'      => PIXELSTORE_POST_TYPE,
				'post_status'    => 'any',
				'posts_per_page' => 500,
				'fields'         => 'ids',
			)
		);
		foreach ( $posts as $post_id ) {
			wp_delete_post( $post_id, true );
		}
		return count( $posts );
	}

	private static function read() {
		$path = PIXELSTORE_PLUGIN_DIR . 'data/catalog-seed.json';
		if ( ! file_exists( $path ) ) {
			return null;
		}
		$json = json_decode( (string) file_get_contents( $path ), true );
		return is_array( $json ) ? $json : null;
	}
}
