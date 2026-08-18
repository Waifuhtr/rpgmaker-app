<?php
/**
 * Oyun Detay Sayfası Şablonu (Akıllı Link Yönlendirmeli, Breadcrumb ve
 * Özelleştiriciden seçilebilen 4 farklı tam sayfa tasarımı ile)
 */
if ( ! defined( 'ABSPATH' ) ) exit;
get_header();

while ( have_posts() ) : the_post();
	// ============================================================
	// PAYLAŞILAN VERİ (Tüm tasarımlar bu verileri kullanır)
	// ============================================================
	$post_id = get_the_ID();
	$bg_image = get_post_meta( $post_id, 'game_background_image', true ) ?: get_the_post_thumbnail_url( $post_id, 'full' );
	$archive_link = get_post_type_archive_link('game');

	$get_terms_with_icon = function( $taxonomy ) use ( $post_id, $archive_link ) {
		$terms = get_the_terms( $post_id, $taxonomy );
		if ( is_wp_error( $terms ) || empty( $terms ) ) return '-';
		$output = array();
		foreach ( $terms as $term ) {
			$icon_meta = get_term_meta( $term->term_id, 'sl_term_icon', true );
			$icon_html = '';
			if ( $icon_meta ) {
				if ( strpos( $icon_meta, 'http' ) === 0 ) {
					$icon_html = '<img src="' . esc_url( $icon_meta ) . '" class="sl-term-icon" alt="' . esc_attr( $term->name ) . '" loading="lazy" decoding="async">';
				} else {
					$icon_html = '<span class="sl-term-emoji">' . esc_html( $icon_meta ) . '</span>';
				}
			}
			$smart_link = add_query_arg( array( $taxonomy => $term->slug ), $archive_link );
			$output[] = '<a href="' . esc_url( $smart_link ) . '" class="sl-term-link">' . $icon_html . esc_html( $term->name ) . '</a>';
		}
		return implode( ', ', $output );
	};

	$get_first_term_name = function( $taxonomy ) use ( $post_id ) {
		$terms = get_the_terms( $post_id, $taxonomy );
		return ( ! empty( $terms ) && ! is_wp_error( $terms ) ) ? $terms[0]->name : '';
	};

	$download_url = get_post_meta( $post_id, 'game_download_url', true );
	$mirror_url   = get_post_meta( $post_id, 'game_download_mirror', true );
	$version      = get_post_meta( $post_id, 'game_version', true );
	$size         = get_post_meta( $post_id, 'game_size', true ) ?: 'Bilinmiyor';
	$download_count = (int) get_post_meta( $post_id, 'game_download_count', true );
	$changelog    = get_post_meta( $post_id, 'game_changelog', true );
	$post_tags    = get_the_tags( $post_id );
	$trailer_url  = get_post_meta( $post_id, 'game_trailer_url', true );
	$genres       = get_the_terms( $post_id, 'game_genre' );
	$platform_first = $get_first_term_name( 'game_platform' );
	$language_first = $get_first_term_name( 'game_language' );

	$avg = get_post_meta( $post_id, 'sl_user_rating_avg', true ) ?: 0;
	$count = get_post_meta( $post_id, 'sl_user_rating_count', true ) ?: 0;
	$pct = min( 100, max( 0, ( (float) $avg / 5 ) * 100 ) );

	$is_logged_in = is_user_logged_in() ? 'true' : 'false';
	$is_favorited = false;
	if ( is_user_logged_in() ) {
		$user_favs = get_user_meta( get_current_user_id(), 'sl_favorites', true );
		if ( is_array( $user_favs ) && in_array( $post_id, $user_favs ) ) { $is_favorited = true; }
	}
	$btn_class = $is_favorited ? 'sl-favorite-btn active' : 'sl-favorite-btn';
	$btn_text  = $is_favorited ? __( 'İstek Listesinden Çıkar', 'steamlike' ) : __( 'İstek Listesine Ekle', 'steamlike' );

	$align_class = get_theme_mod( 'sl_meta_alignment', 'left' );
	$sg_style = get_theme_mod( 'sl_single_game_style', 'default' );

	// Kapak görseli HTML'ini tek yerden üretelim (her tasarımda tekrar kullanılacak)
	$cover_html = has_post_thumbnail()
		? get_the_post_thumbnail( $post_id, 'steamlike-cover', array( 'loading' => 'eager', 'fetchpriority' => 'high', 'alt' => get_the_title() ) )
		: '<img src="' . esc_url( STEAMLIKE_URI . 'assets/images/placeholder-cover.jpg' ) . '" alt="' . esc_attr( get_the_title() ) . '" loading="eager">';
	?>

	<main id="primary" class="site-main sl-single-game sl-sg-style-<?php echo esc_attr( $sg_style ); ?>">

	<?php if ( 'playstore' === $sg_style ) :
		// ============================================================
		// TASARIM: PLAY STORE TARZI
		// ============================================================
		?>
		<div class="sl-sg-ps-topbar"><div class="container"><?php sl_breadcrumb(); ?></div></div>

		<div class="container sl-sg-ps-container">

			<div class="sl-sg-ps-header">
				<div class="sl-sg-ps-icon sl-hud-frame">
					<span class="sl-hud-corner sl-hud-tr"></span><span class="sl-hud-corner sl-hud-bl"></span>
					<?php echo $cover_html; ?>
				</div>
				<div class="sl-sg-ps-heading">
					<h1 class="sl-sg-ps-title"><?php the_title(); ?></h1>
					<div class="sl-sg-ps-dev"><?php echo wp_kses_post( $get_terms_with_icon( 'game_developer' ) ); ?></div>
					<div class="sl-sg-ps-genres">
						<?php if ( $genres && ! is_wp_error( $genres ) ) : foreach ( $genres as $genre ) :
							$smart_link = add_query_arg( array( 'game_genre' => $genre->slug ), $archive_link ); ?>
							<a href="<?php echo esc_url( $smart_link ); ?>" class="sl-sg-ps-genre-chip"><?php echo esc_html( $genre->name ); ?></a>
						<?php endforeach; endif; ?>
					</div>
				</div>
				<div class="sl-sg-ps-cta">
					<?php if ( $download_url ) : ?>
						<a href="<?php echo esc_url( home_url( '/indir/?game_id=' . $post_id ) ); ?>" target="_blank" class="sl-btn sl-btn-primary sl-sg-ps-install-btn sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-download"></span> <?php esc_html_e( 'Yükle', 'steamlike' ); ?></a>
					<?php endif; ?>
					<a href="#" class="<?php echo esc_attr( $btn_class ); ?> sl-sg-ps-fav-icon" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-logged-in="<?php echo esc_attr( $is_logged_in ); ?>" aria-label="<?php echo esc_attr( $btn_text ); ?>"><span class="dashicons dashicons-heart"></span><span class="sl-fav-text sl-sr-only"><?php echo esc_html( $btn_text ); ?></span></a>
				</div>
			</div>

			<div class="sl-sg-ps-stats">
				<div class="sl-sg-ps-stat">
					<strong><?php echo esc_html( $avg ); ?></strong>
					<div class="sl-rating-stars" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-avg="<?php echo esc_attr( $avg ); ?>">
						<?php for ( $i = 1; $i <= 5; $i++ ) { $star_class = ( $i <= round( $avg ) ) ? 'dashicons-star-filled' : 'dashicons-star-empty'; echo '<span class="dashicons ' . $star_class . ' sl-star sl-sg-ps-star" data-rating="' . $i . '"></span>'; } ?>
					</div>
					<span><?php echo esc_html( $count ); ?> <?php esc_html_e( 'Oy', 'steamlike' ); ?></span>
					<span class="sl-rating-msg"></span>
				</div>
				<div class="sl-sg-ps-stat-divider"></div>
				<div class="sl-sg-ps-stat">
					<strong><span class="sl-download-count-val"><?php echo esc_html( $download_count ); ?></span>+</strong>
					<span><?php esc_html_e( 'İndirme', 'steamlike' ); ?></span>
				</div>
				<div class="sl-sg-ps-stat-divider"></div>
				<div class="sl-sg-ps-stat">
					<strong><span class="dashicons dashicons-desktop"></span></strong>
					<span><?php echo esc_html( $platform_first ?: '—' ); ?></span>
				</div>
			</div>

			<?php get_template_part( 'template-parts/single/screenshots' ); ?>

			<div class="sl-content-box sl-sg-ps-about">
				<h3><?php esc_html_e( 'Bu Oyun Hakkında', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php the_content(); ?></div>
			</div>

			<?php if ( ! empty( $changelog ) ) : ?>
			<div class="sl-content-box sl-changelog-box">
				<h3><span class="dashicons dashicons-welcome-write-blog"></span> <?php esc_html_e( 'Değişiklik Günlüğü / Notlar', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php echo wpautop( esc_html( $changelog ) ); ?></div>
			</div>
			<?php endif; ?>

			<div class="sl-content-box sl-sg-ps-infogrid">
				<h3><?php esc_html_e( 'Uygulama Bilgileri', 'steamlike' ); ?></h3>
				<div class="sl-sg-ps-info-rows">
					<div class="sl-sg-ps-info-row"><span><?php esc_html_e( 'Güncel Sürüm', 'steamlike' ); ?></span><strong class="sl-mono"><?php echo esc_html( $version ?: 'v1.0' ); ?></strong></div>
					<div class="sl-sg-ps-info-row"><span><?php esc_html_e( 'Boyut', 'steamlike' ); ?></span><strong class="sl-mono"><?php echo esc_html( $size ); ?></strong></div>
					<div class="sl-sg-ps-info-row"><span><?php esc_html_e( 'Platform', 'steamlike' ); ?></span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_platform' ) ); ?></strong></div>
					<div class="sl-sg-ps-info-row"><span><?php esc_html_e( 'Dil', 'steamlike' ); ?></span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_language' ) ); ?></strong></div>
					<div class="sl-sg-ps-info-row"><span><?php esc_html_e( 'Geliştirici', 'steamlike' ); ?></span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_developer' ) ); ?></strong></div>
				</div>
				<?php if ( $mirror_url ) : ?><a href="<?php echo esc_url( $mirror_url ); ?>" target="_blank" class="sl-btn sl-btn-secondary sl-mt-15 sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-external"></span> <?php esc_html_e( 'Alternatif Link', 'steamlike' ); ?></a><?php endif; ?>
			</div>

			<?php
			if ( $post_tags ) {
				echo '<div class="sl-tags-box"><h3>' . esc_html__( 'Etiketler', 'steamlike' ) . '</h3><div class="sl-tag-cloud">';
				foreach ( $post_tags as $tag ) {
					$smart_link = add_query_arg( array( 'post_tag' => $tag->slug ), $archive_link );
					echo '<a href="' . esc_url( $smart_link ) . '" class="sl-animated-tag">' . esc_html( $tag->name ) . '</a>';
				}
				echo '</div></div>';
			}
			if ( $trailer_url ) : ?>
				<div class="sl-content-box sl-media-box"><h3><?php esc_html_e( 'Oyun Fragmanı', 'steamlike' ); ?></h3><div class="sl-video-wrapper"><?php $embed_code = wp_oembed_get( $trailer_url, array( 'width' => 800 ) ); echo $embed_code ? $embed_code : '<a href="' . esc_url( $trailer_url ) . '" target="_blank" class="sl-btn sl-btn-secondary"><span class="dashicons dashicons-video-alt3"></span> ' . esc_html__( 'Fragmanı İzle', 'steamlike' ) . '</a>'; ?></div></div>
			<?php endif;

			get_template_part( 'template-parts/single/requirements' );
			?>

			<div class="sl-content-box sl-sg-ps-links-row">
				<button type="button" id="sl-open-report-modal" class="sl-btn sl-btn-secondary sl-report-btn"><span class="dashicons dashicons-warning"></span> <?php esc_html_e( 'Sorun / Hata Bildir', 'steamlike' ); ?></button>
				<a href="<?php echo esc_url( get_permalink() . 'inceleme/' ); ?>" class="sl-btn sl-btn-secondary sl-reviews-link-btn"><span class="dashicons dashicons-testimonial"></span> <?php esc_html_e( 'İncelemeleri Gör / Yaz', 'steamlike' ); ?></a>
			</div>

			<?php get_template_part( 'template-parts/single/related' ); ?>
		</div>

	<?php elseif ( 'itchio' === $sg_style ) :
		// ============================================================
		// TASARIM: ITCH.IO TARZI
		// ============================================================
		?>
		<div class="sl-sg-itch-banner" style="background-image:url('<?php echo esc_url( $bg_image ); ?>');"><div class="sl-sg-itch-banner-overlay"></div></div>

		<div class="container sl-sg-itch-container">
			<?php sl_breadcrumb(); ?>

			<h1 class="sl-sg-itch-title"><?php the_title(); ?></h1>
			<div class="sl-sg-itch-byline"><?php esc_html_e( 'Geliştirici:', 'steamlike' ); ?> <?php echo wp_kses_post( $get_terms_with_icon( 'game_developer' ) ); ?></div>

			<div class="sl-sg-itch-layout">
				<div class="sl-sg-itch-main">

					<div class="sl-content-box">
						<h3><?php esc_html_e( 'Açıklama', 'steamlike' ); ?></h3>
						<div class="sl-entry-content"><?php the_content(); ?></div>
					</div>

					<?php if ( ! empty( $changelog ) ) : ?>
					<div class="sl-content-box sl-changelog-box">
						<h3><span class="dashicons dashicons-welcome-write-blog"></span> <?php esc_html_e( 'Değişiklik Günlüğü / Notlar', 'steamlike' ); ?></h3>
						<div class="sl-entry-content"><?php echo wpautop( esc_html( $changelog ) ); ?></div>
					</div>
					<?php endif; ?>

					<?php get_template_part( 'template-parts/single/screenshots' ); ?>

					<?php if ( $trailer_url ) : ?>
						<div class="sl-content-box sl-media-box"><h3><?php esc_html_e( 'Oyun Fragmanı', 'steamlike' ); ?></h3><div class="sl-video-wrapper"><?php $embed_code = wp_oembed_get( $trailer_url, array( 'width' => 800 ) ); echo $embed_code ? $embed_code : '<a href="' . esc_url( $trailer_url ) . '" target="_blank" class="sl-btn sl-btn-secondary"><span class="dashicons dashicons-video-alt3"></span> ' . esc_html__( 'Fragmanı İzle', 'steamlike' ) . '</a>'; ?></div></div>
					<?php endif; ?>

					<?php
					if ( $post_tags ) {
						echo '<div class="sl-tags-box"><h3>' . esc_html__( 'Etiketler', 'steamlike' ) . '</h3><div class="sl-tag-cloud">';
						foreach ( $post_tags as $tag ) {
							$smart_link = add_query_arg( array( 'post_tag' => $tag->slug ), $archive_link );
							echo '<a href="' . esc_url( $smart_link ) . '" class="sl-animated-tag">' . esc_html( $tag->name ) . '</a>';
						}
						echo '</div></div>';
					}
					get_template_part( 'template-parts/single/requirements' );
					?>
				</div>

				<aside class="sl-sg-itch-sidebar">
					<div class="sl-sg-itch-cover sl-hud-frame">
						<span class="sl-hud-corner sl-hud-tr"></span><span class="sl-hud-corner sl-hud-bl"></span>
						<?php echo $cover_html; ?>
					</div>

					<?php if ( $download_url ) : ?>
						<a href="<?php echo esc_url( home_url( '/indir/?game_id=' . $post_id ) ); ?>" target="_blank" class="sl-btn sl-btn-primary sl-btn-block sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-download"></span> <?php esc_html_e( 'İndir', 'steamlike' ); ?></a>
					<?php endif; ?>
					<?php if ( $mirror_url ) : ?>
						<a href="<?php echo esc_url( $mirror_url ); ?>" target="_blank" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-external"></span> <?php esc_html_e( 'Alternatif Link', 'steamlike' ); ?></a>
					<?php endif; ?>

					<a href="#" class="<?php echo esc_attr( $btn_class ); ?> sl-btn-block sl-mt-10" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-logged-in="<?php echo esc_attr( $is_logged_in ); ?>"><span class="dashicons dashicons-heart"></span> <span class="sl-fav-text"><?php echo esc_html( $btn_text ); ?></span></a>

					<div class="sl-sg-itch-info-list">
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Durum', 'steamlike' ); ?></span><strong><?php echo esc_html( $version ? sprintf( __( 'Yayında · %s', 'steamlike' ), $version ) : __( 'Yayında', 'steamlike' ) ); ?></strong></div>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Platformlar', 'steamlike' ); ?></span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_platform' ) ); ?></strong></div>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Dil', 'steamlike' ); ?></span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_language' ) ); ?></strong></div>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Boyut', 'steamlike' ); ?></span><strong class="sl-mono"><?php echo esc_html( $size ); ?></strong></div>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'İndirme', 'steamlike' ); ?></span><strong class="sl-mono"><span class="sl-download-count-val"><?php echo esc_html( $download_count ); ?></span>+</strong></div>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Puan', 'steamlike' ); ?></span><strong><span class="sl-text-gold"><?php echo esc_html( $avg ); ?> / 5</span></strong></div>
						<?php if ( $genres && ! is_wp_error( $genres ) ) : ?>
						<div class="sl-sg-itch-info-row"><span><?php esc_html_e( 'Tür', 'steamlike' ); ?></span><strong>
							<?php foreach ( $genres as $genre ) : $smart_link = add_query_arg( array( 'game_genre' => $genre->slug ), $archive_link ); ?>
								<a href="<?php echo esc_url( $smart_link ); ?>" class="sl-term-link"><?php echo esc_html( $genre->name ); ?></a><?php echo ( $genre !== end( $genres ) ) ? ', ' : ''; ?>
							<?php endforeach; ?>
						</strong></div>
						<?php endif; ?>
					</div>

					<div class="sl-sg-itch-rating-box">
						<div class="sl-rating-stars" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-avg="<?php echo esc_attr( $avg ); ?>">
							<?php for ( $i = 1; $i <= 5; $i++ ) { $star_class = ( $i <= round( $avg ) ) ? 'dashicons-star-filled' : 'dashicons-star-empty'; echo '<span class="dashicons ' . $star_class . ' sl-star" data-rating="' . $i . '"></span>'; } ?>
						</div>
						<span class="sl-rating-msg"></span>
					</div>

					<button type="button" id="sl-open-report-modal" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-report-btn"><span class="dashicons dashicons-warning"></span> <?php esc_html_e( 'Sorun Bildir', 'steamlike' ); ?></button>
					<a href="<?php echo esc_url( get_permalink() . 'inceleme/' ); ?>" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-reviews-link-btn"><span class="dashicons dashicons-testimonial"></span> <?php esc_html_e( 'İncelemeler', 'steamlike' ); ?></a>
				</aside>
			</div>

			<?php get_template_part( 'template-parts/single/related' ); ?>
		</div>

	<?php elseif ( 'holo' === $sg_style ) :
		// ============================================================
		// TASARIM: HOLO-TERMİNAL (İMZA TASARIM)
		// ============================================================
		?>
		<div class="sl-sg-holo-hero">
			<div class="sl-sg-holo-cover sl-hud-frame">
				<span class="sl-hud-corner sl-hud-tr"></span><span class="sl-hud-corner sl-hud-bl"></span>
				<?php echo $cover_html; ?>
				<div class="sl-sg-holo-scan"></div>
			</div>
			<div class="sl-sg-holo-panel">
				<?php sl_breadcrumb(); ?>
				<span class="sl-eyebrow"><?php esc_html_e( 'SİSTEM KAYDI', 'steamlike' ); ?> #<?php echo esc_html( $post_id ); ?></span>
				<h1 class="sl-sg-holo-title"><?php the_title(); ?></h1>

				<div class="sl-sg-holo-readout">
					<div class="sl-sg-holo-row"><span>DEVELOPER</span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_developer' ) ); ?></strong></div>
					<div class="sl-sg-holo-row"><span>VERSION</span><strong class="sl-mono"><?php echo esc_html( $version ?: 'v1.0' ); ?></strong></div>
					<div class="sl-sg-holo-row"><span>SIZE</span><strong class="sl-mono"><?php echo esc_html( $size ); ?></strong></div>
					<div class="sl-sg-holo-row"><span>PLATFORM</span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_platform' ) ); ?></strong></div>
					<div class="sl-sg-holo-row"><span>LANGUAGE</span><strong><?php echo wp_kses_post( $get_terms_with_icon( 'game_language' ) ); ?></strong></div>
					<div class="sl-sg-holo-row"><span>DOWNLOADS</span><strong class="sl-mono"><span class="sl-download-count-val"><?php echo esc_html( $download_count ); ?></span>+</strong></div>
				</div>

				<div class="sl-sg-holo-rating">
					<div class="sl-rating-box" style="--sl-pct: <?php echo esc_attr( $pct ); ?>">
						<div class="sl-rating-stars" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-avg="<?php echo esc_attr( $avg ); ?>">
							<?php for ( $i = 1; $i <= 5; $i++ ) { $star_class = ( $i <= round( $avg ) ) ? 'dashicons-star-filled' : 'dashicons-star-empty'; echo '<span class="dashicons ' . $star_class . ' sl-star" data-rating="' . $i . '"></span>'; } ?>
						</div>
						<div class="sl-rating-info"><span class="sl-rating-avg-text"><?php echo esc_html( $avg ); ?></span> / 5 (<?php echo esc_html( $count ); ?> <?php esc_html_e( 'Oy', 'steamlike' ); ?>)<br><span class="sl-rating-msg"></span></div>
					</div>
				</div>

				<div class="sl-sg-holo-cta">
					<?php if ( $download_url ) : ?>
						<a href="<?php echo esc_url( home_url( '/indir/?game_id=' . $post_id ) ); ?>" target="_blank" class="sl-btn sl-btn-primary sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-download"></span> <?php esc_html_e( 'İNDİRMEYİ BAŞLAT', 'steamlike' ); ?></a>
					<?php endif; ?>
					<?php if ( $mirror_url ) : ?>
						<a href="<?php echo esc_url( $mirror_url ); ?>" target="_blank" class="sl-btn sl-btn-secondary sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-external"></span> <?php esc_html_e( 'ALT. LİNK', 'steamlike' ); ?></a>
					<?php endif; ?>
					<a href="#" class="<?php echo esc_attr( $btn_class ); ?>" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-logged-in="<?php echo esc_attr( $is_logged_in ); ?>"><span class="dashicons dashicons-heart"></span> <span class="sl-fav-text"><?php echo esc_html( $btn_text ); ?></span></a>
					<button type="button" id="sl-open-report-modal" class="sl-btn sl-report-btn"><span class="dashicons dashicons-warning"></span> <span class="sl-btn-label"><?php esc_html_e( 'HATA BİLDİR', 'steamlike' ); ?></span></button>
				</div>
			</div>
		</div>

		<div class="container sl-sg-holo-container">
			<?php if ( $genres && ! is_wp_error( $genres ) ) : ?>
			<div class="sl-genre-row sl-mb-30">
				<?php foreach ( $genres as $genre ) : $smart_link = add_query_arg( array( 'game_genre' => $genre->slug ), $archive_link ); ?>
					<a href="<?php echo esc_url( $smart_link ); ?>" class="sl-neon-tag"><?php echo esc_html( $genre->name ); ?></a>
				<?php endforeach; ?>
			</div>
			<?php endif; ?>

			<div class="sl-content-box sl-sg-holo-terminal">
				<h3><span class="sl-mono sl-text-signal">[ ABOUT ]</span> <?php esc_html_e( 'Oyun Hakkında', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php the_content(); ?></div>
			</div>

			<?php if ( ! empty( $changelog ) ) : ?>
			<div class="sl-content-box sl-changelog-box sl-sg-holo-terminal">
				<h3><span class="sl-mono sl-text-success">[ CHANGELOG ]</span> <?php esc_html_e( 'Değişiklik Günlüğü', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php echo wpautop( esc_html( $changelog ) ); ?></div>
			</div>
			<?php endif; ?>

			<?php get_template_part( 'template-parts/single/screenshots' ); ?>

			<?php
			if ( $post_tags ) {
				echo '<div class="sl-tags-box sl-sg-holo-terminal"><h3><span class="sl-mono sl-text-signal">[ TAGS ]</span> ' . esc_html__( 'Etiketler', 'steamlike' ) . '</h3><div class="sl-tag-cloud">';
				foreach ( $post_tags as $tag ) {
					$smart_link = add_query_arg( array( 'post_tag' => $tag->slug ), $archive_link );
					echo '<a href="' . esc_url( $smart_link ) . '" class="sl-animated-tag">' . esc_html( $tag->name ) . '</a>';
				}
				echo '</div></div>';
			}
			if ( $trailer_url ) : ?>
				<div class="sl-content-box sl-media-box sl-sg-holo-terminal"><h3><span class="sl-mono sl-text-signal">[ TRAILER ]</span> <?php esc_html_e( 'Oyun Fragmanı', 'steamlike' ); ?></h3><div class="sl-video-wrapper"><?php $embed_code = wp_oembed_get( $trailer_url, array( 'width' => 800 ) ); echo $embed_code ? $embed_code : '<a href="' . esc_url( $trailer_url ) . '" target="_blank" class="sl-btn sl-btn-secondary"><span class="dashicons dashicons-video-alt3"></span> ' . esc_html__( 'Fragmanı İzle', 'steamlike' ) . '</a>'; ?></div></div>
			<?php endif;

			get_template_part( 'template-parts/single/requirements' );
			?>

			<a href="<?php echo esc_url( get_permalink() . 'inceleme/' ); ?>" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-mb-30 sl-reviews-link-btn"><span class="dashicons dashicons-testimonial"></span> <?php esc_html_e( 'İncelemeleri Gör / Yaz', 'steamlike' ); ?></a>

			<?php get_template_part( 'template-parts/single/related' ); ?>
		</div>

	<?php else :
		// ============================================================
		// TASARIM: VARSAYILAN (APPYN LAYOUT)
		// ============================================================
		?>
		<div class="sl-single-bg" style="background-image: url('<?php echo esc_url( $bg_image ); ?>');"><div class="sl-single-bg-overlay"></div></div>

		<div class="container sl-appyn-container">

			<?php sl_breadcrumb(); ?>

			<div class="sl-appyn-box">
				<div class="sl-appyn-left">
					<div class="sl-appyn-cover sl-hud-frame">
						<span class="sl-hud-corner sl-hud-tr"></span>
						<span class="sl-hud-corner sl-hud-bl"></span>
						<?php echo $cover_html; ?>
					</div>
					<?php if ( $download_url ) : ?>
						<a href="<?php echo esc_url( home_url( '/indir/?game_id=' . $post_id ) ); ?>" target="_blank" class="sl-btn sl-btn-primary sl-btn-block sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-download"></span> <?php esc_html_e( 'İndir (APK / PC)', 'steamlike' ); ?></a>
					<?php endif; ?>
					<?php if ( $mirror_url ) : ?>
						<a href="<?php echo esc_url( $mirror_url ); ?>" target="_blank" class="sl-btn sl-btn-secondary sl-btn-block sl-track-download sl-mt-10" data-post-id="<?php echo esc_attr( $post_id ); ?>"><span class="dashicons dashicons-external"></span> <?php esc_html_e( 'Alternatif Link', 'steamlike' ); ?></a>
					<?php endif; ?>
					<a href="<?php echo esc_url( get_permalink() . 'inceleme/' ); ?>" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-reviews-link-btn"><span class="dashicons dashicons-testimonial"></span> <?php esc_html_e( 'İncelemeleri Gör / Yaz', 'steamlike' ); ?></a>
					<button type="button" id="sl-open-report-modal" class="sl-btn sl-btn-block sl-mt-10 sl-report-btn"><span class="dashicons dashicons-warning"></span> <?php esc_html_e( 'Sorun / Hata Bildir', 'steamlike' ); ?></button>

					<div class="sl-rating-box" style="--sl-pct: <?php echo esc_attr( $pct ); ?>">
						<div class="sl-rating-stars" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-avg="<?php echo esc_attr( $avg ); ?>">
							<?php for ( $i = 1; $i <= 5; $i++ ) {
								$star_class = ( $i <= round( $avg ) ) ? 'dashicons-star-filled' : 'dashicons-star-empty';
								echo '<span class="dashicons ' . $star_class . ' sl-star" data-rating="' . $i . '"></span>';
							} ?>
						</div>
						<div class="sl-rating-info"><span class="sl-rating-avg-text"><?php echo esc_html( $avg ); ?></span> / 5 (<?php echo esc_html( $count ); ?> Oy)<br><span class="sl-rating-msg"></span></div>
					</div>
					<a href="#" class="<?php echo esc_attr( $btn_class ); ?> sl-btn-block" data-post-id="<?php echo esc_attr( $post_id ); ?>" data-logged-in="<?php echo esc_attr( $is_logged_in ); ?>"><span class="dashicons dashicons-heart"></span> <span class="sl-fav-text"><?php echo esc_html( $btn_text ); ?></span></a>
				</div>
				<div class="sl-appyn-right">
					<h1 class="sl-game-title"><?php the_title(); ?></h1>
					<ul class="sl-meta-list sl-meta-align-<?php echo esc_attr( $align_class ); ?>">
						<li class="sl-meta-item"><span class="sl-meta-label">Geliştirici</span><span class="sl-meta-value"><?php echo wp_kses_post( $get_terms_with_icon('game_developer') ); ?></span></li>
						<li class="sl-meta-item"><span class="sl-meta-label">Sürüm</span><span class="sl-meta-value"><?php echo esc_html( $version ?: 'Cihaza göre değişir' ); ?></span></li>
						<li class="sl-meta-item"><span class="sl-meta-label">Boyut</span><span class="sl-meta-value"><?php echo esc_html( $size ); ?></span></li>
						<li class="sl-meta-item"><span class="sl-meta-label">Platform</span><span class="sl-meta-value"><?php echo wp_kses_post( $get_terms_with_icon('game_platform') ); ?></span></li>
						<li class="sl-meta-item"><span class="sl-meta-label">Dil</span><span class="sl-meta-value"><?php echo wp_kses_post( $get_terms_with_icon('game_language') ); ?></span></li>
						<li class="sl-meta-item"><span class="sl-meta-label">İndirilme</span><span class="sl-meta-value"><span class="sl-download-count-val"><?php echo esc_html( $download_count ); ?></span>+ Kez</span></li>
					</ul>
					<div class="sl-genre-row">
						<?php if ( $genres && ! is_wp_error( $genres ) ) : foreach ( $genres as $genre ) :
							$smart_link = add_query_arg( array( 'game_genre' => $genre->slug ), $archive_link ); ?>
							<a href="<?php echo esc_url( $smart_link ); ?>" class="sl-neon-tag"><?php echo esc_html( $genre->name ); ?></a>
						<?php endforeach; endif; ?>
					</div>
				</div>
			</div>

			<div class="sl-content-box">
				<h3><?php esc_html_e( 'Oyun Hakkında', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php the_content(); ?></div>
			</div>

			<?php if ( ! empty( $changelog ) ) : ?>
			<div class="sl-content-box sl-changelog-box">
				<h3><span class="dashicons dashicons-welcome-write-blog"></span> <?php esc_html_e( 'Değişiklik Günlüğü / Notlar', 'steamlike' ); ?></h3>
				<div class="sl-entry-content"><?php echo wpautop( esc_html( $changelog ) ); ?></div>
			</div>
			<?php endif; ?>

			<?php
			get_template_part( 'template-parts/single/screenshots' );

			if ( $post_tags ) {
				echo '<div class="sl-tags-box"><h3>' . esc_html__( 'Etiketler', 'steamlike' ) . '</h3><div class="sl-tag-cloud">';
				foreach ( $post_tags as $tag ) {
					$smart_link = add_query_arg( array( 'post_tag' => $tag->slug ), $archive_link );
					echo '<a href="' . esc_url( $smart_link ) . '" class="sl-animated-tag">' . esc_html( $tag->name ) . '</a>';
				}
				echo '</div></div>';
			}

			if ( $trailer_url ) : ?>
				<div class="sl-content-box sl-media-box"><h3><?php esc_html_e( 'Oyun Fragmanı', 'steamlike' ); ?></h3><div class="sl-video-wrapper"><?php $embed_code = wp_oembed_get( $trailer_url, array( 'width' => 800 ) ); if ( $embed_code ) { echo $embed_code; } else { echo '<a href="' . esc_url( $trailer_url ) . '" target="_blank" class="sl-btn sl-btn-secondary"><span class="dashicons dashicons-video-alt3"></span> ' . esc_html__( 'Fragmanı İzle', 'steamlike' ) . '</a>'; } ?></div></div>
			<?php endif;
			
			get_template_part( 'template-parts/single/requirements' );
			get_template_part( 'template-parts/single/related' );
			?>
		</div>
	<?php endif; ?>

	<div class="sl-auth-modal-wrapper" id="sl-report-modal">
		<div class="sl-auth-modal-bg" id="sl-close-report-modal"></div>
		<div class="sl-auth-modal-content sl-content-box sl-modal-danger">
			<span class="sl-auth-modal-close" id="sl-close-report-btn">&times;</span>
			<h3 class="sl-auth-modal-title sl-text-danger-title"><span class="dashicons dashicons-warning"></span> <?php esc_html_e( 'Hata Bildir', 'steamlike' ); ?></h3>
			<p class="sl-auth-modal-sub"><?php esc_html_e( 'Oyunla ilgili indirme, kurulum veya oynanış sorunu yaşıyorsanız bize bildirin.', 'steamlike' ); ?></p>
			
			<form method="post" enctype="multipart/form-data" class="sl-form sl-mt-20">
				<input type="hidden" name="report_game_id" value="<?php echo esc_attr( $post_id ); ?>">
				<label><?php esc_html_e( 'Sorunu Detaylıca Anlatın', 'steamlike' ); ?></label>
				<textarea name="report_message" rows="4" required></textarea>
				<label><?php esc_html_e( 'Ekran Görüntüsü (İsteğe Bağlı)', 'steamlike' ); ?></label>
				<input type="file" name="report_image" accept="image/*">
				<button type="submit" name="sl_submit_report" class="sl-btn sl-btn-block sl-btn-danger-solid"><?php esc_html_e( 'Raporu Gönder', 'steamlike' ); ?></button>
			</form>
		</div>
	</div>

	<?php if ( isset($_GET['rapor']) && $_GET['rapor'] === 'basarili' ) : ?>
		<div class="sl-toast" id="sl-report-toast"><span class="dashicons dashicons-yes-alt"></span> <?php esc_html_e( 'Raporunuz başarıyla yönetime iletildi. Teşekkür ederiz!', 'steamlike' ); ?></div>
		<script>
			document.addEventListener('DOMContentLoaded', function() {
				var toast = document.getElementById('sl-report-toast');
				if (toast) {
					requestAnimationFrame(function() { toast.classList.add('active'); });
					setTimeout(function() { toast.classList.remove('active'); }, 5000);
				}
			});
		</script>
	<?php endif; ?>

	<script>
		// Rapor Modal Aç/Kapat Kodları (tüm tasarımlarda ortak, savunmacı null kontrolleriyle)
		document.addEventListener('DOMContentLoaded', function() {
			var openBtn = document.getElementById('sl-open-report-modal');
			var modal = document.getElementById('sl-report-modal');
			var closeBg = document.getElementById('sl-close-report-modal');
			var closeBtn = document.getElementById('sl-close-report-btn');
			if (openBtn && modal) { openBtn.addEventListener('click', function() { modal.classList.add('active'); }); }
			if (closeBg && modal) { closeBg.addEventListener('click', function() { modal.classList.remove('active'); }); }
			if (closeBtn && modal) { closeBtn.addEventListener('click', function() { modal.classList.remove('active'); }); }
			document.addEventListener('keydown', function(e) {
				if (e.key === 'Escape' && modal) { modal.classList.remove('active'); }
			});
		});
	</script>

	</main>
<?php endwhile; get_footer(); ?>
