<?php
/**
 * Oyun Detay - Sistem Gereksinimleri
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$post_id = get_the_ID();

// Minimum Gereksinimler
$min_os      = get_post_meta( $post_id, 'minimum_os', true );
$min_cpu     = get_post_meta( $post_id, 'minimum_cpu', true );
$min_ram     = get_post_meta( $post_id, 'minimum_ram', true );
$min_gpu     = get_post_meta( $post_id, 'minimum_gpu', true );
$min_storage = get_post_meta( $post_id, 'minimum_storage', true );

// Önerilen Gereksinimler
$rec_os      = get_post_meta( $post_id, 'recommended_os', true );
$rec_cpu     = get_post_meta( $post_id, 'recommended_cpu', true );
$rec_ram     = get_post_meta( $post_id, 'recommended_ram', true );
$rec_gpu     = get_post_meta( $post_id, 'recommended_gpu', true );
$rec_storage = get_post_meta( $post_id, 'recommended_storage', true );

// Eğer en az bir minimum gereksinim girilmişse kutuyu göster
if ( $min_os || $min_cpu || $min_ram ) :
?>
<div class="sl-content-box sl-sys-req-box">
	<h3><?php esc_html_e( 'Sistem Gereksinimleri', 'steamlike' ); ?></h3>
	
	<div class="sl-sys-req-grid">
		<div class="sl-req-col">
			<h4><?php esc_html_e( 'Minimum:', 'steamlike' ); ?></h4>
			<ul class="sl-req-list">
				<?php if ( $min_os ) echo '<li><strong>' . esc_html__( 'İşletim Sistemi:', 'steamlike' ) . '</strong> ' . esc_html( $min_os ) . '</li>'; ?>
				<?php if ( $min_cpu ) echo '<li><strong>' . esc_html__( 'İşlemci:', 'steamlike' ) . '</strong> ' . esc_html( $min_cpu ) . '</li>'; ?>
				<?php if ( $min_ram ) echo '<li><strong>' . esc_html__( 'Bellek:', 'steamlike' ) . '</strong> ' . esc_html( $min_ram ) . '</li>'; ?>
				<?php if ( $min_gpu ) echo '<li><strong>' . esc_html__( 'Ekran Kartı:', 'steamlike' ) . '</strong> ' . esc_html( $min_gpu ) . '</li>'; ?>
				<?php if ( $min_storage ) echo '<li><strong>' . esc_html__( 'Depolama:', 'steamlike' ) . '</strong> ' . esc_html( $min_storage ) . '</li>'; ?>
			</ul>
		</div>

		<?php if ( $rec_os || $rec_cpu || $rec_ram ) : ?>
		<div class="sl-req-col">
			<h4><?php esc_html_e( 'Önerilen:', 'steamlike' ); ?></h4>
			<ul class="sl-req-list">
				<?php if ( $rec_os ) echo '<li><strong>' . esc_html__( 'İşletim Sistemi:', 'steamlike' ) . '</strong> ' . esc_html( $rec_os ) . '</li>'; ?>
				<?php if ( $rec_cpu ) echo '<li><strong>' . esc_html__( 'İşlemci:', 'steamlike' ) . '</strong> ' . esc_html( $rec_cpu ) . '</li>'; ?>
				<?php if ( $rec_ram ) echo '<li><strong>' . esc_html__( 'Bellek:', 'steamlike' ) . '</strong> ' . esc_html( $rec_ram ) . '</li>'; ?>
				<?php if ( $rec_gpu ) echo '<li><strong>' . esc_html__( 'Ekran Kartı:', 'steamlike' ) . '</strong> ' . esc_html( $rec_gpu ) . '</li>'; ?>
				<?php if ( $rec_storage ) echo '<li><strong>' . esc_html__( 'Depolama:', 'steamlike' ) . '</strong> ' . esc_html( $rec_storage ) . '</li>'; ?>
			</ul>
		</div>
		<?php endif; ?>
	</div>
</div>
<?php endif; ?>
