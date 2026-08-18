<?php
/**
 * Ana Sayfa Şablonu
 * Özelleştiriciden seçilebilen 4 farklı tam sayfa tasarımı (Varsayılan, Play Store,
 * itch.io, Holo-Terminal) — tüm tasarımlar aynı veri sorgularını paylaşır.
 */
if ( ! defined( 'ABSPATH' ) ) exit;
get_header();

// ============================================================
// PAYLAŞILAN VERİ SORGULARI (Tüm tasarımlar bunları kullanır)
// ============================================================
$hero_args  = array( 'post_type' => 'game', 'posts_per_page' => 5, 'meta_key' => 'game_featured', 'meta_value' => '1' );
$hero_query = new WP_Query( $hero_args );
$hero_count = $hero_query->post_count;

$fp_style = get_theme_mod( 'sl_frontpage_style', 'default' );
?>

<main id="primary" class="site-main sl-fp-style-<?php echo esc_attr( $fp_style ); ?>">

	<?php if ( 'itchio' !== $fp_style ) : ?>
	<div class="sl-hero-slider-wrapper">
		<div class="sl-hero-track" id="sl-hero-track">
			<?php
			if ( $hero_query->have_posts() ) :
				while ( $hero_query->have_posts() ) : $hero_query->the_post();
					$bg_img = get_post_meta( get_the_ID(), 'game_background_image', true ) ?: get_the_post_thumbnail_url( get_the_ID(), 'full' );
					$subtitle = get_post_meta( get_the_ID(), 'game_subtitle', true );
					?>
					<div class="sl-hero-slide" style="background-image: url('<?php echo esc_url( $bg_img ); ?>');">
						<div class="sl-hero-overlay"></div>
						<div class="sl-hero-inner container">
							<div class="sl-hero-content">
								<span class="sl-badge-featured"><span class="dashicons dashicons-star-filled"></span> <?php esc_html_e( 'Öne Çıkan', 'steamlike' ); ?></span>
								<h1><?php the_title(); ?></h1>
								<?php if ( $subtitle ) : ?><p class="sl-hero-subtitle"><?php echo esc_html( $subtitle ); ?></p><?php endif; ?>
								<p><?php echo wp_trim_words( get_the_excerpt(), 20, '...' ); ?></p>
								<a href="<?php the_permalink(); ?>" class="sl-btn sl-btn-primary"><?php esc_html_e( 'Oyunu İncele', 'steamlike' ); ?></a>
							</div>
						</div>
					</div>
				<?php endwhile; wp_reset_postdata(); else : ?>
					<div class="sl-hero-empty"><?php esc_html_e( 'Öne çıkan oyun bulunamadı. Lütfen panelden "Öne Çıkan Yap" kutucuğunu işaretleyin.', 'steamlike' ); ?></div>
				<?php endif; ?>
		</div>

		<button class="sl-slider-btn sl-slider-prev" onclick="slMoveSlider(-1)" aria-label="<?php esc_attr_e( 'Önceki', 'steamlike' ); ?>">&#10094;</button>
		<button class="sl-slider-btn sl-slider-next" onclick="slMoveSlider(1)" aria-label="<?php esc_attr_e( 'Sonraki', 'steamlike' ); ?>">&#10095;</button>

		<?php if ( $hero_count > 1 ) : ?>
		<div class="sl-hero-dots" id="sl-hero-dots">
			<?php for ( $sl_i = 0; $sl_i < $hero_count; $sl_i++ ) : ?>
				<button class="sl-hero-dot<?php echo ( 0 === $sl_i ) ? ' active' : ''; ?>" onclick="slGoToSlide(<?php echo esc_attr( $sl_i ); ?>)" aria-label="<?php echo esc_attr( sprintf( __( 'Slayt %d', 'steamlike' ), $sl_i + 1 ) ); ?>"></button>
			<?php endfor; ?>
		</div>
		<?php endif; ?>
	</div>

	<script>
		let slCurrentSlide = 0;
		let slAutoTimer = null;
		const slTrack = document.getElementById('sl-hero-track');
		const slSlides = document.querySelectorAll('.sl-hero-slide');
		const slDots = document.querySelectorAll('.sl-hero-dot');
		const slTotalSlides = slSlides.length;
		const slWrapper = document.querySelector('.sl-hero-slider-wrapper');

		function slUpdateDots() { slDots.forEach((dot, i) => dot.classList.toggle('active', i === slCurrentSlide)); }

		function slMoveSlider(direction) {
            if(slTotalSlides <= 1) return;
			slCurrentSlide += direction;
			if (slCurrentSlide >= slTotalSlides) slCurrentSlide = 0;
			if (slCurrentSlide < 0) slCurrentSlide = slTotalSlides - 1;
			slTrack.style.transform = `translateX(-${slCurrentSlide * 100}%)`;
			slUpdateDots();
		}
		function slGoToSlide(index) {
			if (slTotalSlides <= 1) return;
			slCurrentSlide = index;
			slTrack.style.transform = `translateX(-${slCurrentSlide * 100}%)`;
			slUpdateDots();
		}
		function slStartAuto() { if (slTotalSlides > 1) { slAutoTimer = setInterval(() => { slMoveSlider(1); }, 6000); } }
		slStartAuto();
		if (slWrapper) {
			slWrapper.addEventListener('mouseenter', () => clearInterval(slAutoTimer));
			slWrapper.addEventListener('mouseleave', slStartAuto);
		}
	</script>
	<?php else : ?>
	<div class="sl-fp-itch-header">
		<div class="container">
			<span class="sl-eyebrow"><?php bloginfo( 'name' ); ?></span>
			<h1><?php bloginfo( 'description' ); ?></h1>
			<p><?php esc_html_e( 'Yeni eklenen, en çok oylanan ve en çok oynanan oyunları keşfedin.', 'steamlike' ); ?></p>
		</div>
	</div>
	<?php endif; ?>

    <div class="container sl-homepage-sections">

		<?php if ( 'playstore' === $fp_style ) : ?>

			<div class="sl-homepage-section sl-fp-ps-row">
				<h2><span class="dashicons dashicons-megaphone"></span> <?php esc_html_e( 'Yeni Eklenenler', 'steamlike' ); ?></h2>
				<div class="post-grid sl-fp-scroll-row">
					<?php $new_games = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'orderby' => 'date', 'order' => 'DESC' ) );
					if ( $new_games->have_posts() ) : while ( $new_games->have_posts() ) : $new_games->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oyun eklenmemiş.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section sl-fp-ps-row">
				<h2><span class="dashicons dashicons-star-filled"></span> <?php esc_html_e( 'En Çok Oylananlar', 'steamlike' ); ?></h2>
				<div class="post-grid sl-fp-scroll-row">
					<?php $top_rated = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_user_rating_avg', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $top_rated->have_posts() ) : while ( $top_rated->have_posts() ) : $top_rated->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oylanan oyun bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section sl-fp-ps-row">
				<h2><span class="dashicons dashicons-clock"></span> <?php esc_html_e( 'En Çok Oynananlar', 'steamlike' ); ?></h2>
				<div class="post-grid sl-fp-scroll-row">
					<?php $most_played = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_total_playtime', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $most_played->have_posts() ) : while ( $most_played->have_posts() ) : $most_played->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Süre verisi bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

		<?php elseif ( 'itchio' === $fp_style ) : ?>

			<div class="sl-homepage-section">
				<div class="sl-fp-itch-section-head"><h2><?php esc_html_e( 'Yeni Eklenenler', 'steamlike' ); ?></h2><a href="<?php echo esc_url( get_post_type_archive_link('game') ); ?>" class="sl-section-more"><?php esc_html_e( 'Tümünü Gör', 'steamlike' ); ?> &rarr;</a></div>
				<div class="post-grid sl-fp-dense-grid">
					<?php $new_games = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 10, 'orderby' => 'date', 'order' => 'DESC' ) );
					if ( $new_games->have_posts() ) : while ( $new_games->have_posts() ) : $new_games->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oyun eklenmemiş.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section">
				<div class="sl-fp-itch-section-head"><h2><?php esc_html_e( 'En Çok Oylananlar', 'steamlike' ); ?></h2><a href="<?php echo esc_url( get_post_type_archive_link('game') ); ?>" class="sl-section-more"><?php esc_html_e( 'Tümünü Gör', 'steamlike' ); ?> &rarr;</a></div>
				<div class="post-grid sl-fp-dense-grid">
					<?php $top_rated = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 10, 'meta_key' => 'sl_user_rating_avg', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $top_rated->have_posts() ) : while ( $top_rated->have_posts() ) : $top_rated->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oylanan oyun bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section">
				<div class="sl-fp-itch-section-head"><h2><?php esc_html_e( 'En Çok Oynananlar', 'steamlike' ); ?></h2><a href="<?php echo esc_url( get_post_type_archive_link('game') ); ?>" class="sl-section-more"><?php esc_html_e( 'Tümünü Gör', 'steamlike' ); ?> &rarr;</a></div>
				<div class="post-grid sl-fp-dense-grid">
					<?php $most_played = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 10, 'meta_key' => 'sl_total_playtime', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $most_played->have_posts() ) : while ( $most_played->have_posts() ) : $most_played->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Süre verisi bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

		<?php elseif ( 'holo' === $fp_style ) : ?>

			<div class="sl-homepage-section sl-sg-holo-terminal sl-fp-holo-panel">
				<h2><span class="sl-mono sl-text-signal">[ YENİ_EKLENENLER ]</span></h2>
				<div class="post-grid">
					<?php $new_games = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'orderby' => 'date', 'order' => 'DESC' ) );
					if ( $new_games->have_posts() ) : while ( $new_games->have_posts() ) : $new_games->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oyun eklenmemiş.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section sl-sg-holo-terminal sl-fp-holo-panel">
				<h2><span class="sl-mono sl-text-gold">[ EN_YÜKSEK_PUAN ]</span></h2>
				<div class="post-grid">
					<?php $top_rated = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_user_rating_avg', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $top_rated->have_posts() ) : while ( $top_rated->have_posts() ) : $top_rated->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oylanan oyun bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section sl-sg-holo-terminal sl-fp-holo-panel">
				<h2><span class="sl-mono sl-text-success">[ EN_ÇOK_OYNANAN ]</span></h2>
				<div class="post-grid">
					<?php $most_played = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_total_playtime', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $most_played->have_posts() ) : while ( $most_played->have_posts() ) : $most_played->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Süre verisi bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

		<?php else : ?>

			<div class="sl-homepage-section">
				<h2><span class="dashicons dashicons-megaphone"></span> <?php esc_html_e( 'Yeni Eklenenler', 'steamlike' ); ?></h2>
				<div class="post-grid">
					<?php $new_games = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'orderby' => 'date', 'order' => 'DESC' ) );
					if ( $new_games->have_posts() ) : while ( $new_games->have_posts() ) : $new_games->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oyun eklenmemiş.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section">
				<h2><span class="dashicons dashicons-star-filled"></span> <?php esc_html_e( 'En Çok Oylananlar', 'steamlike' ); ?></h2>
				<div class="post-grid">
					<?php $top_rated = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_user_rating_avg', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $top_rated->have_posts() ) : while ( $top_rated->have_posts() ) : $top_rated->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Henüz oylanan oyun bulunmuyor.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

			<div class="sl-homepage-section">
				<h2><span class="dashicons dashicons-clock"></span> <?php esc_html_e( 'En Çok Oynananlar', 'steamlike' ); ?></h2>
				<div class="post-grid">
					<?php $most_played = new WP_Query( array( 'post_type' => 'game', 'posts_per_page' => 8, 'meta_key' => 'sl_total_playtime', 'orderby' => 'meta_value_num', 'order' => 'DESC' ) );
					if ( $most_played->have_posts() ) : while ( $most_played->have_posts() ) : $most_played->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					endwhile; wp_reset_postdata(); else: ?><p class="sl-empty-note"><?php esc_html_e( 'Admin panelinden henüz "Süreleri Senkronize Et" işlemi yapılmamış veya oynanan oyun yok.', 'steamlike' ); ?></p><?php endif; ?>
				</div>
			</div>

		<?php endif; ?>

    </div>
</main>

<?php get_footer(); ?>
