<?php
/**
 * Tema Footer Dosyası
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$sl_social = get_option( 'steamlike_settings' );
$sl_discord = ! empty( $sl_social['discord_url'] ) ? $sl_social['discord_url'] : '';
$sl_youtube = ! empty( $sl_social['youtube_url'] ) ? $sl_social['youtube_url'] : '';

$sl_footer_genres = get_terms( array(
	'taxonomy'   => 'game_genre',
	'number'     => 6,
	'hide_empty' => true,
) );
?>
	<footer id="colophon" class="sl-site-footer">
		<div class="container">
			<div class="sl-footer-grid">

				<div class="sl-footer-col sl-footer-brand">
					<?php if ( has_custom_logo() ) : the_custom_logo(); else : ?>
						<a href="<?php echo esc_url( home_url( '/' ) ); ?>" class="sl-text-logo"><?php bloginfo( 'name' ); ?></a>
					<?php endif; ?>
					<p class="sl-footer-brand-desc"><?php bloginfo( 'description' ); ?></p>
					<?php if ( $sl_discord || $sl_youtube ) : ?>
						<div class="sl-footer-social">
							<?php if ( $sl_discord ) : ?>
								<a href="<?php echo esc_url( $sl_discord ); ?>" target="_blank" rel="noopener noreferrer" aria-label="Discord"><span class="dashicons dashicons-groups"></span></a>
							<?php endif; ?>
							<?php if ( $sl_youtube ) : ?>
								<a href="<?php echo esc_url( $sl_youtube ); ?>" target="_blank" rel="noopener noreferrer" aria-label="YouTube"><span class="dashicons dashicons-video-alt3"></span></a>
							<?php endif; ?>
						</div>
					<?php endif; ?>
				</div>

				<div class="sl-footer-col">
					<h4><?php esc_html_e( 'Keşfet', 'steamlike' ); ?></h4>
					<?php if ( has_nav_menu( 'footer' ) ) : ?>
						<?php
						wp_nav_menu( array(
							'theme_location' => 'footer',
							'container'      => false,
							'items_wrap'     => '<ul>%3$s</ul>',
						) );
						?>
					<?php else : ?>
						<ul>
							<li><a href="<?php echo esc_url( home_url( '/' ) ); ?>"><?php esc_html_e( 'Ana Sayfa', 'steamlike' ); ?></a></li>
							<li><a href="<?php echo esc_url( get_post_type_archive_link( 'game' ) ); ?>"><?php esc_html_e( 'Oyun Kütüphanesi', 'steamlike' ); ?></a></li>
							<li><a href="<?php echo esc_url( home_url( '/hesabim/' ) ); ?>"><?php esc_html_e( 'Hesabım', 'steamlike' ); ?></a></li>
						</ul>
					<?php endif; ?>
				</div>

				<?php if ( ! empty( $sl_footer_genres ) && ! is_wp_error( $sl_footer_genres ) ) : ?>
				<div class="sl-footer-col">
					<h4><?php esc_html_e( 'Popüler Türler', 'steamlike' ); ?></h4>
					<ul>
						<?php foreach ( $sl_footer_genres as $sl_genre ) : ?>
							<li><a href="<?php echo esc_url( get_term_link( $sl_genre ) ); ?>"><?php echo esc_html( $sl_genre->name ); ?></a></li>
						<?php endforeach; ?>
					</ul>
				</div>
				<?php endif; ?>

				<div class="sl-footer-col">
					<h4><?php esc_html_e( 'Hesap', 'steamlike' ); ?></h4>
					<ul>
						<?php if ( is_user_logged_in() ) : ?>
							<li><a href="<?php echo esc_url( home_url( '/hesabim/' ) ); ?>"><?php esc_html_e( 'Profilim', 'steamlike' ); ?></a></li>
						<?php else : ?>
							<li><a href="<?php echo esc_url( home_url( '/hesabim/' ) ); ?>"><?php esc_html_e( 'Giriş / Kayıt', 'steamlike' ); ?></a></li>
						<?php endif; ?>
						<li><a href="<?php echo esc_url( home_url( '/?s=' ) ); ?>"><?php esc_html_e( 'Oyun Ara', 'steamlike' ); ?></a></li>
					</ul>
				</div>

			</div>

			<div class="sl-footer-bottom">
				<div class="site-info">
					<?php
					$footer_text = get_theme_mod( 'sl_footer_text', '&copy; ' . date( 'Y' ) . ' SteamLike. Proudly powered by WordPress.' );
					echo do_shortcode( wp_kses_post( $footer_text ) );
					?>
				</div>
				<div class="sl-footer-madeby"><span class="dashicons dashicons-controls-play"></span> <?php esc_html_e( 'İyi Oyunlar', 'steamlike' ); ?></div>
			</div>
		</div>
	</footer>
</div><?php wp_footer(); ?>
</body>
</html>
