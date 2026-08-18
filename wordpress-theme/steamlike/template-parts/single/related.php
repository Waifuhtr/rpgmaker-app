<?php
/**
 * Oyun Detay - Benzer Oyunlar Modülü
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) exit;

$current_id = get_the_ID();

// Mevcut oyunun türlerini (genre) al
$genres = wp_get_post_terms( $current_id, 'game_genre', array( 'fields' => 'ids' ) );

// Eğer oyunun bir türü yoksa benzer oyun gösteremeyiz
if ( empty( $genres ) || is_wp_error( $genres ) ) {
	return;
}

// Aynı türdeki diğer oyunları çek (Mevcut oyunu hariç tut)
$args = array(
	'post_type'      => 'game',
	'posts_per_page' => 4, // 4 adet benzer oyun göster
	'post__not_in'   => array( $current_id ), // Kendini gösterme
	'tax_query'      => array(
		array(
			'taxonomy' => 'game_genre',
			'field'    => 'id',
			'terms'    => $genres,
			'operator' => 'IN',
		),
	),
);

$related_games = new WP_Query( $args );

if ( $related_games->have_posts() ) : ?>
	
	<div class="sl-content-box sl-related-games" style="margin-top: 30px;">
		<h3 style="margin-bottom: 20px; font-size: 20px;"><?php esc_html_e( 'Bunlar da Hoşuna Gidebilir', 'steamlike' ); ?></h3>
		
		<div class="post-grid">
			<?php 
			while ( $related_games->have_posts() ) : $related_games->the_post();
				get_template_part( 'template-parts/cards/card-game' );
			endwhile; 
			wp_reset_postdata(); 
			?>
		</div>
	</div>

<?php endif; ?>
