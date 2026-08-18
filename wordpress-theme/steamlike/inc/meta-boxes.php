<?php
/**
 * Game Custom Meta Boxes
 */
if ( ! defined( 'ABSPATH' ) ) exit;

class SteamLike_Meta_Boxes {

	public function __construct() {
		add_action( 'add_meta_boxes', array( $this, 'add_game_meta_boxes' ) );
		add_action( 'save_post', array( $this, 'save_game_meta_boxes' ) );
		add_action( 'admin_enqueue_scripts', array( $this, 'enqueue_media_uploader' ) );
	}

	public function enqueue_media_uploader() { wp_enqueue_media(); }

	public function add_game_meta_boxes() {
		add_meta_box( 'sl_game_basic_meta', __( 'Temel Bilgiler', 'steamlike' ), array( $this, 'render_basic_meta' ), 'game', 'normal', 'high' );
		add_meta_box( 'sl_game_sysreq_meta', __( 'Sistem Gereksinimleri', 'steamlike' ), array( $this, 'render_sysreq_meta' ), 'game', 'normal', 'high' );
		add_meta_box( 'sl_game_download_meta', __( 'İndirme Alanları', 'steamlike' ), array( $this, 'render_download_meta' ), 'game', 'normal', 'high' );
		add_meta_box( 'sl_game_media_meta', __( 'Medya ve Galeri', 'steamlike' ), array( $this, 'render_media_meta' ), 'game', 'normal', 'high' );
		add_meta_box( 'sl_game_stats_meta', __( 'Teknik ve İstatistikler', 'steamlike' ), array( $this, 'render_stats_meta' ), 'game', 'side', 'low' );
	}

	private function get_meta_fields() {
		return array(
			'game_api_id'        => array( 'label' => __( 'API Oyun ID (Süre Çekmek İçin)', 'steamlike' ), 'type' => 'text' ),
			'game_subtitle'      => array( 'label' => __( 'Alt Başlık', 'steamlike' ), 'type' => 'text' ),
			'game_changelog'     => array( 'label' => __( 'Değişiklik Günlüğü / Özel Notlar', 'steamlike' ), 'type' => 'textarea' ),
			'game_version'       => array( 'label' => __( 'Oyun Sürümü', 'steamlike' ), 'type' => 'text' ),
			'game_size'          => array( 'label' => __( 'Dosya Boyutu (Örn: 45 GB)', 'steamlike' ), 'type' => 'text' ),
			'game_release_date'  => array( 'label' => __( 'Çıkış Tarihi', 'steamlike' ), 'type' => 'date' ),
			'game_age_rating'    => array( 'label' => __( 'Yaş Sınırı (Örn: +18)', 'steamlike' ), 'type' => 'text' ),
			'game_license_type'  => array( 'label' => __( 'Lisans Türü', 'steamlike' ), 'type' => 'text' ),
			'minimum_os'         => array( 'label' => __( 'Min OS', 'steamlike' ), 'type' => 'text' ),
			'minimum_cpu'        => array( 'label' => __( 'Min CPU', 'steamlike' ), 'type' => 'text' ),
			'minimum_ram'        => array( 'label' => __( 'Min RAM', 'steamlike' ), 'type' => 'text' ),
			'minimum_gpu'        => array( 'label' => __( 'Min GPU', 'steamlike' ), 'type' => 'text' ),
			'minimum_storage'    => array( 'label' => __( 'Min Depolama', 'steamlike' ), 'type' => 'text' ),
			'recommended_os'     => array( 'label' => __( 'Önerilen OS', 'steamlike' ), 'type' => 'text' ),
			'recommended_cpu'    => array( 'label' => __( 'Önerilen CPU', 'steamlike' ), 'type' => 'text' ),
			'recommended_ram'    => array( 'label' => __( 'Önerilen RAM', 'steamlike' ), 'type' => 'text' ),
			'recommended_gpu'    => array( 'label' => __( 'Önerilen GPU', 'steamlike' ), 'type' => 'text' ),
			'recommended_storage'=> array( 'label' => __( 'Önerilen Depolama', 'steamlike' ), 'type' => 'text' ),
			'game_download_url'       => array( 'label' => __( 'Ana İndirme Linki', 'steamlike' ), 'type' => 'url' ),
			'game_download_mirror'    => array( 'label' => __( 'Alternatif İndirme Linki', 'steamlike' ), 'type' => 'url' ),
			'game_download_password'  => array( 'label' => __( 'Arşiv Şifresi', 'steamlike' ), 'type' => 'text' ),
			'game_installation_guide' => array( 'label' => __( 'Kurulum Rehberi', 'steamlike' ), 'type' => 'textarea' ),
			'game_trailer_url'   => array( 'label' => __( 'Fragman (YouTube URL)', 'steamlike' ), 'type' => 'url' ),
			'game_background_image'=> array( 'label' => __( 'Arka Plan Görseli URL', 'steamlike' ), 'type' => 'text' ),
			'game_screenshots'   => array( 'label' => __( 'Ekran Görüntüleri', 'steamlike' ), 'type' => 'gallery' ),
			'game_multiplayer_support'=> array( 'label' => __( 'Multiplayer Desteği', 'steamlike' ), 'type' => 'checkbox' ),
			'game_controller_support' => array( 'label' => __( 'Oyun Kolu Desteği', 'steamlike' ), 'type' => 'checkbox' ),
			'game_featured'           => array( 'label' => __( 'Öne Çıkan Yap', 'steamlike' ), 'type' => 'checkbox' ),
			'game_editors_choice'     => array( 'label' => __( 'Editörün Seçimi', 'steamlike' ), 'type' => 'checkbox' ),
			'game_rating'             => array( 'label' => __( 'Manuel Puan (1-100)', 'steamlike' ), 'type' => 'number' ),
		);
	}

