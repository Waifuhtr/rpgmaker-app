<?php
/**
 * Uygulamadan oyun ekleme / düzenleme / silme (yalnızca yönetici).
 *
 * Yazılan her alan temanın meta anahtarına gider, böylece uygulamadan eklenen kayıt wp-admin'de
 * de eksiksiz görünür ve tema şablonları onu normal bir oyun gibi işler.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Write {

	/** Serbest metin meta alanları: anahtar => karakter sınırı. */
	const TEXT_META = array(
		'game_subtitle'           => 160,
		'game_version'            => 40,
		'game_size'               => 40,
		'game_release_date'       => 20,
		'game_age_rating'         => 20,
		'game_license_type'       => 60,
		'game_api_id'             => 60,
		'minimum_os'              => 120,
		'minimum_cpu'             => 120,
		'minimum_ram'             => 60,
		'minimum_gpu'             => 120,
		'minimum_storage'         => 60,
		'recommended_os'          => 120,
		'recommended_cpu'         => 120,
		'recommended_ram'         => 60,
		'recommended_gpu'         => 120,
		'recommended_storage'     => 60,
		'game_download_password'  => 120,
		'game_changelog'          => 5000,
		'game_installation_guide' => 5000,
	);

	const URL_META = array( 'game_download_url', 'game_download_mirror', 'game_trailer_url', 'game_background_image' );

	const BOOL_META = array( 'game_multiplayer_support', 'game_controller_support', 'game_featured', 'game_editors_choice' );

	/** Uygulamadan yazılabilen taksonomiler. Terim yoksa oluşturulur. */
	const WRITABLE_TAXONOMIES = array(
		'genres'    => 'game_genre',
		'platforms' => 'game_platform',
		'languages' => 'game_language',
		'features'  => 'game_features',
	);

	/** Tek terimli taksonomiler. */
	const SINGLE_TAXONOMIES = array(
		'developer' => 'game_developer',
		'publisher' => 'game_publisher',
		'status'    => 'game_status',
	);

	/**
	 * @return array|WP_Error Kaydedilen oyunun tam yükü.
	 */
	public static function save( array $input, $existing_identifier = null, $user_id = 0 ) {
		$title = trim( wp_strip_all_tags( (string) ( $input['title'] ?? '' ) ) );
		if ( '' === $title ) {
			return new WP_Error( 'psb_no_title', 'Başlık boş olamaz.', array( 'status' => 400 ) );
		}

		$post_id = null;
		if ( $existing_identifier ) {
			$existing = PSB_Query::find( $existing_identifier, true );
			if ( ! $existing ) {
				return new WP_Error( 'psb_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
			}
			$post_id = $existing->ID;
		}

		$published = ! isset( $input['published'] ) || (bool) $input['published'];

		$postarr = array(
			'post_type'    => PSB_POST_TYPE,
			'post_title'   => mb_substr( $title, 0, 160 ),
			'post_content' => wp_kses_post( (string) ( $input['description'] ?? '' ) ),
			'post_status'  => $published ? 'publish' : 'draft',
		);
		if ( isset( $input['excerpt'] ) ) {
			$postarr['post_excerpt'] = mb_substr( wp_strip_all_tags( (string) $input['excerpt'] ), 0, 400 );
		}

		if ( $post_id ) {
			$postarr['ID'] = $post_id;
			$result        = wp_update_post( $postarr, true );
		} else {
			$result = wp_insert_post( $postarr, true );
		}
		if ( is_wp_error( $result ) ) {
			return $result;
		}
		$post_id = (int) $result;

		self::save_meta( $post_id, $input );
		self::save_taxonomies( $post_id, $input );

		$post = get_post( $post_id );
		return PSB_Mapper::detail( $post, $user_id );
	}

	private static function save_meta( $post_id, array $input ) {
		foreach ( self::TEXT_META as $key => $limit ) {
			if ( ! array_key_exists( $key, $input ) ) {
				continue;
			}
			$value = mb_substr( wp_strip_all_tags( (string) $input[ $key ] ), 0, $limit );
			update_post_meta( $post_id, $key, $value );
		}

		foreach ( self::URL_META as $key ) {
			if ( ! array_key_exists( $key, $input ) ) {
				continue;
			}
			$raw = trim( (string) $input[ $key ] );
			// Yalnızca http(s) kabul edilir; javascript: gibi şemalar düşer.
			$url = ( $raw && preg_match( '#^https?://#i', $raw ) ) ? esc_url_raw( $raw ) : '';
			update_post_meta( $post_id, $key, $url );
		}

		foreach ( self::BOOL_META as $key ) {
			if ( ! array_key_exists( $key, $input ) ) {
				continue;
			}
			update_post_meta( $post_id, $key, ! empty( $input[ $key ] ) ? '1' : '0' );
		}

		// Puan ve sayaçlar uygulamadan yazılamaz: puan kullanıcı oylarından, sayaçlar olaylardan gelir.
	}

	private static function save_taxonomies( $post_id, array $input ) {
		foreach ( self::WRITABLE_TAXONOMIES as $field => $taxonomy ) {
			if ( ! array_key_exists( $field, $input ) || ! taxonomy_exists( $taxonomy ) ) {
				continue;
			}
			$names = is_array( $input[ $field ] ) ? $input[ $field ] : array( $input[ $field ] );
			self::assign_terms( $post_id, $taxonomy, $names );
		}

		foreach ( self::SINGLE_TAXONOMIES as $field => $taxonomy ) {
			if ( ! array_key_exists( $field, $input ) || ! taxonomy_exists( $taxonomy ) ) {
				continue;
			}
			$name = trim( wp_strip_all_tags( (string) $input[ $field ] ) );
			self::assign_terms( $post_id, $taxonomy, '' === $name ? array() : array( $name ) );
		}

		if ( array_key_exists( 'tags', $input ) ) {
			// Etiketler de aynı yoldan geçer: isim yerine terim ID'si atamak belirsizlik bırakmaz.
			self::assign_terms( $post_id, 'post_tag', (array) $input['tags'], 15 );
		}
	}

	/** Terim adları verilir; olmayan terim oluşturulur (uygulamadan yeni tür eklenebilsin). */
	private static function assign_terms( $post_id, $taxonomy, array $names, $limit = 12 ) {
		$term_ids = array();
		foreach ( array_slice( $names, 0, $limit ) as $name ) {
			$name = trim( wp_strip_all_tags( (string) $name ) );
			if ( '' === $name ) {
				continue;
			}
			$term = term_exists( $name, $taxonomy );
			if ( ! $term ) {
				$term = wp_insert_term( mb_substr( $name, 0, 80 ), $taxonomy );
			}
			if ( ! is_wp_error( $term ) && isset( $term['term_id'] ) ) {
				$term_ids[] = (int) $term['term_id'];
			}
		}
		wp_set_post_terms( $post_id, $term_ids, $taxonomy, false );
	}

	public static function delete( $identifier ) {
		$post = PSB_Query::find( $identifier, true );
		if ( ! $post ) {
			return new WP_Error( 'psb_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		// Çöp kutusuna taşınır, kalıcı silinmez: yanlış dokunuş veri kaybına yol açmasın.
		wp_trash_post( $post->ID );
		return true;
	}

	public static function set_published( $identifier, $published ) {
		$post = PSB_Query::find( $identifier, true );
		if ( ! $post ) {
			return new WP_Error( 'psb_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		wp_update_post(
			array(
				'ID'          => $post->ID,
				'post_status' => $published ? 'publish' : 'draft',
			)
		);
		return (bool) $published;
	}
}
