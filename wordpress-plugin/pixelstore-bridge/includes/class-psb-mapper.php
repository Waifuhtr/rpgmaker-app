<?php
/**
 * SteamLike oyun kaydını uygulamanın beklediği JSON'a çevirir.
 *
 * Uygulama ile site arasındaki tek sözleşme burada durur. Alan adları temanın meta anahtarlarıyla
 * birebir eşlenir; eklenti yeni bir şema uydurmaz:
 *
 *   id                -> post_name (slug)          coverUrl        -> öne çıkan görsel
 *   title             -> post_title                screenshots     -> game_screenshots (medya ID'leri)
 *   description       -> post_content               sizeLabel       -> game_size ("45 GB" gibi serbest metin)
 *   rating            -> sl_user_rating_avg         downloadCount   -> game_download_count
 *   platform/dil/tür  -> game_* taksonomileri       requirements    -> minimum_* / recommended_*
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Mapper {

	/** Liste kartları için hafif yük. */
	public static function summary( WP_Post $post, $user_id = 0 ) {
		$id = $post->ID;

		return array(
			'id'             => $post->post_name,
			'postId'         => $id,
			'title'          => get_the_title( $post ),
			'subtitle'       => (string) get_post_meta( $id, 'game_subtitle', true ),
			'excerpt'        => self::excerpt( $post ),
			'coverUrl'       => self::cover_url( $id, 'steamlike-cover' ),
			'developer'      => self::first_term( $id, 'game_developer' ),
			'publisher'      => self::first_term( $id, 'game_publisher' ),
			'platform'       => self::first_term( $id, 'game_platform' ),
			'language'       => self::first_term( $id, 'game_language' ),
			'status'         => self::first_term( $id, 'game_status' ),
			'genres'         => self::term_names( $id, 'game_genre' ),
			'version'        => (string) get_post_meta( $id, 'game_version', true ),
			'sizeLabel'      => (string) ( get_post_meta( $id, 'game_size', true ) ?: '' ),
			'rating'         => (float) ( get_post_meta( $id, 'sl_user_rating_avg', true ) ?: 0 ),
			'ratingCount'    => (int) get_post_meta( $id, 'sl_user_rating_count', true ),
			'downloadCount'  => (int) get_post_meta( $id, 'game_download_count', true ),
			'viewCount'      => (int) get_post_meta( $id, 'game_view_count', true ),
			'featured'       => '1' === (string) get_post_meta( $id, 'game_featured', true ),
			'editorsChoice'  => '1' === (string) get_post_meta( $id, 'game_editors_choice', true ),
			'published'      => 'publish' === $post->post_status,
			'updatedAt'      => get_the_modified_date( 'Y-m-d', $post ),
			'favorited'      => $user_id ? PSB_Social::is_favorite( $user_id, $id ) : false,
		);
	}

	/** Detay ekranı ve yönetim düzenleyicisi için tam yük. */
	public static function detail( WP_Post $post, $user_id = 0 ) {
		$id      = $post->ID;
		$payload = self::summary( $post, $user_id );

		$payload['description']     = self::plain_text( $post->post_content );
		$payload['changelog']       = self::plain_text( get_post_meta( $id, 'game_changelog', true ) );
		$payload['installGuide']    = self::plain_text( get_post_meta( $id, 'game_installation_guide', true ) );
		$payload['heroUrl']         = self::hero_url( $id );
		$payload['screenshots']     = self::screenshots( $id );
		$payload['trailerUrl']      = (string) get_post_meta( $id, 'game_trailer_url', true );
		$payload['downloadUrl']     = (string) get_post_meta( $id, 'game_download_url', true );
		$payload['mirrorUrl']       = (string) get_post_meta( $id, 'game_download_mirror', true );
		$payload['archivePassword'] = (string) get_post_meta( $id, 'game_download_password', true );
		$payload['releaseDate']     = (string) get_post_meta( $id, 'game_release_date', true );
		$payload['ageRating']       = (string) get_post_meta( $id, 'game_age_rating', true );
		$payload['licenseType']     = (string) get_post_meta( $id, 'game_license_type', true );
		$payload['multiplayer']     = '1' === (string) get_post_meta( $id, 'game_multiplayer_support', true );
		$payload['controller']      = '1' === (string) get_post_meta( $id, 'game_controller_support', true );
		$payload['features']        = self::term_names( $id, 'game_features' );
		$payload['platforms']       = self::term_names( $id, 'game_platform' );
		$payload['languages']       = self::term_names( $id, 'game_language' );
		$payload['tags']            = self::tag_names( $id );
		$payload['permalink']       = get_permalink( $post );
		$payload['requirements']    = self::requirements( $id );
		$payload['userRating']      = $user_id ? PSB_Social::user_rating( $user_id, $id ) : 0;
		$payload['reviewCount']     = (int) get_comments(
			array( 'post_id' => $id, 'type' => 'review', 'status' => 'approve', 'count' => true )
		);
		$payload['hasReviewed']     = $user_id ? PSB_Social::has_reviewed( $user_id, $id ) : false;

		return $payload;
	}

	/* ---- Parçalar ---------------------------------------------------------------------------- */

	public static function cover_url( $post_id, $size = 'steamlike-cover' ) {
		$url = get_the_post_thumbnail_url( $post_id, $size );
		if ( ! $url ) {
			$url = get_the_post_thumbnail_url( $post_id, 'large' );
		}
		return $url ? $url : '';
	}

	/** Detay ekranının arka planı: özel arka plan görseli, yoksa tam boy kapak. */
	private static function hero_url( $post_id ) {
		$custom = get_post_meta( $post_id, 'game_background_image', true );
		if ( $custom ) {
			return esc_url_raw( $custom );
		}
		$url = get_the_post_thumbnail_url( $post_id, 'full' );
		return $url ? $url : '';
	}

	/**
	 * `game_screenshots` virgülle ayrılmış medya ID listesidir. Uygulamaya hem küçük hem tam boy
	 * adres gönderilir: liste küçüğü, tam ekran görüntüleyici büyüğü kullanır.
	 */
	private static function screenshots( $post_id ) {
		$raw = (string) get_post_meta( $post_id, 'game_screenshots', true );
		if ( '' === trim( $raw ) ) {
			return array();
		}
		$out = array();
		foreach ( array_slice( array_filter( array_map( 'trim', explode( ',', $raw ) ) ), 0, 24 ) as $attachment_id ) {
			if ( ! ctype_digit( (string) $attachment_id ) ) {
				continue;
			}
			$full  = wp_get_attachment_image_url( (int) $attachment_id, 'full' );
			$thumb = wp_get_attachment_image_url( (int) $attachment_id, 'large' );
			if ( ! $full ) {
				continue;
			}
			$out[] = array(
				'id'       => (int) $attachment_id,
				'url'      => $thumb ? $thumb : $full,
				'fullUrl'  => $full,
			);
		}
		return $out;
	}

	private static function requirements( $post_id ) {
		$read = function ( $key ) use ( $post_id ) {
			return (string) get_post_meta( $post_id, $key, true );
		};

		$min = array(
			'os'      => $read( 'minimum_os' ),
			'cpu'     => $read( 'minimum_cpu' ),
			'ram'     => $read( 'minimum_ram' ),
			'gpu'     => $read( 'minimum_gpu' ),
			'storage' => $read( 'minimum_storage' ),
		);
		$rec = array(
			'os'      => $read( 'recommended_os' ),
			'cpu'     => $read( 'recommended_cpu' ),
			'ram'     => $read( 'recommended_ram' ),
			'gpu'     => $read( 'recommended_gpu' ),
			'storage' => $read( 'recommended_storage' ),
		);

		return array(
			'minimum'     => $min,
			'recommended' => $rec,
			'hasMinimum'  => '' !== implode( '', $min ),
			'hasRecommended' => '' !== implode( '', $rec ),
		);
	}

	public static function first_term( $post_id, $taxonomy ) {
		$terms = get_the_terms( $post_id, $taxonomy );
		if ( is_wp_error( $terms ) || empty( $terms ) ) {
			return '';
		}
		return $terms[0]->name;
	}

	public static function term_names( $post_id, $taxonomy ) {
		$terms = get_the_terms( $post_id, $taxonomy );
		if ( is_wp_error( $terms ) || empty( $terms ) ) {
			return array();
		}
		return array_values( wp_list_pluck( $terms, 'name' ) );
	}

	private static function tag_names( $post_id ) {
		$tags = get_the_tags( $post_id );
		if ( is_wp_error( $tags ) || empty( $tags ) ) {
			return array();
		}
		return array_values( wp_list_pluck( $tags, 'name' ) );
	}

	private static function excerpt( WP_Post $post ) {
		$excerpt = has_excerpt( $post ) ? $post->post_excerpt : $post->post_content;
		$text    = self::plain_text( $excerpt );
		return wp_trim_words( $text, 26, '…' );
	}

	/**
	 * Editör içeriğini düz metne çevirir. Uygulama HTML işlemez; paragraflar satır atlamasına
	 * dönüşür, liste maddeleri madde imiyle korunur.
	 *
	 * `the_content` filtresi bilinçli olarak uygulanmaz: REST bağlamında başka eklentilerin
	 * araya girmesi (reklam, script) düz metni kirletirdi.
	 */
	public static function plain_text( $content ) {
		$text = (string) $content;
		if ( '' === trim( $text ) ) {
			return '';
		}
		if ( function_exists( 'do_blocks' ) ) {
			$text = do_blocks( $text );
		}
		$text = str_ireplace( array( '<li>', '<li ' ), array( "\n• ", "\n• <" ), $text );
		$text = str_ireplace(
			array( '</p>', '<br>', '<br/>', '<br />', '</div>', '</h1>', '</h2>', '</h3>', '</h4>', '</li>', '</ul>', '</ol>' ),
			"\n",
			$text
		);
		$text = wp_strip_all_tags( $text );
		$text = html_entity_decode( $text, ENT_QUOTES, 'UTF-8' );
		$text = preg_replace( "/[ \t]+\n/", "\n", $text );
		$text = preg_replace( "/\n{3,}/", "\n\n", $text );
		return trim( $text );
	}
}
