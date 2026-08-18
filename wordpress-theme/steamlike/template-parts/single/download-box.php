<?php
/**
 * Oyun Detay - İndirme Kutusu (Sidebar)
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$post_id = get_the_ID();

$download_url = get_post_meta( $post_id, 'game_download_url', true );
$mirror_url   = get_post_meta( $post_id, 'game_download_mirror', true );
$password     = get_post_meta( $post_id, 'game_download_password', true );
$game_size    = get_post_meta( $post_id, 'game_size', true );
$game_version = get_post_meta( $post_id, 'game_version', true );

// Sayaç verilerini çek
$view_count     = (int) get_post_meta( $post_id, 'game_view_count', true );
$download_count = (int) get_post_meta( $post_id, 'game_download_count', true );
?>

<div class="sl-sidebar-widget sl-download-widget">
	<div class="sl-download-price">
		<?php esc_html_e( 'Ücretsiz İndir', 'steamlike' ); ?>
	</div>

	<?php if ( $download_url ) : ?>
		<a href="<?php echo esc_url( home_url( '/indir/?game_id=' . $post_id ) ); ?>" target="_blank" rel="noopener noreferrer" class="sl-btn sl-btn-primary sl-btn-large sl-btn-block sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>">
	<span class="dashicons dashicons-download"></span> <?php esc_html_e( 'Hemen İndir', 'steamlike' ); ?>
</a>

	<?php else : ?>
		<button class="sl-btn sl-btn-disabled sl-btn-large sl-btn-block" disabled>
			<?php esc_html_e( 'Link Bekleniyor', 'steamlike' ); ?>
		</button>
	<?php endif; ?>

	<?php if ( $mirror_url ) : ?>
		<a href="<?php echo esc_url( $mirror_url ); ?>" target="_blank" rel="noopener noreferrer" class="sl-btn sl-btn-secondary sl-btn-block sl-track-download" data-post-id="<?php echo esc_attr( $post_id ); ?>" style="margin-top: 10px;">
			<?php esc_html_e( 'Alternatif Link (Mirror)', 'steamlike' ); ?>
		</a>
	<?php endif; ?>

	<ul class="sl-download-info">
		<?php if ( $game_size ) : ?>
			<li><span class="dashicons dashicons-database"></span> <strong><?php esc_html_e( 'Boyut:', 'steamlike' ); ?></strong> <?php echo esc_html( $game_size ); ?></li>
		<?php endif; ?>
		
		<?php if ( $game_version ) : ?>
			<li><span class="dashicons dashicons-tag"></span> <strong><?php esc_html_e( 'Sürüm:', 'steamlike' ); ?></strong> <?php echo esc_html( $game_version ); ?></li>
		<?php endif; ?>

		<?php if ( $password ) : ?>
			<li class="sl-password-box">
				<span class="dashicons dashicons-lock"></span> <strong><?php esc_html_e( 'Arşiv Şifresi:', 'steamlike' ); ?></strong> 
				<code style="background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px;"><?php echo esc_html( $password ); ?></code>
			</li>
		<?php endif; ?>
	</ul>

	<div style="margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.05); display: flex; justify-content: space-between; font-size: 13px; color: #94a3b8;">
		<div><span class="dashicons dashicons-visibility" style="vertical-align: text-bottom; font-size: 16px;"></span> <?php echo esc_html( $view_count ); ?> Görüntülenme</div>
		<div><span class="dashicons dashicons-download" style="vertical-align: text-bottom; font-size: 16px;"></span> <span class="sl-download-count-val"><?php echo esc_html( $download_count ); ?></span> İndirme</div>
	</div>
</div>
