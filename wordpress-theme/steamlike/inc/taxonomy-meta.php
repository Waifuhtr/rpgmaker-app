<?php
/**
 * Taxonomy Custom Meta Fields (İkon ve Bayraklar İçin)
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class SteamLike_Taxonomy_Meta {

	private $taxonomies = array( 'game_platform', 'game_language', 'game_features' );

	public function __construct() {
		foreach ( $this->taxonomies as $tax ) {
			// Yeni kategori ekleme ekranı
			add_action( "{$tax}_add_form_fields", array( $this, 'add_icon_field' ), 10, 2 );
			// Var olan kategoriyi düzenleme ekranı
			add_action( "{$tax}_edit_form_fields", array( $this, 'edit_icon_field' ), 10, 2 );
			// Verileri kaydetme
			add_action( "edited_{$tax}", array( $this, 'save_icon_field' ), 10, 2 );
			add_action( "create_{$tax}", array( $this, 'save_icon_field' ), 10, 2 );
		}
	}

	public function add_icon_field() {
		?>
		<div class="form-field term-icon-wrap">
			<label for="sl_term_icon"><?php esc_html_e( 'İkon / Bayrak', 'steamlike' ); ?></label>
			<input type="text" name="sl_term_icon" id="sl_term_icon" value="">
			<p><?php esc_html_e( 'Örn: Bayrak emojisi (🇹🇷, 🇬🇧) kopyalayıp yapıştırabilir veya bir ikon resim URL\'si (https://...) girebilirsiniz.', 'steamlike' ); ?></p>
		</div>
		<?php
	}

	public function edit_icon_field( $term ) {
		$icon = get_term_meta( $term->term_id, 'sl_term_icon', true );
		?>
		<tr class="form-field term-icon-wrap">
			<th scope="row"><label for="sl_term_icon"><?php esc_html_e( 'İkon / Bayrak', 'steamlike' ); ?></label></th>
			<td>
				<input type="text" name="sl_term_icon" id="sl_term_icon" value="<?php echo esc_attr( $icon ); ?>">
				<p class="description"><?php esc_html_e( 'Emojiler (🇹🇷) metin olarak kalır, URL girerseniz resim olarak gösterilir.', 'steamlike' ); ?></p>
			</td>
		</tr>
		<?php
	}

	public function save_icon_field( $term_id ) {
		if ( isset( $_POST['sl_term_icon'] ) ) {
			// Emoji veya URL olabileceği için sanitize_text_field kullanıyoruz (URL'yi bozmaması için unslash ile birlikte)
			$icon = sanitize_text_field( wp_unslash( $_POST['sl_term_icon'] ) );
			update_term_meta( $term_id, 'sl_term_icon', $icon );
		}
	}
}
