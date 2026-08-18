<?php
/**
 * Oyun listeleme, filtreleme, canlı arama ve taksonomi listeleri.
 *
 * Filtre ve sıralama seçenekleri temanın AJAX filtresiyle (inc/ajax.php) aynı mantığı izler,
 * böylece uygulama ile site aynı sonuçları verir.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Query {

	const MAX_PER_PAGE = 40;

	/** Uygulamada filtre çipi olarak gösterilen taksonomiler. */
	const FILTER_TAXONOMIES = array( 'game_genre', 'game_platform', 'game_language', 'game_status' );

	public static function games( WP_REST_Request $request, $is_admin, $user_id ) {
		$per_page = (int) ( $request->get_param( 'per_page' ) ?: 20 );
		$per_page = max( 1, min( self::MAX_PER_PAGE, $per_page ) );
		$page     = max( 1, (int) ( $request->get_param( 'page' ) ?: 1 ) );

		$args = array(
			'post_type'      => PSB_POST_TYPE,
			'posts_per_page' => $per_page,
			'paged'          => $page,
			// Taslaklar yalnızca yöneticiye görünür.
			'post_status'    => $is_admin ? array( 'publish', 'draft', 'pending', 'private' ) : array( 'publish' ),
		);

		$search = trim( (string) $request->get_param( 'search' ) );
		if ( '' !== $search ) {
			$args['s'] = $search;
		}

		$tax_query = array( 'relation' => 'AND' );
		foreach ( self::FILTER_TAXONOMIES as $taxonomy ) {
			$value = $request->get_param( $taxonomy );
			if ( is_array( $value ) ) {
				$value = array_map( 'sanitize_title', $value );
			} elseif ( null !== $value && '' !== $value ) {
				$value = array( sanitize_title( (string) $value ) );
			} else {
				continue;
			}
			if ( empty( $value ) ) {
				continue;
			}
			$tax_query[] = array(
				'taxonomy' => $taxonomy,
				'field'    => 'slug',
				'terms'    => $value,
			);
		}
		if ( count( $tax_query ) > 1 ) {
			$args['tax_query'] = $tax_query;
		}

		if ( '1' === (string) $request->get_param( 'featured' ) ) {
			$args['meta_query'] = array(
				array(
					'key'   => 'game_featured',
					'value' => '1',
				),
			);
		}

		switch ( (string) $request->get_param( 'sort' ) ) {
			case 'oldest':
				$args['orderby'] = 'date';
				$args['order']   = 'ASC';
				break;
			case 'rating':
				$args['meta_key'] = 'sl_user_rating_avg';
				$args['orderby']  = 'meta_value_num';
				$args['order']    = 'DESC';
				break;
			case 'downloads':
				$args['meta_key'] = 'game_download_count';
				$args['orderby']  = 'meta_value_num';
				$args['order']    = 'DESC';
				break;
			case 'title':
				$args['orderby'] = 'title';
				$args['order']   = 'ASC';
				break;
			case 'newest':
			default:
				$args['orderby'] = 'date';
				$args['order']   = 'DESC';
				break;
		}

		$query = new WP_Query( $args );
		$games = array();
		foreach ( $query->posts as $post ) {
			$games[] = PSB_Mapper::summary( $post, $user_id );
		}

		return array(
			'games' => $games,
			'page'  => $page,
			'pages' => (int) $query->max_num_pages,
			'total' => (int) $query->found_posts,
		);
	}

	/** Canlı arama: yalnızca kart için gereken alanlar döner, yük küçük tutulur. */
	public static function live_search( $keyword, $limit = 8 ) {
		$keyword = trim( (string) $keyword );
		if ( mb_strlen( $keyword ) < 2 ) {
			return array();
		}

		$query = new WP_Query(
			array(
				'post_type'      => PSB_POST_TYPE,
				'post_status'    => 'publish',
				'posts_per_page' => max( 1, min( 12, (int) $limit ) ),
				's'              => $keyword,
				'no_found_rows'  => true,
			)
		);

		$out = array();
		foreach ( $query->posts as $post ) {
			$out[] = array(
				'id'       => $post->post_name,
				'postId'   => $post->ID,
				'title'    => get_the_title( $post ),
				'coverUrl' => PSB_Mapper::cover_url( $post->ID ),
				'platform' => PSB_Mapper::first_term( $post->ID, 'game_platform' ),
				'rating'   => (float) ( get_post_meta( $post->ID, 'sl_user_rating_avg', true ) ?: 0 ),
			);
		}
		return $out;
	}

	/**
	 * Filtre çipleri için taksonomi listeleri. Terim ikonu/bayrağı temanın `sl_term_icon`
	 * meta'sından gelir (emoji veya görsel adresi olabilir).
	 */
	public static function taxonomies() {
		$out = array();
		foreach ( self::FILTER_TAXONOMIES as $taxonomy ) {
			if ( ! taxonomy_exists( $taxonomy ) ) {
				continue;
			}
			$terms = get_terms(
				array(
					'taxonomy'   => $taxonomy,
					'hide_empty' => true,
					'orderby'    => 'count',
					'order'      => 'DESC',
					'number'     => 60,
				)
			);
			if ( is_wp_error( $terms ) ) {
				continue;
			}
			$items = array();
			foreach ( $terms as $term ) {
				$icon = (string) get_term_meta( $term->term_id, 'sl_term_icon', true );
				$items[] = array(
					'slug'  => $term->slug,
					'name'  => $term->name,
					'count' => (int) $term->count,
					// Emoji ise metin, http ile başlıyorsa görsel adresi.
					'icon'  => ( $icon && 0 !== strpos( $icon, 'http' ) ) ? $icon : '',
					'iconUrl' => ( $icon && 0 === strpos( $icon, 'http' ) ) ? esc_url_raw( $icon ) : '',
				);
			}
			$out[ $taxonomy ] = $items;
		}
		return $out;
	}

	/** Slug'a göre oyun bulur. Sayısal gelirse post ID olarak da denenir. */
	public static function find( $identifier, $include_drafts ) {
		$identifier = (string) $identifier;
		$statuses   = $include_drafts ? array( 'publish', 'draft', 'pending', 'private' ) : array( 'publish' );

		if ( ctype_digit( $identifier ) ) {
			$post = get_post( (int) $identifier );
			if ( $post && PSB_POST_TYPE === $post->post_type && in_array( $post->post_status, $statuses, true ) ) {
				return $post;
			}
		}

		$posts = get_posts(
			array(
				'name'           => sanitize_title( $identifier ),
				'post_type'      => PSB_POST_TYPE,
				'post_status'    => $statuses,
				'posts_per_page' => 1,
			)
		);
		return $posts ? $posts[0] : null;
	}

	public static function stats() {
		$counts = wp_count_posts( PSB_POST_TYPE );
		$ids    = get_posts(
			array(
				'post_type'      => PSB_POST_TYPE,
				'post_status'    => array( 'publish', 'draft', 'pending', 'private' ),
				'posts_per_page' => 2000,
				'fields'         => 'ids',
			)
		);

		$downloads    = 0;
		$views        = 0;
		$rating_sum   = 0.0;
		$rating_count = 0;
		$per_genre    = array();

		foreach ( $ids as $id ) {
			$downloads += (int) get_post_meta( $id, 'game_download_count', true );
			$views     += (int) get_post_meta( $id, 'game_view_count', true );
			$avg        = (float) get_post_meta( $id, 'sl_user_rating_avg', true );
			if ( $avg > 0 ) {
				$rating_sum += $avg;
				++$rating_count;
			}
			foreach ( PSB_Mapper::term_names( $id, 'game_genre' ) as $genre ) {
				$per_genre[ $genre ] = ( $per_genre[ $genre ] ?? 0 ) + 1;
			}
		}

		arsort( $per_genre );

		return array(
			'totalGames'     => count( $ids ),
			'published'      => (int) $counts->publish,
			'drafts'         => (int) $counts->draft + (int) $counts->pending,
			'totalDownloads' => $downloads,
			'totalViews'     => $views,
			'averageRating'  => $rating_count ? round( $rating_sum / $rating_count, 1 ) : 0,
			'reviewCount'    => (int) get_comments( array( 'type' => 'review', 'status' => 'approve', 'count' => true ) ),
			'reportCount'    => post_type_exists( PSB_REPORT_POST_TYPE )
				? (int) wp_count_posts( PSB_REPORT_POST_TYPE )->publish
				: 0,
			'userCount'      => (int) count_users()['total_users'],
			'perGenre'       => $per_genre ? array_slice( $per_genre, 0, 12, true ) : new stdClass(),
		);
	}

	public static function users( $limit = 100 ) {
		$users = get_users( array( 'number' => max( 1, min( 200, (int) $limit ) ), 'orderby' => 'registered', 'order' => 'DESC' ) );
		$out   = array();
		foreach ( $users as $user ) {
			$favorites = get_user_meta( $user->ID, 'sl_favorites', true );
			$out[]     = array(
				'username'      => $user->user_login,
				'displayName'   => $user->display_name ? $user->display_name : $user->user_login,
				'role'          => PSB_Auth::is_admin( $user ) ? 'admin' : 'user',
				'roleLabel'     => implode( ', ', array_map( 'strval', (array) $user->roles ) ),
				'avatarUrl'     => PSB_Auth::avatar_url( $user->ID, 96 ),
				'joinedAt'      => mysql2date( 'Y-m-d', $user->user_registered ),
				'favoriteCount' => is_array( $favorites ) ? count( $favorites ) : 0,
				'badges'        => PSB_Auth::badges( $user->ID ),
			);
		}
		return $out;
	}
}
