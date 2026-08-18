<?php
/**
 * Oyun Detay - Meta Bilgileri (Sidebar)
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$post_id = get_the_ID();

// İkonlu taksonomi çeken özel fonksiyonumuz
$get_terms_with_icon = function( $taxonomy ) use ( $post_id ) {
	$terms = get_the_terms( $post_id, $taxonomy );
	if ( is_wp_error( $terms ) || empty( $terms ) ) return '-';

	$output = array();
	foreach ( $terms as $term ) {
		$icon_meta = get_term_meta( $term->term_id, 'sl_term_icon', true );
		$icon_html = '';
		
		if ( $icon_meta ) {
			// Eğer meta veri "http" ile başlıyorsa bu bir resim URL'sidir
			if ( strpos( $icon_meta, 'http' ) === 0 ) {
				$icon_html = '<img src="' . esc_url( $icon_meta ) . '" class="sl-term-icon" alt="' . esc_attr( $term->name ) . '">';
			} else {
				// Değilse emojidir (🇹🇷 vb.)
				$icon_html = '<span class="sl-term-emoji">' . esc_html( $icon_meta ) . '</span>';
			}
		}
		
		$term_link = get_term_link( $term );
		$output[]  = '<a href="' . esc_url( $term_link ) . '">' . $icon_html . esc_html( $term->name ) . '</a>';
	}
	
	return implode( ', ', $output );
};
?>

<div class="sl-sidebar-widget sl-meta-widget">
	<table class="sl-meta-table">
		<tbody>
			<tr>
				<th><?php esc_html_e( 'Geliştirici:', 'steamlike' ); ?></th>
				<td><?php echo wp_kses_post( $get_terms_with_icon( 'game_developer' ) ); ?></td>
			</tr>
			<tr>
				<th><?php esc_html_e( 'Yayıncı:', 'steamlike' ); ?></th>
				<td><?php echo wp_kses_post( $get_terms_with_icon( 'game_publisher' ) ); ?></td>
			</tr>
			<tr>
				<th><?php esc_html_e( 'Platform:', 'steamlike' ); ?></th>
				<td><?php echo wp_kses_post( $get_terms_with_icon( 'game_platform' ) ); ?></td>
			</tr>
			<tr>
				<th><?php esc_html_e( 'Dil:', 'steamlike' ); ?></th>
				<td><?php echo wp_kses_post( $get_terms_with_icon( 'game_language' ) ); ?></td>
			</tr>
			<tr>
				<th><?php esc_html_e( 'Özellikler:', 'steamlike' ); ?></th>
				<td><?php echo wp_kses_post( $get_terms_with_icon( 'game_features' ) ); ?></td>
			</tr>
		</tbody>
	</table>
</div>
