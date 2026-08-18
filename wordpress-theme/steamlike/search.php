<?php
/**
 * Arama Sonuçları Şablonu
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

get_header();
?>

<main id="primary" class="site-main sl-search-page">
	<div class="container">

		<?php sl_breadcrumb(); ?>

		<header class="page-header">
			<h1 class="page-title">
				<?php
				/* translators: %s: Arama terimi. */
				printf( esc_html__( 'Arama Sonuçları: %s', 'steamlike' ), '<span class="search-term">' . get_search_query() . '</span>' );
				?>
			</h1>
		</header>

		<?php if ( have_posts() ) : ?>

			<div class="post-grid">
				<?php
				while ( have_posts() ) :
					the_post();
					
					// Kart şablonumuzu çağırıyoruz
					get_template_part( 'template-parts/cards/card-game' );

				endwhile;
				?>
			</div>

			<?php
			// Sayfalama (Pagination)
			the_posts_pagination(
				array(
					'prev_text' => '<span class="dashicons dashicons-arrow-left-alt2"></span>',
					'next_text' => '<span class="dashicons dashicons-arrow-right-alt2"></span>',
					'class'     => 'sl-pagination',
				)
			);
			?>

		<?php else : ?>

			<div class="sl-no-results">
				<span class="dashicons dashicons-search"></span>
				<h2><?php esc_html_e( 'Hiçbir sonuç bulunamadı.', 'steamlike' ); ?></h2>
				<p><?php esc_html_e( 'Farklı kelimelerle veya daha genel bir terimle tekrar aramayı deneyin.', 'steamlike' ); ?></p>
			</div>

		<?php endif; ?>

	</div>
</main>

<?php
get_footer();
