<?php
/**
 * Yeni Çıkan Oyunlar Bölümü
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

// Son 12 oyunu tarihe göre azalan sırayla çek
$args = array(
	'post_type'      => 'game',
	'posts_per_page' => 12,
	'orderby'        => 'date',
	'order'          => 'DESC',
);

$latest_games = new WP_Query( $args );
?>

<section class="sl-section sl-latest-games">
	<div class="container" style="max-width: 1200px; margin: 0 auto; padding: 0 15px;">
		
		<div class="sl-section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid var(--sl-color-dark-400); padding-bottom: 10px;">
			<h2 class="sl-section-title" style="margin: 0; color: #f8fafc; font-size: 24px; font-weight: bold; border-left: 4px solid var(--sl-color-blue-100); padding-left: 10px;">
				<?php esc_html_e( 'Yeni Çıkanlar', 'steamlike' ); ?>
			</h2>
			
			<a href="<?php echo esc_url( get_post_type_archive_link( 'game' ) ); ?>" class="sl-view-all" style="color: var(--sl-color-blue-200); text-decoration: none; font-size: 14px; font-weight: 600; transition: var(--sl-transition-speed);">
				<?php esc_html_e( 'Tümünü Gör', 'steamlike' ); ?> &rarr;
			</a>
		</div>

		<?php if ( $latest_games->have_posts() ) : ?>
			
			<div class="post-grid">
				<?php
				while ( $latest_games->have_posts() ) :
					$latest_games->the_post();

					// Her döngüde daha önce yazdığımız oyun kartını çağırıyoruz
					get_template_part( 'template-parts/cards/card-game' );

				endwhile;
				wp_reset_postdata(); // Döngü bittikten sonra global post objesini sıfırla
				?>
			</div>

		<?php else : ?>
			
			<div class="sl-no-content" style="background: var(--sl-color-dark-200); padding: 30px; border-radius: var(--sl-border-radius); text-align: center; color: #94a3b8;">
				<p><?php esc_html_e( 'Henüz sisteme eklenmiş bir oyun bulunmuyor.', 'steamlike' ); ?></p>
			</div>

		<?php endif; ?>
		
	</div>
</section>
