<?php
/**
 * Kayıtların okunması/yazılması ve uygulama şemasına dönüştürülmesi.
 *
 * Uygulama tarafındaki JSON şeması ile WordPress yazısı arasındaki eşleme burada tek yerde durur:
 *   id                -> post_name (slug)
 *   title             -> post_title
 *   longDescription   -> post_content
 *   shortDescription  -> meta (_ps_short_description)
 *   published         -> post_status (publish | draft)
 *   category          -> pixelstore_category taksonomisi
 *   diğer alanlar     -> meta
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_Repository {

	/** Kullanıcı rolünde yüke konmayan alanlar. */
	const ADMIN_ONLY_FIELDS = array( 'published', 'createdAt' );

	/**
	 * Katalog listesi.
	 *
	 * @param bool $include_drafts Yalnızca yönetici için true olmalı.
	 */
	public static function catalog( $include_drafts ) {
		$statuses = $include_drafts ? array( 'publish', 'draft', 'pending', 'private' ) : array( 'publish' );

		$query = new WP_Query(
			array(
				'post_type'      => PIXELSTORE_POST_TYPE,
				'post_status'    => $statuses,
				'posts_per_page' => 200,
				'orderby'        => 'title',
				'order'          => 'ASC',
				'no_found_rows'  => true,
			)
		);

		$apps = array();
		foreach ( $query->posts as $post ) {
			$apps[] = self::to_payload( $post, $include_drafts );
		}
		return $apps;
	}

	/** Slug'a göre kayıt bulur. */
	public static function find( $id, $include_drafts ) {
		$statuses = $include_drafts ? array( 'publish', 'draft', 'pending', 'private' ) : array( 'publish' );
		$posts    = get_posts(
			array(
				'name'           => sanitize_title( $id ),
				'post_type'      => PIXELSTORE_POST_TYPE,
				'post_status'    => $statuses,
				'posts_per_page' => 1,
			)
		);
		return $posts ? $posts[0] : null;
	}

	/** WordPress yazısını uygulama şemasına çevirir. */
	public static function to_payload( WP_Post $post, $include_admin_fields ) {
		$meta = function ( $key, $default = '' ) use ( $post ) {
			$value = get_post_meta( $post->ID, $key, true );
			return ( '' === $value || null === $value ) ? $default : $value;
		};

		$terms    = wp_get_post_terms( $post->ID, PIXELSTORE_TAXONOMY, array( 'fields' => 'names' ) );
		$category = ( ! is_wp_error( $terms ) && $terms ) ? $terms[0] : 'Diğer';

		$screenshots = json_decode( (string) $meta( PixelStore_CPT::META['screenshots'], '[]' ), true );
		if ( ! is_array( $screenshots ) ) {
			$screenshots = array();
		}

		$tags = json_decode( (string) $meta( PixelStore_CPT::META['tags'], '[]' ), true );
		if ( ! is_array( $tags ) ) {
			$tags = array();
		}

		$payload = array(
			'id'               => $post->post_name,
			'title'            => $post->post_title,
			'developer'        => (string) $meta( PixelStore_CPT::META['developer'], 'Bilinmeyen Stüdyo' ),
			'category'         => $category,
			'version'          => (string) $meta( PixelStore_CPT::META['version'], '1.0.0' ),
			'sizeMb'           => (float) $meta( PixelStore_CPT::META['sizeMb'], 12 ),
			'rating'           => (float) $meta( PixelStore_CPT::META['rating'], 0 ),
			'ratingCount'      => (int) $meta( PixelStore_CPT::META['ratingCount'], 0 ),
			'installs'         => (int) $meta( PixelStore_CPT::META['installs'], 0 ),
			'contentRating'    => (string) $meta( PixelStore_CPT::META['contentRating'], '7+' ),
			'updatedAt'        => get_the_modified_date( 'Y-m-d', $post ),
			'iconSeed'         => (string) $meta( PixelStore_CPT::META['iconSeed'], $post->post_name ),
			'palette'          => (string) $meta( PixelStore_CPT::META['palette'], 'slate' ),
			'tags'             => array_values( array_slice( array_map( 'strval', $tags ), 0, 6 ) ),
			'shortDescription' => (string) $meta( PixelStore_CPT::META['shortDescription'], '' ),
			'longDescription'  => self::plain_text( $post->post_content ),
			'downloadUrl'      => (string) $meta( PixelStore_CPT::META['downloadUrl'], '' ),
			'screenshots'      => self::sanitize_screenshots( $screenshots, $post->post_name ),
		);

		if ( $include_admin_fields ) {
			$payload['published'] = ( 'publish' === $post->post_status );
			$payload['createdAt'] = get_the_date( 'Y-m-d', $post );
		} else {
			// Kullanıcı rolünde yayın durumu bilgisi bile gönderilmez; liste zaten yalnız yayında olanları taşır.
			$payload['published'] = true;
		}

		return $payload;
	}

	/**
	 * Uygulamadan gelen yükü kaydeder.
	 *
	 * @return array|WP_Error Kaydedilen kaydın yükü.
	 */
	public static function save( array $input, $existing_id = null ) {
		$title = trim( (string) ( $input['title'] ?? '' ) );
		if ( '' === $title ) {
			return new WP_Error( 'ps_no_title', 'Başlık boş olamaz.', array( 'status' => 400 ) );
		}

		$post_id = null;
		if ( $existing_id ) {
			$existing = self::find( $existing_id, true );
			if ( ! $existing ) {
				return new WP_Error( 'ps_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
			}
			$post_id = $existing->ID;
		}

		$published = ! isset( $input['published'] ) || (bool) $input['published'];

		$postarr = array(
			'post_type'    => PIXELSTORE_POST_TYPE,
			'post_title'   => wp_strip_all_tags( mb_substr( $title, 0, 120 ) ),
			'post_content' => wp_kses_post( mb_substr( (string) ( $input['longDescription'] ?? '' ), 0, 8000 ) ),
			'post_status'  => $published ? 'publish' : 'draft',
		);

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

		// Tür: yoksa oluşturulur, böylece uygulamadan yeni tür de eklenebilir.
		$category = trim( (string) ( $input['category'] ?? '' ) );
		if ( '' !== $category ) {
			$term = term_exists( $category, PIXELSTORE_TAXONOMY );
			if ( ! $term ) {
				$term = wp_insert_term( mb_substr( $category, 0, 60 ), PIXELSTORE_TAXONOMY );
			}
			if ( ! is_wp_error( $term ) && isset( $term['term_id'] ) ) {
				wp_set_post_terms( $post_id, array( (int) $term['term_id'] ), PIXELSTORE_TAXONOMY, false );
			}
		}

		$map = PixelStore_CPT::META;

		update_post_meta( $post_id, $map['developer'], self::text( $input['developer'] ?? '', 80 ) );
		update_post_meta( $post_id, $map['version'], self::text( $input['version'] ?? '1.0.0', 24 ) );
		update_post_meta( $post_id, $map['sizeMb'], self::number( $input['sizeMb'] ?? 12, 0.1, 4096 ) );
		update_post_meta( $post_id, $map['rating'], self::number( $input['rating'] ?? 0, 0, 5 ) );
		update_post_meta( $post_id, $map['ratingCount'], (int) max( 0, min( 100000000, (int) ( $input['ratingCount'] ?? 0 ) ) ) );
		update_post_meta( $post_id, $map['contentRating'], self::text( $input['contentRating'] ?? '7+', 8 ) );
		update_post_meta( $post_id, $map['iconSeed'], self::text( $input['iconSeed'] ?? '', 48 ) );
		update_post_meta( $post_id, $map['palette'], self::text( $input['palette'] ?? 'slate', 24 ) );
		update_post_meta( $post_id, $map['shortDescription'], self::text( $input['shortDescription'] ?? '', 200 ) );

		// İndirme sayacı istemciden yazılamaz: yalnızca /install ucu artırır.
		if ( '' === get_post_meta( $post_id, $map['installs'], true ) ) {
			update_post_meta( $post_id, $map['installs'], 0 );
		}

		$download = trim( (string) ( $input['downloadUrl'] ?? '' ) );
		$download = ( $download && preg_match( '#^https?://#i', $download ) ) ? esc_url_raw( $download ) : '';
		update_post_meta( $post_id, $map['downloadUrl'], $download );

		$tags = array();
		if ( isset( $input['tags'] ) && is_array( $input['tags'] ) ) {
			foreach ( array_slice( $input['tags'], 0, 6 ) as $tag ) {
				$clean = self::text( $tag, 24 );
				if ( '' !== $clean ) {
					$tags[] = $clean;
				}
			}
		}
		update_post_meta( $post_id, $map['tags'], wp_json_encode( $tags ) );

		$shots = array();
		if ( isset( $input['screenshots'] ) && is_array( $input['screenshots'] ) ) {
			$shots = self::sanitize_screenshots( $input['screenshots'], get_post_field( 'post_name', $post_id ) );
		}
		update_post_meta( $post_id, $map['screenshots'], wp_json_encode( $shots ) );

		$post = get_post( $post_id );
		return self::to_payload( $post, true );
	}

	public static function delete( $id ) {
		$post = self::find( $id, true );
		if ( ! $post ) {
			return new WP_Error( 'ps_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		wp_delete_post( $post->ID, true );
		return true;
	}

	public static function set_published( $id, $published ) {
		$post = self::find( $id, true );
		if ( ! $post ) {
			return new WP_Error( 'ps_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		wp_update_post(
			array(
				'ID'          => $post->ID,
				'post_status' => $published ? 'publish' : 'draft',
			)
		);
		return (bool) $published;
	}

	/** İndirme sayacını artırır. Yalnızca yayında olan kayıtlar sayılır. */
	public static function record_install( $id ) {
		$post = self::find( $id, false );
		if ( ! $post ) {
			return new WP_Error( 'ps_not_found', 'Kayıt bulunamadı.', array( 'status' => 404 ) );
		}
		$key   = PixelStore_CPT::META['installs'];
		$value = (int) get_post_meta( $post->ID, $key, true ) + 1;
		update_post_meta( $post->ID, $key, $value );
		return $value;
	}

	public static function stats() {
		$posts = get_posts(
			array(
				'post_type'      => PIXELSTORE_POST_TYPE,
				'post_status'    => array( 'publish', 'draft', 'pending', 'private' ),
				'posts_per_page' => 500,
				'fields'         => 'ids',
			)
		);

		$installs     = 0;
		$published    = 0;
		$drafts       = 0;
		$rating_sum   = 0.0;
		$rating_count = 0;
		$per_category = array();

		foreach ( $posts as $post_id ) {
			$installs += (int) get_post_meta( $post_id, PixelStore_CPT::META['installs'], true );
			if ( 'publish' === get_post_status( $post_id ) ) {
				++$published;
			} else {
				++$drafts;
			}
			$rating = (float) get_post_meta( $post_id, PixelStore_CPT::META['rating'], true );
			if ( $rating > 0 ) {
				$rating_sum += $rating;
				++$rating_count;
			}
			$terms = wp_get_post_terms( $post_id, PIXELSTORE_TAXONOMY, array( 'fields' => 'names' ) );
			$name  = ( ! is_wp_error( $terms ) && $terms ) ? $terms[0] : 'Diğer';
			$per_category[ $name ] = ( $per_category[ $name ] ?? 0 ) + 1;
		}

		return array(
			'totalApps'     => count( $posts ),
			'published'     => $published,
			'drafts'        => $drafts,
			'totalInstalls' => $installs,
			'averageRating' => $rating_count ? round( $rating_sum / $rating_count, 1 ) : 0,
			'perCategory'   => $per_category ? $per_category : new stdClass(),
		);
	}

	/** Yönetim panelindeki kullanıcı listesi. Parola bilgisi hiç dokunulmaz. */
	public static function users() {
		$users = get_users( array( 'number' => 100 ) );
		$out   = array();
		foreach ( $users as $user ) {
			$out[] = array(
				'username'    => $user->user_login,
				'displayName' => $user->display_name ? $user->display_name : $user->user_login,
				'role'        => PixelStore_Auth::is_admin( $user ) ? 'admin' : 'user',
				'avatarSeed'  => 'wp-' . $user->ID . '-' . substr( md5( $user->user_login ), 0, 6 ),
				'joinedAt'    => mysql2date( 'Y-m-d', $user->user_registered ),
				'note'        => implode( ', ', array_map( 'strval', (array) $user->roles ) ),
			);
		}
		return $out;
	}

	/* ---- Yardımcılar ------------------------------------------------------------------------- */

	private static function sanitize_screenshots( array $input, $fallback_id ) {
		$allowed_scenes = array( 'title', 'field', 'battle', 'town', 'cave', 'menu' );
		$out            = array();
		$index          = 0;
		foreach ( array_slice( $input, 0, 8 ) as $raw ) {
			if ( ! is_array( $raw ) ) {
				continue;
			}
			$scene = isset( $raw['scene'] ) ? (string) $raw['scene'] : 'field';
			$out[] = array(
				'seed'    => self::text( $raw['seed'] ?? ( $fallback_id . '-' . $index ), 48 ),
				'kind'    => ( isset( $raw['kind'] ) && 'tablet' === $raw['kind'] ) ? 'tablet' : 'phone',
				'scene'   => in_array( $scene, $allowed_scenes, true ) ? $scene : 'field',
				'caption' => self::text( $raw['caption'] ?? '', 80 ),
			);
			++$index;
		}
		return $out;
	}

	private static function text( $value, $limit ) {
		return mb_substr( wp_strip_all_tags( trim( (string) $value ) ), 0, $limit );
	}

	private static function number( $value, $min, $max ) {
		$number = (float) $value;
		return max( $min, min( $max, $number ) );
	}

	/** Editör içeriğini uygulamanın beklediği düz metne çevirir (paragraflar satır atlaması olur). */
	private static function plain_text( $content ) {
		$text = str_ireplace( array( '</p>', '<br>', '<br/>', '<br />' ), "\n", (string) $content );
		$text = wp_strip_all_tags( $text );
		$text = preg_replace( "/\n{3,}/", "\n\n", $text );
		return trim( html_entity_decode( $text, ENT_QUOTES, 'UTF-8' ) );
	}
}