	private function render_fields( $post, $field_keys ) {
		wp_nonce_field( 'sl_save_game_meta', 'sl_game_meta_nonce' );
		$all_fields = $this->get_meta_fields();

		echo '<div style="display: flex; flex-wrap: wrap; gap: 15px;">';
		foreach ( $field_keys as $key ) {
			if ( ! isset( $all_fields[ $key ] ) ) continue;
			
			$field = $all_fields[ $key ];
			$value = get_post_meta( $post->ID, $key, true );
			
			echo '<div style="flex: 1 1 45%; min-width: 250px; margin-bottom: 10px;">';
			echo '<label for="' . esc_attr( $key ) . '" style="display: block; font-weight: bold; margin-bottom: 5px;">' . esc_html( $field['label'] ) . '</label>';

			if ( $field['type'] === 'textarea' ) {
				echo '<textarea id="' . esc_attr( $key ) . '" name="' . esc_attr( $key ) . '" style="width: 100%; height: 100px;">' . esc_textarea( $value ) . '</textarea>';
			} elseif ( $field['type'] === 'checkbox' ) {
				$checked = checked( $value, '1', false );
				echo '<input type="checkbox" id="' . esc_attr( $key ) . '" name="' . esc_attr( $key ) . '" value="1" ' . $checked . '>';
			} elseif ( $field['type'] === 'gallery' ) {
				echo '<p style="font-size:11px; color:#666;">Not: Tek seferde çoklu resim seçmek için CTRL tuşuna basılı tutarak tıklayın.</p>';
				echo '<input type="hidden" id="sl_screenshots_input" name="' . esc_attr( $key ) . '" value="' . esc_attr( $value ) . '">';
				echo '<button type="button" class="button button-secondary" id="sl_upload_gallery_btn">' . esc_html__( 'Görsel Seç / Ekle', 'steamlike' ) . '</button>';
				echo '<button type="button" class="button button-link-delete" id="sl_clear_gallery_btn" style="color:red; margin-left:10px;">' . esc_html__( 'Tümünü Temizle', 'steamlike' ) . '</button>';
				echo '<div id="sl_gallery_preview" style="display:flex; gap:10px; margin-top:10px; flex-wrap:wrap;">';
				if ( ! empty( $value ) ) {
					$ids = explode( ',', $value );
					foreach ( $ids as $id ) {
						$url = wp_get_attachment_thumb_url( $id );
						if ( $url ) echo '<img src="' . esc_url( $url ) . '" style="width:80px; height:80px; object-fit:cover; border-radius:4px; border:1px solid #ccc;">';
					}
				}
				echo '</div>';
				
				?>
				<script>
				jQuery(document).ready(function($){
					$('#sl_upload_gallery_btn').on('click', function(e){
						e.preventDefault();
						var frame = wp.media({ title: 'Ekran Görüntüleri Seç', button: { text: 'Ekle' }, multiple: true });
						frame.on('select', function(){
							var attachment = frame.state().get('selection').toJSON();
							var existing_ids = $('#sl_screenshots_input').val() ? $('#sl_screenshots_input').val().split(',') : [];
							
							$.each(attachment, function(index, value){
								var id_str = value.id.toString();
								if($.inArray(id_str, existing_ids) === -1) {
									existing_ids.push(id_str);
									var url = value.sizes && value.sizes.thumbnail ? value.sizes.thumbnail.url : value.url;
									$('#sl_gallery_preview').append('<img src="'+url+'" style="width:80px; height:80px; object-fit:cover; border-radius:4px; border:1px solid #ccc;">');
								}
							});
							$('#sl_screenshots_input').val(existing_ids.join(','));
						});
						frame.open();
					});
					$('#sl_clear_gallery_btn').on('click', function(e){
						e.preventDefault();
						$('#sl_screenshots_input').val('');
						$('#sl_gallery_preview').html('');
					});
				});
				</script>
				<?php
			} else {
				$type_attr = in_array( $field['type'], ['url', 'number', 'date'] ) ? $field['type'] : 'text';
				echo '<input type="' . esc_attr( $type_attr ) . '" id="' . esc_attr( $key ) . '" name="' . esc_attr( $key ) . '" value="' . esc_attr( $value ) . '" style="width: 100%;">';
			}
			echo '</div>';
		}
		echo '</div>';
	}

