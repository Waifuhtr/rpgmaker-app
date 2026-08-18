<?php
/**
 * Ana şablon dosyası
 *
 * WordPress tema hiyerarşisinde en temel dosyadır.
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

get_header();
?>

	<main id="primary" class="site-main">
		<div class="container">
			
			<?php
			if ( have_posts() ) :

				if ( is_home() && ! is_front_page() ) :
					?>
					<header>
						<h1 class="page-title screen-reader-text"><?php single_post_title(); ?></h1>
					</header>
					<?php
				endif;

				echo '<div class="post-grid">';
				/* Döngüyü başlat */
				while ( have_posts() ) :
					the_post();

					// Normal blog yazıları için geçici bir içerik çıktısı (İleride template-parts'a böleceğiz)
					?>
					<article id="post-<?php the_ID(); ?>" <?php post_class( 'steamlike-card' ); ?>>
						<header class="entry-header">
							<?php
							if ( is_singular() ) :
								the_title( '<h1 class="entry-title">', '</h1>' );
							else :
								the_title( '<h2 class="entry-title"><a href="' . esc_url( get_permalink() ) . '" rel="bookmark">', '</a></h2>' );
							endif;
							?>
						</header>

						<div class="entry-summary">
							<?php the_excerpt(); ?>
						</div>
					</article>
					<?php

				endwhile;
				echo '</div>';

				the_posts_navigation();

			else :

				// İçerik bulunamadığında gösterilecek kısım
				echo '<p>' . esc_html__( 'Henüz bir içerik bulunmuyor.', 'steamlike' ) . '</p>';

			endif;
			?>

		</div></main><?php
get_footer();
