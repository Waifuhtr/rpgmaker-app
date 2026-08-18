<?php
/**
 * SteamLike Theme Options
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit; // Direkt erişimi engelle
}

class SteamLike_Theme_Options {

	private $options;

	public function __construct() {
		add_action( 'admin_menu', array( $this, 'add_theme_page' ) );
		add_action( 'admin_init', array( $this, 'page_init' ) );
	}

	// Admin menüsüne "Tema Ayarları" sekmesini ekler
	public function add_theme_page() {
		add_menu_page(
			__( 'SteamLike Ayarları', 'steamlike' ), // Sayfa başlığı
			__( 'Tema Ayarları', 'steamlike' ),      // Menü başlığı
			'manage_options',                        // Yetki
			'steamlike-options',                     // Menü slug
			array( $this, 'create_admin_page' ),     // Çıktı fonksiyonu
			'dashicons-desktop',                     // İkon
			59                                       // Pozisyon
		);
	}

	// Ayar sayfasının HTML yapısı
	public function create_admin_page() {
		$this->options = get_option( 'steamlike_settings' );
		?>
		<div class="wrap">
			<h1><?php esc_html_e( 'SteamLike Tema Ayarları', 'steamlike' ); ?></h1>
			<form method="post" action="options.php">
			<?php
				// Ayar grubunu çıktıla ve güvenlik nonce'larını ekle
				settings_fields( 'steamlike_option_group' );
				// Kayıtlı alanları ekrana bas
				do_settings_sections( 'steamlike-options' );
				// Kaydet butonu
				submit_button();
			?>
			</form>
		</div>
		<?php
	}

	// Alanları WordPress Settings API'ye kaydeder
	public function page_init() {
		register_setting(
			'steamlike_option_group', // Ayar grubu
			'steamlike_settings',     // Veritabanı option_name
			array( $this, 'sanitize' ) // Temizleme fonksiyonu
		);

		// 1. Kısım: Sosyal Medya ve Topluluk
		add_settings_section(
			'setting_section_social', // ID
			__( 'Topluluk ve Sosyal Medya', 'steamlike' ), // Başlık
			array( $this, 'print_section_info' ), // Açıklama fonksiyonu
			'steamlike-options' // Sayfa slug
		);

		add_settings_field(
			'discord_url', 
			__( 'Discord Sunucu Bağlantısı', 'steamlike' ), 
			array( $this, 'url_callback' ), 
			'steamlike-options', 
			'setting_section_social',
			array( 'id' => 'discord_url' )
		);

		add_settings_field(
			'youtube_url', 
			__( 'YouTube Kanal Bağlantısı', 'steamlike' ), 
			array( $this, 'url_callback' ), 
			'steamlike-options', 
			'setting_section_social',
			array( 'id' => 'youtube_url' )
		);

		// 2. Kısım: Genel ve Tasarım Ayarları
		add_settings_section(
			'setting_section_general',
			__( 'Genel Ayarlar', 'steamlike' ),
			null,
			'steamlike-options'
		);

		add_settings_field(
			'footer_text', 
			__( 'Footer Telif Metni', 'steamlike' ), 
			array( $this, 'textarea_callback' ), 
			'steamlike-options', 
			'setting_section_general',
			array( 'id' => 'footer_text' )
		);
	}

	// Güvenlik ve temizleme işlemi
	public function sanitize( $input ) {
		$sanitized_input = array();
		
		if ( isset( $input['discord_url'] ) ) {
			$sanitized_input['discord_url'] = esc_url_raw( $input['discord_url'] );
		}
		if ( isset( $input['youtube_url'] ) ) {
			$sanitized_input['youtube_url'] = esc_url_raw( $input['youtube_url'] );
		}
		if ( isset( $input['footer_text'] ) ) {
			$sanitized_input['footer_text'] = sanitize_text_field( $input['footer_text'] );
		}

		return $sanitized_input;
	}

	// Kısım açıklaması
	public function print_section_info() {
		echo '<p>' . esc_html__( 'Oyun topluluğunuzun bağlantılarını buradan yönetebilirsiniz.', 'steamlike' ) . '</p>';
	}

	// URL Input Çıktısı
	public function url_callback( $args ) {
		$id = $args['id'];
		$val = isset( $this->options[$id] ) ? esc_url( $this->options[$id] ) : '';
		printf(
			'<input type="url" id="%1$s" name="steamlike_settings[%1$s]" value="%2$s" class="regular-text" />',
			esc_attr( $id ),
			$val
		);
	}

	// Textarea Çıktısı
	public function textarea_callback( $args ) {
		$id = $args['id'];
		$val = isset( $this->options[$id] ) ? esc_textarea( $this->options[$id] ) : '';
		printf(
			'<textarea id="%1$s" name="steamlike_settings[%1$s]" rows="4" cols="50">%2$s</textarea>',
			esc_attr( $id ),
			$val
		);
		echo '<p class="description">' . esc_html__( 'Telif hakkı ve kısa açıklamalarınızı buraya girebilirsiniz.', 'steamlike' ) . '</p>';
	}
}