	public function render_basic_meta( $post ) { $this->render_fields( $post, array( 'game_api_id', 'game_subtitle', 'game_changelog', 'game_version', 'game_size', 'game_release_date', 'game_age_rating', 'game_license_type' ) ); }
	
	public function render_sysreq_meta( $post ) {
		echo '<h4>' . esc_html__( 'Minimum Gereksinimler', 'steamlike' ) . '</h4>';
		$this->render_fields( $post, array( 'minimum_os', 'minimum_cpu', 'minimum_ram', 'minimum_gpu', 'minimum_storage' ) );
		echo '<hr><h4>' . esc_html__( 'Önerilen Gereksinimler', 'steamlike' ) . '</h4>';
		$this->render_fields( $post, array( 'recommended_os', 'recommended_cpu', 'recommended_ram', 'recommended_gpu', 'recommended_storage' ) );
	}
	public function render_download_meta( $post ) { $this->render_fields( $post, array( 'game_download_url', 'game_download_mirror', 'game_download_password', 'game_installation_guide' ) ); }
	public function render_media_meta( $post ) { $this->render_fields( $post, array( 'game_trailer_url', 'game_background_image', 'game_screenshots' ) ); }
	public function render_stats_meta( $post ) { $this->render_fields( $post, array( 'game_multiplayer_support', 'game_controller_support', 'game_featured', 'game_editors_choice', 'game_rating' ) ); }

	public function save_game_meta_boxes( $post_id ) {
		if ( ! isset( $_POST['sl_game_meta_nonce'] ) || ! wp_verify_nonce( $_POST['sl_game_meta_nonce'], 'sl_save_game_meta' ) ) return;
		if ( defined( 'DOING_AUTOSAVE' ) && DOING_AUTOSAVE ) return;
		if ( ! current_user_can( 'edit_post', $post_id ) ) return;
		$all_fields = $this->get_meta_fields();
		foreach ( $all_fields as $key => $field ) {
			if ( $field['type'] === 'checkbox' ) { update_post_meta( $post_id, $key, isset( $_POST[ $key ] ) ? '1' : '0' ); } 
			else { if ( isset( $_POST[ $key ] ) ) { update_post_meta( $post_id, $key, wp_unslash( $_POST[ $key ] ) ); } }
		}
	}
}
