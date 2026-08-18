<?php
/**
 * İstek listesi, puanlama, incelemeler, inceleme oyları, hata raporu ve sayaçlar.
 *
 * Hepsi temanın kullandığı anahtarların ÜZERİNDE çalışır; uygulamadan yapılan işlem sitede,
 * sitede yapılan işlem uygulamada görünür:
 *
 *   istek listesi  -> user meta  sl_favorites
 *   puan           -> post meta  sl_user_rating_sum / sl_user_rating_count / sl_user_rating_avg
 *   inceleme       -> wp_comments, comment_type = 'review' (+ sl_recommended, sl_playtime)
 *   inceleme oyu   -> comment meta sl_upvotes / sl_downvotes
 *   indirme sayacı -> post meta  game_download_count
 *   görüntülenme   -> post meta  game_view_count
 *   hata raporu    -> sl_report kayıt tipi + reported_game_id
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Social {

	/**
	 * Puan veren kullanıcılar: user_id => puan.
	 *
	 * Tema tarayıcı çerezine güveniyor; uygulamada çerez yok, bu yüzden oy sahibi kullanıcıya
	 * bağlanır. Böylece oy değiştirilebilir ve ortalama bozulmaz.
	 */
	const META_RATING_VOTERS = 'psb_rating_voters';
	const META_REVIEW_VOTERS = 'psb_review_voters';

	/* ---- İstek listesi ----------------------------------------------------------------------- */

	public static function favorites( $user_id ) {
		$list = get_user_meta( $user_id, 'sl_favorites', true );
		if ( ! is_array( $list ) ) {
			return array();
		}
		return array_values( array_unique( array_map( 'intval', $list ) ) );
	}

	public static function is_favorite( $user_id, $post_id ) {
		return in_array( (int) $post_id, self::favorites( $user_id ), true );
	}

	/** @return array{favorited:bool,count:int} */
	public static function toggle_favorite( $user_id, $post_id ) {
		$post_id = (int) $post_id;
		$list    = self::favorites( $user_id );

		if ( in_array( $post_id, $list, true ) ) {
			$list      = array_values( array_diff( $list, array( $post_id ) ) );
			$favorited = false;
		} else {
			$list[]    = $post_id;
			$favorited = true;
		}

		update_user_meta( $user_id, 'sl_favorites', $list );
		return array(
			'favorited' => $favorited,
			'count'     => count( $list ),
		);
	}

	/* ---- Puanlama ---------------------------------------------------------------------------- */

	public static function user_rating( $user_id, $post_id ) {
		$voters = get_post_meta( $post_id, self::META_RATING_VOTERS, true );
		if ( ! is_array( $voters ) ) {
			return 0;
		}
		return isset( $voters[ $user_id ] ) ? (int) $voters[ $user_id ] : 0;
	}

	/**
	 * Puan verir veya mevcut oyu değiştirir.
	 *
	 * Toplam/sayı/ortalama temanın anahtarlarında güncellenir; oy değişikliğinde eski puan
	 * toplamdan düşülür, sayaç artmaz.
	 *
	 * @return array|WP_Error
	 */
	public static function rate( $user_id, $post_id, $rating ) {
		$rating = (int) $rating;
		if ( $rating < 1 || $rating > 5 ) {
			return new WP_Error( 'psb_bad_rating', 'Puan 1 ile 5 arasında olmalı.', array( 'status' => 400 ) );
		}

		$post_id = (int) $post_id;
		$voters  = get_post_meta( $post_id, self::META_RATING_VOTERS, true );
		$voters  = is_array( $voters ) ? $voters : array();

		$sum   = (int) get_post_meta( $post_id, 'sl_user_rating_sum', true );
		$count = (int) get_post_meta( $post_id, 'sl_user_rating_count', true );

		if ( isset( $voters[ $user_id ] ) ) {
			$sum = $sum - (int) $voters[ $user_id ] + $rating;
		} else {
			$sum += $rating;
			++$count;
		}
		$voters[ $user_id ] = $rating;

		// Sayaç ile oy sahibi listesi ayrışmasın (sitede çerezle verilmiş anonim oylar da olabilir).
		$count = max( $count, count( $voters ) );
		$avg   = $count > 0 ? round( $sum / $count, 1 ) : 0;

		update_post_meta( $post_id, self::META_RATING_VOTERS, $voters );
		update_post_meta( $post_id, 'sl_user_rating_sum', $sum );
		update_post_meta( $post_id, 'sl_user_rating_count', $count );
		update_post_meta( $post_id, 'sl_user_rating_avg', $avg );

		return array(
			'rating'      => $avg,
			'ratingCount' => $count,
			'userRating'  => $rating,
		);
	}

	/* ---- İncelemeler ------------------------------------------------------------------------- */

	public static function has_reviewed( $user_id, $post_id ) {
		$existing = get_comments(
			array(
				'post_id' => (int) $post_id,
				'user_id' => (int) $user_id,
				'type'    => 'review',
				'count'   => true,
			)
		);
		return (int) $existing > 0;
	}

	public static function reviews( $post_id, $viewer_id = 0, $limit = 50 ) {
		$comments = get_comments(
			array(
				'post_id' => (int) $post_id,
				'type'    => 'review',
				'status'  => 'approve',
				'number'  => max( 1, min( 100, (int) $limit ) ),
				'orderby' => 'comment_date_gmt',
				'order'   => 'DESC',
			)
		);

		$out = array();
		foreach ( $comments as $comment ) {
			$voters = get_comment_meta( $comment->comment_ID, self::META_REVIEW_VOTERS, true );
			$voters = is_array( $voters ) ? $voters : array();

			$out[] = array(
				'id'          => (int) $comment->comment_ID,
				'author'      => $comment->comment_author,
				'authorId'    => (int) $comment->user_id,
				'avatarUrl'   => $comment->user_id
					? PSB_Auth::avatar_url( (int) $comment->user_id, 96 )
					: get_avatar_url( $comment->comment_author_email, array( 'size' => 96 ) ),
				'content'     => PSB_Mapper::plain_text( $comment->comment_content ),
				'recommended' => '1' === (string) get_comment_meta( $comment->comment_ID, 'sl_recommended', true ),
				'playtime'    => (string) ( get_comment_meta( $comment->comment_ID, 'sl_playtime', true ) ?: '' ),
				'date'        => mysql2date( 'Y-m-d', $comment->comment_date ),
				'upvotes'     => (int) get_comment_meta( $comment->comment_ID, 'sl_upvotes', true ),
				'downvotes'   => (int) get_comment_meta( $comment->comment_ID, 'sl_downvotes', true ),
				'voted'       => $viewer_id ? isset( $voters[ $viewer_id ] ) : false,
				'mine'        => $viewer_id && (int) $comment->user_id === (int) $viewer_id,
				'badges'      => $comment->user_id ? PSB_Auth::badges( (int) $comment->user_id ) : array(),
			);
		}
		return $out;
	}

	/**
	 * @return array|WP_Error
	 */
	public static function add_review( WP_User $user, $post_id, $content, $recommended ) {
		$content = trim( wp_strip_all_tags( (string) $content ) );
		if ( mb_strlen( $content ) < 3 ) {
			return new WP_Error( 'psb_short_review', 'İnceleme çok kısa.', array( 'status' => 400 ) );
		}
		if ( self::has_reviewed( $user->ID, $post_id ) ) {
			return new WP_Error( 'psb_already_reviewed', 'Bu oyun için zaten bir inceleme yazdın.', array( 'status' => 409 ) );
		}

		$comment_id = wp_insert_comment(
			array(
				'comment_post_ID'      => (int) $post_id,
				'comment_author'       => $user->display_name ? $user->display_name : $user->user_login,
				'comment_author_email' => $user->user_email,
				'comment_content'      => mb_substr( $content, 0, 5000 ),
				'comment_type'         => 'review',
				'user_id'              => $user->ID,
				'comment_approved'     => 1,
			)
		);

		if ( ! $comment_id ) {
			return new WP_Error( 'psb_review_failed', 'İnceleme kaydedilemedi.', array( 'status' => 500 ) );
		}

		add_comment_meta( $comment_id, 'sl_recommended', $recommended ? '1' : '0' );
		// Oynama süresi lisans sisteminden gelir; uygulama bu veriyi taşımıyor.
		add_comment_meta( $comment_id, 'sl_playtime', 'Süre kaydedilmedi' );

		return array( 'id' => (int) $comment_id );
	}

	/** Kullanıcı yalnızca kendi incelemesini silebilir; yönetici hepsini silebilir. */
	public static function delete_review( WP_User $user, $comment_id ) {
		$comment = get_comment( (int) $comment_id );
		if ( ! $comment || 'review' !== $comment->comment_type ) {
			return new WP_Error( 'psb_not_found', 'İnceleme bulunamadı.', array( 'status' => 404 ) );
		}
		if ( (int) $comment->user_id !== (int) $user->ID && ! PSB_Auth::is_admin( $user ) ) {
			return new WP_Error( 'psb_forbidden', 'Bu incelemeyi silme yetkin yok.', array( 'status' => 403 ) );
		}
		wp_delete_comment( (int) $comment_id, true );
		return true;
	}

	/**
	 * İnceleme oyu. Tema çerez kullanıyor; uygulamada oy kullanıcıya bağlanır ve tekrar oy
	 * verilmesi engellenir.
	 *
	 * @return array|WP_Error
	 */
	public static function vote_review( $user_id, $comment_id, $direction ) {
		$comment = get_comment( (int) $comment_id );
		if ( ! $comment || 'review' !== $comment->comment_type ) {
			return new WP_Error( 'psb_not_found', 'İnceleme bulunamadı.', array( 'status' => 404 ) );
		}
		if ( (int) $comment->user_id === (int) $user_id ) {
			return new WP_Error( 'psb_self_vote', 'Kendi incelemene oy veremezsin.', array( 'status' => 403 ) );
		}

		$voters = get_comment_meta( $comment_id, self::META_REVIEW_VOTERS, true );
		$voters = is_array( $voters ) ? $voters : array();
		if ( isset( $voters[ $user_id ] ) ) {
			return new WP_Error( 'psb_voted', 'Bu inceleme için zaten oy kullandın.', array( 'status' => 409 ) );
		}

		$key   = ( 'up' === $direction ) ? 'sl_upvotes' : 'sl_downvotes';
		$value = (int) get_comment_meta( $comment_id, $key, true ) + 1;
		update_comment_meta( $comment_id, $key, $value );

		$voters[ $user_id ] = ( 'up' === $direction ) ? 'up' : 'down';
		update_comment_meta( $comment_id, self::META_REVIEW_VOTERS, $voters );

		return array(
			'upvotes'   => (int) get_comment_meta( $comment_id, 'sl_upvotes', true ),
			'downvotes' => (int) get_comment_meta( $comment_id, 'sl_downvotes', true ),
		);
	}

	/* ---- Sayaçlar ---------------------------------------------------------------------------- */

	/** İndirme sayacını artırır ve gerçek indirme adresini döner. */
	public static function record_download( $post_id, $mirror = false ) {
		$post_id = (int) $post_id;
		$key     = $mirror ? 'game_download_mirror' : 'game_download_url';
		$url     = (string) get_post_meta( $post_id, $key, true );
		if ( '' === $url ) {
			$url = (string) get_post_meta( $post_id, 'game_download_url', true );
		}
		if ( '' === $url ) {
			return new WP_Error( 'psb_no_download', 'Bu kayıt için indirme bağlantısı tanımlı değil.', array( 'status' => 404 ) );
		}

		$count = (int) get_post_meta( $post_id, 'game_download_count', true ) + 1;
		update_post_meta( $post_id, 'game_download_count', $count );

		return array(
			'url'           => $url,
			'downloadCount' => $count,
			'password'      => (string) get_post_meta( $post_id, 'game_download_password', true ),
		);
	}

	/** Görüntülenme sayacı. Uygulama detay ekranını her açtığında bir kez çağırır. */
	public static function record_view( $post_id ) {
		$count = (int) get_post_meta( (int) $post_id, 'game_view_count', true ) + 1;
		update_post_meta( (int) $post_id, 'game_view_count', $count );
		return $count;
	}

	/* ---- Hata raporu ------------------------------------------------------------------------- */

	/**
	 * @return array|WP_Error
	 */
	public static function add_report( WP_User $user, $post_id, $message ) {
		if ( ! post_type_exists( PSB_REPORT_POST_TYPE ) ) {
			return new WP_Error( 'psb_no_reports', 'Rapor sistemi bu sitede etkin değil.', array( 'status' => 501 ) );
		}
		$message = trim( wp_strip_all_tags( (string) $message ) );
		if ( mb_strlen( $message ) < 5 ) {
			return new WP_Error( 'psb_short_report', 'Sorun açıklaması çok kısa.', array( 'status' => 400 ) );
		}

		$game_title = get_the_title( (int) $post_id );
		$author     = $user->display_name ? $user->display_name : $user->user_login;

		$report_id = wp_insert_post(
			array(
				'post_title'   => sprintf( '[%s] Hata Raporu - %s', $game_title, $author ),
				'post_content' => mb_substr( $message, 0, 5000 ),
				'post_type'    => PSB_REPORT_POST_TYPE,
				'post_status'  => 'publish',
			),
			true
		);

		if ( is_wp_error( $report_id ) ) {
			return $report_id;
		}
		update_post_meta( $report_id, 'reported_game_id', (int) $post_id );
		update_post_meta( $report_id, 'psb_reporter_id', (int) $user->ID );

		return array( 'id' => (int) $report_id );
	}

	/* ---- Kullanıcının kendi verisi ------------------------------------------------------------ */

	/** Profil ekranı: istek listesi kartları. */
	public static function favorite_games( $user_id ) {
		$ids = self::favorites( $user_id );
		if ( empty( $ids ) ) {
			return array();
		}
		$posts = get_posts(
			array(
				'post_type'      => PSB_POST_TYPE,
				'post__in'       => $ids,
				'posts_per_page' => count( $ids ),
				'orderby'        => 'post__in',
				'post_status'    => 'publish',
			)
		);
		$out = array();
		foreach ( $posts as $post ) {
			$out[] = PSB_Mapper::summary( $post, $user_id );
		}
		return $out;
	}
}
