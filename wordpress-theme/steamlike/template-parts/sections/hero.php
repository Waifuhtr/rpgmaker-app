<?php
/**
 * Ana Sayfa - Hero Slider
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) exit;

$args = array( 
	'post_type'      => 'game', 
	'posts_per_page' => 3, 
	'meta_query'     => array( array( 'key' => 'game_featured', 'value' => '1' ) ) 
);
$hero_games = new WP_Query( $args );

if ( $hero_games->have_posts() ) : ?>
<section class="sl-hero-slider">
	<?php while ( $hero_games->have_posts() ) : $hero_games->the_post(); 
		$bg_img = get_post_meta( get_the_ID(), 'game_background_image', true ) ?: get_the_post_thumbnail_url( get_the_ID(), 'full' );
	?>
		<div class="sl-hero-slide" style="background-image: url('<?php echo esc_url( $bg_img ); ?>');">
			<div class="sl-hero-overlay"></div>
			<div class="container sl-hero-inner">
				<div class="sl-hero-content">
					
					<div>
						<?php 
						// Durum (Tamamlandı vb. varsa daha büyük ve yeşil gösterir)
						$statuses = get_the_terms( get_the_ID(), 'game_status' );
						if ( $statuses && ! is_wp_error( $statuses ) ) {
							echo '<span class="sl-badge-status">' . esc_html( $statuses[0]->name ) . '</span>';
						}
						?>
						<span class="sl-badge-featured"><?php esc_html_e( 'ÖNE ÇIKAN', 'steamlike' ); ?></span>
					</div>

					<h1><?php the_title(); ?></h1>
					
					<p>
						<?php 
						$excerpt = get_the_excerpt();
						if ( empty( $excerpt ) ) {
							$excerpt = wp_trim_words( get_the_content(), 25, '...' );
						}
						echo esc_html( $excerpt ); 
						?>
					</p>
					
					<div style="margin-bottom: 25px;">
						<?php 
						// Neon Türler ve Platformlar
						$terms = array_merge( get_the_terms( get_the_ID(), 'game_platform' ) ?: array(), get_the_terms( get_the_ID(), 'game_genre' ) ?: array() );
						foreach ( $terms as $term ) {
							echo '<span class="sl-neon-tag">' . esc_html( $term->name ) . '</span>';
						}
						?>
					</div>

					<a href="<?php the_permalink(); ?>" class="sl-btn sl-btn-primary"><?php esc_html_e( 'Hemen İncele', 'steamlike' ); ?></a>
				</div>
			</div>
		</div>
	<?php endwhile; wp_reset_postdata(); ?>
</section>
<?php endif; ?>
