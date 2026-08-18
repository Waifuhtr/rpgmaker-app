<?php
/**
 * SteamLike functions and definitions
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit; // Direkt erişimi engelle
}

define( 'STEAMLIKE_VERSION', '2.1.0' );
define( 'STEAMLIKE_DIR', trailingslashit( get_template_directory() ) );
define( 'STEAMLIKE_URI', trailingslashit( get_template_directory_uri() ) );

require_once STEAMLIKE_DIR . 'inc/setup.php';
require_once STEAMLIKE_DIR . 'inc/enqueue.php';
require_once STEAMLIKE_DIR . 'inc/custom-post-types.php';
require_once STEAMLIKE_DIR . 'inc/taxonomies.php';
require_once STEAMLIKE_DIR . 'inc/meta-boxes.php';
require_once STEAMLIKE_DIR . 'inc/theme-options.php';
require_once STEAMLIKE_DIR . 'inc/taxonomy-meta.php';
require_once STEAMLIKE_DIR . 'inc/ajax.php';
require_once STEAMLIKE_DIR . 'inc/ratings.php';
require_once STEAMLIKE_DIR . 'inc/favorites.php';
require_once STEAMLIKE_DIR . 'inc/view-counter.php';
require_once STEAMLIKE_DIR . 'inc/breadcrumbs.php';
require_once STEAMLIKE_DIR . 'inc/reviews-api.php';
require_once STEAMLIKE_DIR . 'inc/badges.php';
require_once STEAMLIKE_DIR . 'inc/webp-converter.php';
require_once STEAMLIKE_DIR . 'inc/seo-extra.php';

final class SteamLike_Theme {
	private static $instance = null;
	public static function get_instance() {
		if ( null === self::$instance ) {
			self::$instance = new self();
		}
		return self::$instance;
	}
	private function __construct() {
		$this->init_hooks();
	}
	private function init_hooks() {
		new SteamLike_Setup();
		new SteamLike_Enqueue();
		new SteamLike_CPT();
		new SteamLike_Taxonomies();
		new SteamLike_Meta_Boxes();
		new SteamLike_Theme_Options();
		new SteamLike_Taxonomy_Meta();
		new SteamLike_AJAX();
		new SteamLike_Ratings();
		new SteamLike_Favorites();
		new SteamLike_Counters();
	}
}

function steamlike() {
	return SteamLike_Theme::get_instance();
}
steamlike();

// ==========================================
// OYUN SAYFASI (META) AYARLARI
// ==========================================
add_action( 'customize_register', 'sl_theme_customizer_settings' );
function sl_theme_customizer_settings( $wp_customize ) {
	$wp_customize->add_section( 'sl_game_settings', array(
		'title'       => __( 'Oyun Sayfası Ayarları', 'steamlike' ),
		'priority'    => 30,
	) );
	$wp_customize->add_setting( 'sl_meta_alignment', array( 'default' => 'left', 'sanitize_callback' => 'sanitize_text_field' ) );
	$wp_customize->add_control( 'sl_meta_alignment', array(
		'label'   => __( 'Meta Alanları Hizalaması (Geliştirici, Boyut vb.)', 'steamlike' ),
		'section' => 'sl_game_settings', 
		'type'    => 'select',
		'choices' => array( 'left' => 'Sola Yasla', 'center' => 'Ortala', 'right' => 'Sağa Yasla' ),
	) );
}

// CSS Dosyası için Dinamik Cache-Busting (Önbellek Kırıcı)
add_action( 'wp_enqueue_scripts', 'sl_dynamic_assets_enqueue', 99 );
function sl_dynamic_assets_enqueue() {
    $css_path = get_template_directory() . '/assets/css/main.css';
    $css_version = file_exists( $css_path ) ? filemtime( $css_path ) : '1.0';
    wp_enqueue_style( 'steamlike-main-style', get_template_directory_uri() . '/assets/css/main.css', array(), $css_version );
}

// Rapor Motorunu Sisteme Tanıt
require_once STEAMLIKE_DIR . 'inc/reports.php';

// ==========================================
// TEMA ÖZELLEŞTİRİCİ (RENK VE ANA SAYFA KART TASARIMI)
// ==========================================
add_action( 'customize_register', 'sl_advanced_theme_customizer' );
function sl_advanced_theme_customizer( $wp_customize ) {
	
	// 1. Renk Ayarları Bölümü
	$wp_customize->add_section( 'sl_colors_section', array( 'title' => __( 'Tema Renkleri', 'steamlike' ), 'priority' => 31 ) );
	
	$wp_customize->add_setting( 'sl_main_color', array( 'default' => '#22d3ee', 'sanitize_callback' => 'sanitize_hex_color' ) );
	$wp_customize->add_control( new WP_Customize_Color_Control( $wp_customize, 'sl_main_color', array( 'label' => __( 'Ana Renk (Butonlar, Vurgular)', 'steamlike' ), 'section' => 'sl_colors_section' ) ) );
	
	$wp_customize->add_setting( 'sl_sec_color', array( 'default' => '#67e8f9', 'sanitize_callback' => 'sanitize_hex_color' ) );
	$wp_customize->add_control( new WP_Customize_Color_Control( $wp_customize, 'sl_sec_color', array( 'label' => __( 'İkincil Renk (Hover, Linkler)', 'steamlike' ), 'section' => 'sl_colors_section' ) ) );

	// 2. Kart Tasarımı Bölümü (SAĞLAM DURUYOR)
	$wp_customize->add_section( 'sl_design_section', array( 'title' => __( 'Oyun Kartı Tasarımı', 'steamlike' ), 'priority' => 32 ) );
	$wp_customize->add_setting( 'sl_card_style', array( 'default' => 'default', 'sanitize_callback' => 'sanitize_text_field' ) );
	
	$wp_customize->add_control( 'sl_card_style', array(
		'label'   => __( 'Ana Sayfa: Kart Tasarımını Seç', 'steamlike' ),
		'section' => 'sl_design_section',
		'type'    => 'select',
		'choices' => array(
			'default'      => __( 'Varsayılan Tasarım (Mevcut)', 'steamlike' ),
			'minimal'      => __( 'Tasarım 2: Minimalist (Görsel Odaklı)', 'steamlike' ),
			'horizontal'   => __( 'Tasarım 3: Yatay Liste (Geniş Ekran)', 'steamlike' ),
			'compact'      => __( 'Tasarım 4: Kompakt (Sıkıştırılmış)', 'steamlike' ),
            'modern_glass' => __( 'Tasarım 5: Modern Glass', 'steamlike' )
		),
	) );

	// 3. Sayfa Şablonları Bölümü (Ana Sayfa & Oyun Detay Sayfası - Baştan Aşağı Tasarımlar)
	$wp_customize->add_section( 'sl_page_templates_section', array( 'title' => __( 'Sayfa Şablonları', 'steamlike' ), 'priority' => 33 ) );

	$sl_page_style_choices = array(
		'default'  => __( 'Varsayılan Tasarım (Mevcut)', 'steamlike' ),
		'playstore' => __( 'Play Store Tarzı', 'steamlike' ),
		'itchio'   => __( 'itch.io Tarzı', 'steamlike' ),
		'holo'     => __( 'Holo-Terminal (İmza Tasarım)', 'steamlike' ),
	);

	$wp_customize->add_setting( 'sl_frontpage_style', array( 'default' => 'default', 'sanitize_callback' => 'sanitize_text_field' ) );
	$wp_customize->add_control( 'sl_frontpage_style', array(
		'label'       => __( 'Ana Sayfa: Tasarım Şablonu Seç', 'steamlike' ),
		'description' => __( 'Ana sayfanın tamamen farklı bir düzenle görüntülenmesini sağlar.', 'steamlike' ),
		'section'     => 'sl_page_templates_section',
		'type'        => 'select',
		'choices'     => $sl_page_style_choices,
	) );

	$wp_customize->add_setting( 'sl_single_game_style', array( 'default' => 'default', 'sanitize_callback' => 'sanitize_text_field' ) );
	$wp_customize->add_control( 'sl_single_game_style', array(
		'label'       => __( 'Oyun Detay Sayfası: Tasarım Şablonu Seç', 'steamlike' ),
		'description' => __( 'Oyun/mod detay sayfasının tamamen farklı bir düzenle görüntülenmesini sağlar. Tüm veriler (sürüm, platform, dil, boyut vb.) korunur.', 'steamlike' ),
		'section'     => 'sl_page_templates_section',
		'type'        => 'select',
		'choices'     => $sl_page_style_choices,
	) );
}

// Seçilen Renkleri Siteye Uygulama
add_action( 'wp_head', 'sl_customizer_dynamic_css', 100 );
function sl_customizer_dynamic_css() {
	$main_color = get_theme_mod( 'sl_main_color', '#22d3ee' );
	$sec_color  = get_theme_mod( 'sl_sec_color', '#67e8f9' );
	echo "<style> :root { --sl-color-blue-100: {$main_color}; --sl-color-blue-200: {$sec_color}; } </style>";
}

// ==========================================
// ÖZELLEŞTİRİCİ SIFIRLAMA (RESET) KONTROLÜ
// ==========================================
add_action( 'customize_register', 'sl_customizer_reset_option' );
function sl_customizer_reset_option( $wp_customize ) {
	$wp_customize->add_setting( 'sl_reset_theme_options', array( 'default' => false, 'transport' => 'postMessage' ) );
	$wp_customize->add_control( 'sl_reset_theme_options', array(
		'label'       => __( 'Tüm Tasarım Ayarlarını Sıfırla', 'steamlike' ),
		'description' => __( 'Eğer yaptığınız renk ve kart tasarımı değişikliklerini sevmediyseniz, bu kutuyu işaretleyip "Yayımla" butonuna basın.', 'steamlike' ),
		'section'     => 'sl_design_section',
		'type'        => 'checkbox',
	) );
}

// Sıfırlama Kutusu İşaretlendiyse Veritabanını Temizle
add_action( 'customize_save_after', 'sl_process_customizer_reset' );
function sl_process_customizer_reset( $manager ) {
	if ( $manager->get_setting( 'sl_reset_theme_options' )->post_value() ) {
		remove_theme_mod( 'sl_main_color' );
		remove_theme_mod( 'sl_sec_color' );
		remove_theme_mod( 'sl_card_style' );
		remove_theme_mod( 'sl_meta_alignment' );
		remove_theme_mod( 'sl_frontpage_style' );
		remove_theme_mod( 'sl_single_game_style' );
		set_theme_mod( 'sl_reset_theme_options', false ); 
	}
}

// ==========================================
// ADMİN PANELİ: SÜRE SENKRONİZASYONU
// ==========================================
add_action( 'admin_menu', 'sl_admin_sync_menu' );
function sl_admin_sync_menu() {
	add_management_page( 'Süre Senkronizasyonu', 'Oyun Süreleri Senk.', 'manage_options', 'sl-playtime-sync', 'sl_admin_sync_page' );
}

function sl_admin_sync_page() {
	echo '<div class="wrap"><h1>Oyun Süreleri Senkronizasyonu (Tüm Kullanıcılar)</h1>';
	
	if ( isset($_POST['sl_run_global_sync']) && current_user_can('manage_options') ) {
		$users = get_users();
		$synced_count = 0;
        $game_totals = array();
        
		foreach ( $users as $user ) {
			$played = get_user_meta( $user->ID, 'sl_played_games', true );
			if ( ! empty( $played ) && is_array( $played ) ) {
				foreach ( $played as $pid ) { 
                    $new_playtime = SteamLike_Reviews::get_playtime( $user->ID, $pid ); 
                    
                    if ( $new_playtime ) {
                        $minutes = 0;
                        $time_str = strtolower($new_playtime);
                        if (strpos($time_str, 'saat') !== false) {
                            preg_match('/([0-9\.]+)\s*saat/', $time_str, $m);
                            if(isset($m[1])) $minutes += (float)$m[1] * 60;
                        } elseif (strpos($time_str, 'dakika') !== false || strpos($time_str, 'dk') !== false) {
                            preg_match('/([0-9]+)\s*(dakika|dk)/', $time_str, $m);
                            if(isset($m[1])) $minutes += (int)$m[1];
                        } else {
                            $minutes += (int)$time_str;
                        }

                        if (!isset($game_totals[$pid])) $game_totals[$pid] = 0;
                        $game_totals[$pid] += $minutes;

                        $user_reviews = get_comments( array('post_id' => $pid, 'user_id' => $user->ID, 'type' => 'review') );
                        foreach ( $user_reviews as $review ) {
                            update_comment_meta( $review->comment_ID, 'sl_playtime', $new_playtime );
                        }
                    }
                }
				$synced_count++;
			}
		}

        foreach ($game_totals as $game_id => $total_minutes) {
            update_post_meta($game_id, 'sl_total_playtime', $total_minutes);
        }

		echo '<div class="notice notice-success is-dismissible"><p><strong>Başarılı!</strong> ' . $synced_count . ' kullanıcının verileri işlendi.</p></div>';
	}

	echo '<div class="card" style="max-width: 600px; padding: 20px; margin-top:20px;">
			<h2>Toplu Senkronizasyon Başlat</h2>
			<form method="post"><input type="hidden" name="sl_run_global_sync" value="1">';
	submit_button( 'Tüm Süreleri Senkronize Et', 'primary' );
	echo '</form></div></div>';
}

// ==========================================
// STEAMLIKE FOOTER AYARLARI (ÖZELLEŞTİRİCİ)
// ==========================================
if ( ! function_exists( 'sl_customize_register' ) ) {
    function sl_customize_register( $wp_customize ) {
        $wp_customize->add_section( 'sl_footer_section' , array(
            'title'      => __( 'SteamLike Footer Ayarları', 'steamlike' ),
            'priority'   => 120,
        ) );

        $wp_customize->add_setting( 'sl_footer_text' , array(
            'default'   => '&copy; 2024 SteamLike. Proudly powered by WordPress.',
            'transport' => 'refresh',
        ) );

        $wp_customize->add_control( 'sl_footer_text_control', array(
            'label'      => __( 'Footer Metni (HTML ve Kısa Kod destekler)', 'steamlike' ),
            'section'    => 'sl_footer_section',
            'settings'   => 'sl_footer_text',
            'type'       => 'textarea',
        ) );
    }
    add_action( 'customize_register', 'sl_customize_register' );
}

// ==========================================
// GİRİŞ YAPMAYAN KULLANICILAR İÇİN İKONLARI (DASHICONS) AKTİF ETME
// ==========================================
if ( ! function_exists( 'sl_load_dashicons_front_end' ) ) {
    add_action( 'wp_enqueue_scripts', 'sl_load_dashicons_front_end' );
    function sl_load_dashicons_front_end() {
        wp_enqueue_style( 'dashicons' );
    }
}

// ==========================================
// İNCELEME OYLAMA SİSTEMİ (VERİTABANI KAYIT İŞLEMİ)
// ==========================================
if ( ! function_exists( 'sl_handle_review_vote' ) ) {
    add_action( 'wp_ajax_sl_vote_review', 'sl_handle_review_vote' );
    add_action( 'wp_ajax_nopriv_sl_vote_review', 'sl_handle_review_vote' ); 
    function sl_handle_review_vote() {
        $comment_id = isset( $_POST['comment_id'] ) ? intval( $_POST['comment_id'] ) : 0;
        $type = isset( $_POST['type'] ) ? sanitize_text_field( $_POST['type'] ) : '';

        if ( empty( $comment_id ) || ! in_array( $type, array( 'up', 'down' ) ) ) {
            wp_send_json_error( 'Geçersiz işlem veya ID eksik.' );
            wp_die();
        }

        if ( isset( $_COOKIE['sl_voted_review_' . $comment_id] ) ) {
            wp_send_json_error( 'Bu içerik için zaten oy kullandınız.' );
            wp_die();
        }

        $meta_key = ( $type === 'up' ) ? 'sl_upvotes' : 'sl_downvotes';
        $current_votes = intval( get_comment_meta( $comment_id, $meta_key, true ) );
        $new_votes = $current_votes + 1;
        
        update_comment_meta( $comment_id, $meta_key, $new_votes );

        wp_send_json_success( array( 'new_count' => $new_votes ) );
        wp_die(); 
    }
}

// ==========================================
// İNCELEME OYLAMA ANİMASYONU (JS)
// ==========================================
if ( ! function_exists( 'sl_review_voting_script' ) ) {
    add_action( 'wp_footer', 'sl_review_voting_script' );
    function sl_review_voting_script() {
        ?>
        <script>
        jQuery(document).ready(function($) {
            $('.sl-vote-btn-small').on('click', function(e) {
                e.preventDefault();
                var btn = $(this);
                if( btn.is(':disabled') ) return;

                var comment_id = btn.attr('data-id'); 
                var vote_type = btn.attr('data-type');
                var voting_container = btn.closest('.sl-review-voting-bar');

                $.ajax({
                    url: '<?php echo admin_url("admin-ajax.php"); ?>',
                    type: 'POST',
                    dataType: 'json',
                    data: { action: 'sl_vote_review', comment_id: comment_id, type: vote_type },
                    beforeSend: function() { btn.css('opacity', '0.5'); },
                    success: function(response) {
                        if(response && response.success) {
                            btn.find('.sl-vote-count-' + vote_type + '-small').text(response.data.new_count);
                            voting_container.find('.sl-vote-btn-small').prop('disabled', true);
                            voting_container.addClass('voted').css('opacity', '0.5');
                            voting_container.find('.sl-vote-text-small').text('Değerlendirme yapıldı.');
                            document.cookie = "sl_voted_review_" + comment_id + "=1; max-age=" + (30*24*60*60) + "; path=/";
                        } else {
                            alert('İşlem başarısız oldu.'); 
                            btn.css('opacity', '1');
                        }
                    }
                });
            });
        });
        </script>
        <?php
    }
}

// ==========================================
// KULLANICILAR İÇİN ADMİN BAR GİZLEME
// ==========================================
add_action('after_setup_theme', 'sl_remove_admin_bar');
function sl_remove_admin_bar() {
    if (!current_user_can('administrator') && !is_admin()) {
        show_admin_bar(false);
    }
}

// ==========================================
// BİLDİRİM SİSTEMİ (AJAX İLE OKUNDU İŞARETLEME)
// ==========================================
if ( ! function_exists( 'sl_mark_notifications_read_handler' ) ) {
    add_action( 'wp_ajax_sl_mark_notifications_read', 'sl_mark_notifications_read_handler' );
    function sl_mark_notifications_read_handler() {
        if ( is_user_logged_in() ) {
            $user_id = get_current_user_id();
            update_user_meta( $user_id, 'sl_notifications_last_read', current_time('timestamp') );
            wp_send_json_success();
        }
        wp_send_json_error();
    }
}

// ==========================================
// 1. STEAMLIKE SEO: ÇÖP SAYFALARI İNDEKSE KAPATMA (NOINDEX)
// ==========================================
add_action( 'wp_head', 'sl_clean_seo_robots', 1 );
function sl_clean_seo_robots() {
    if ( is_search() || is_tag() || is_author() || is_404() || is_date() ) {
        echo '<meta name="robots" content="noindex, follow">' . "\n";
    } elseif ( is_singular('game') || is_front_page() || is_page() ) {
        echo '<meta name="robots" content="index, follow, max-image-preview:large">' . "\n";
    }
}

// ==========================================
// 2. STEAMLIKE SEO: GOOGLE JSON-LD (YAZILIM/OYUN ŞEMASI)
// ==========================================
add_action( 'wp_head', 'sl_native_seo_tags', 2 );
function sl_native_seo_tags() {
    if ( is_singular( 'game' ) ) {
        global $post;
        $post_id = $post->ID;
        
        $title = get_the_title() . ' İndir - ' . get_bloginfo('name');
        $desc = wp_trim_words( get_the_excerpt(), 20, '...' );
        $url = get_permalink();
        $image = get_the_post_thumbnail_url( $post_id, 'full' ) ?: STEAMLIKE_URI . 'assets/images/placeholder.jpg';
        $rating = get_post_meta( $post_id, 'sl_user_rating_avg', true ) ?: '5.0';
        $rating_count = get_post_meta( $post_id, 'sl_user_rating_count', true ) ?: '1';
        $platform = strip_tags( get_the_term_list( $post_id, 'game_platform', '', ', ' ) ) ?: 'Android/PC';
        
        echo '<meta property="og:title" content="' . esc_attr($title) . '">' . "\n";
        echo '<meta property="og:description" content="' . esc_attr($desc) . '">' . "\n";
        echo '<meta property="og:url" content="' . esc_url($url) . '">' . "\n";
        echo '<meta property="og:image" content="' . esc_url($image) . '">' . "\n";
        echo '<meta property="og:type" content="article">' . "\n";
        echo '<meta name="twitter:card" content="summary_large_image">' . "\n";
        
        $schema = array(
            "@context" => "https://schema.org",
            "@type" => "SoftwareApplication",
            "name" => get_the_title(),
            "operatingSystem" => $platform,
            "applicationCategory" => "GameApplication",
            "image" => $image,
            "url" => $url,
            "description" => $desc,
            "aggregateRating" => array(
                "@type" => "AggregateRating",
                "ratingValue" => $rating,
                "ratingCount" => $rating_count,
                "bestRating" => "5",
                "worstRating" => "1"
            ),
            "offers" => array(
                "@type" => "Offer",
                "price" => "0.00",
                "priceCurrency" => "USD"
            )
        );
        
        echo '<script type="application/ld+json">' . wp_json_encode($schema, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) . '</script>' . "\n";
    }
}

// ==========================================
// 3. STEAMLIKE SEO: DİNAMİK VE TEMİZ SAYFA BAŞLIKLARI
// ==========================================
add_filter( 'document_title_parts', 'sl_custom_seo_titles' );
function sl_custom_seo_titles( $title ) {
    if ( is_singular( 'game' ) ) {
        $version = get_post_meta( get_the_ID(), 'game_version', true );
        $ver_text = $version ? ' v' . $version : '';
        $title['title'] = get_the_title() . $ver_text . ' İndir';
    } elseif ( is_post_type_archive( 'game' ) ) {
        $title['title'] = 'Oyun Kütüphanesi ve Arşivi';
    }
    return $title;
}

// ==========================================
// 4. STEAMLIKE SEO & HIZ: LCP GÖRSELİNİ ÖNCEDEN YÜKLEME
// ==========================================
add_action( 'wp_head', 'sl_preload_lcp_image', 1 );
function sl_preload_lcp_image() {
    if ( is_singular( 'game' ) ) {
        $bg_image = get_post_meta( get_the_ID(), 'game_background_image', true ) ?: get_the_post_thumbnail_url( get_the_ID(), 'full' );
        if ( $bg_image ) {
            echo '<link rel="preload" as="image" href="' . esc_url( $bg_image ) . '">' . "\n";
        }
    }
}

// ==========================================
// 5. STEAMLIKE GÜVENLİ OPTİMİZASYON PANELİ
// ==========================================
add_action('admin_menu', 'sl_optimization_menu');
function sl_optimization_menu() {
    add_options_page('Tema Optimizasyonu', 'SteamLike Optimizasyon', 'manage_options', 'sl-optimization', 'sl_optimization_page');
}

add_action('admin_init', 'sl_register_optimization_settings');
function sl_register_optimization_settings() {
    $settings = array('sl_opt_lazyload', 'sl_opt_emojis', 'sl_opt_querystrings', 'sl_opt_font_preload');
    foreach($settings as $s) register_setting('sl_opt_group', $s);
}

function sl_optimization_page() {
    ?>
    <div class="wrap">
        <h1><span class="dashicons dashicons-performance" style="color:#3b82f6;"></span> SteamLike Optimizasyon Paneli</h1>
        <p>Aşağıdaki ayarlar temanın tasarımını ve JavaScript fonksiyonlarını (butonlar, menüler) ASLA bozmaz.</p>
        <div style="background:#fff; padding:20px; border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,0.05); max-width: 600px;">
            <form method="post" action="options.php">
                <?php settings_fields('sl_opt_group'); ?>
                <table class="form-table">
                    <tr><td><label><input type="checkbox" name="sl_opt_lazyload" value="1" <?php checked(get_option('sl_opt_lazyload'), 1); ?> /> Görsel Tembel Yükleme (Lazy Load)</label></td></tr>
                    <tr><td><label><input type="checkbox" name="sl_opt_font_preload" value="1" <?php checked(get_option('sl_opt_font_preload'), 1); ?> /> Google Font Ön Yükleme</label></td></tr>
                    <tr><td><label><input type="checkbox" name="sl_opt_emojis" value="1" <?php checked(get_option('sl_opt_emojis'), 1); ?> /> Emojileri Devre Dışı Bırak</label></td></tr>
                    <tr><td><label><input type="checkbox" name="sl_opt_querystrings" value="1" <?php checked(get_option('sl_opt_querystrings'), 1); ?> /> Sürüm Parametrelerini Sil (?ver=)</label></td></tr>
                </table>
                <?php submit_button('Ayarları Uygula', 'primary'); ?>
            </form>
        </div>
    </div>
    <?php
}

// Güvenli Optimizasyon Motorları
if (get_option('sl_opt_lazyload') == 1) {
    add_action('wp_footer', 'sl_safe_native_lazy_load', 99);
    function sl_safe_native_lazy_load() {
        echo "<script>document.addEventListener('DOMContentLoaded', function() { var images = document.querySelectorAll('img:not([loading])'); images.forEach(function(img) { img.setAttribute('loading', 'lazy'); }); });</script>";
    }
}
if (get_option('sl_opt_font_preload') == 1) {
    add_action('wp_head', 'sl_preload_google_fonts', 1);
    function sl_preload_google_fonts() {
        echo '<link rel="preconnect" href="https://fonts.googleapis.com">' . "\n";
        echo '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>' . "\n";
    }
}
if (get_option('sl_opt_emojis') == 1) {
    remove_action('wp_head', 'print_emoji_detection_script', 7);
    remove_action('wp_print_styles', 'print_emoji_styles');
}
if (get_option('sl_opt_querystrings') == 1) {
    function sl_remove_css_js_version($src) {
        if (strpos($src, '?ver=')) { $src = remove_query_arg('ver', $src); }
        return $src;
    }
    add_filter('style_loader_src', 'sl_remove_css_js_version', 10, 2);
    add_filter('script_loader_src', 'sl_remove_css_js_version', 10, 2);
}

// ==========================================
// 6. STEAMLIKE SEO: SİTE HARİTASI (SITEMAP) TEMİZLİĞİ
// ==========================================
add_filter( 'wp_sitemaps_taxonomies', 'sl_clean_sitemap_taxonomies' );
function sl_clean_sitemap_taxonomies( $taxonomies ) {
    if ( isset( $taxonomies['post_tag'] ) ) unset( $taxonomies['post_tag'] );
    return $taxonomies;
}

add_filter( 'wp_sitemaps_add_provider', 'sl_clean_sitemap_users', 10, 2 );
function sl_clean_sitemap_users( $provider, $name ) {
    if ( 'users' === $name ) return false;
    return $provider;
}

// ==========================================
// 7. STEAMLIKE ŞİFRE SIFIRLAMA OTOMASYONU (AJAX)
// ==========================================
add_action( 'wp_ajax_nopriv_sl_ajax_forgot_password', 'sl_ajax_forgot_password_handler' );
function sl_ajax_forgot_password_handler() {
    check_ajax_referer( 'sl_forgot_nonce', 'security' );

    $login_data = sanitize_text_field( $_POST['user_login'] );
    
    if ( empty( $login_data ) ) {
        wp_send_json_error( 'Lütfen e-posta adresinizi veya kullanıcı adınızı girin.' );
    }

    $user_data = get_user_by( 'email', $login_data );
    if ( ! $user_data ) {
        $user_data = get_user_by( 'login', $login_data );
    }

    if ( ! $user_data ) {
        wp_send_json_error( 'Bu bilgilere ait bir sistem hesabı bulunamadı.' );
    }

    $reset_key = get_password_reset_key( $user_data );
    if ( is_wp_error( $reset_key ) ) {
        wp_send_json_error( 'Anahtar üretilirken bir hata oluştu. Lütfen tekrar deneyin.' );
    }

    $reset_link = home_url( "/sifre-sifirla/?key=" . $reset_key . "&login=" . rawurlencode( $user_data->user_login ) );

    $site_name = get_bloginfo( 'name' );
    $subject = "[$site_name] Şifre Sıfırlama Talebi";
    $message = "Merhaba,\n\n";
    $message .= "Hesabınız için şifre sıfırlama talebinde bulundunuz.\n\n";
    $message .= "Yeni şifrenizi belirlemek için aşağıdaki bağlantıya tıklayın:\n";
    $message .= $reset_link . "\n\n";
    $message .= "Eğer bu talebi siz yapmadıysanız, bu e-postayı güvenle görmezden gelebilirsiniz.\n";

    $mail_sent = wp_mail( $user_data->user_email, $subject, $message );

    if ( $mail_sent ) {
        wp_send_json_success( 'Sıfırlama bağlantısı e-posta adresinize gönderildi. (Gereksiz kutusunu kontrol etmeyi unutmayın)' );
    } else {
        wp_send_json_error( 'E-posta gönderilemedi. Lütfen site yöneticisiyle iletişime geçin.' );
    }
}

// ==========================================
// 8. ÖZEL ŞİFRE SIFIRLAMA SAYFASI (SHORTCODE)
// ==========================================
add_shortcode('sl_password_reset', 'sl_password_reset_page_handler');
function sl_password_reset_page_handler() {
    if ( is_user_logged_in() ) return 'Zaten giriş yapmış durumdasınız.';

    $key = isset($_GET['key']) ? sanitize_text_field($_GET['key']) : '';
    $login = isset($_GET['login']) ? sanitize_text_field($_GET['login']) : '';

    $user = check_password_reset_key($key, $login);

    if ( is_wp_error($user) ) {
        return '<div class="sl-content-box" style="text-align:center; padding:40px;">
                    <h3 style="color:#ef4444;">Geçersiz veya Süresi Dolmuş Bağlantı</h3>
                    <p>Şifre sıfırlama bağlantısının süresi dolmuş olabilir. Lütfen tekrar talep edin.</p>
                    <a href="'.home_url().'" class="sl-btn sl-btn-primary">Ana Sayfaya Dön</a>
                </div>';
    }

    if ( isset($_POST['sl_new_password']) ) {
        if ( $_POST['sl_new_password'] === $_POST['sl_new_password_confirm'] ) {
            reset_password($user, $_POST['sl_new_password']);
            return '<div class="sl-content-box" style="text-align:center; padding:40px;">
                        <h3 style="color:#10b981;">Şifreniz Başarıyla Değiştirildi!</h3>
                        <p>Artık yeni şifrenizle giriş yapabilirsiniz.</p>
                        <button class="sl-btn sl-btn-primary" id="sl-open-auth-modal">Giriş Yap</button>
                    </div>';
        } else {
            echo '<p style="color:#ef4444; text-align:center;">Şifreler birbiriyle eşleşmiyor!</p>';
        }
    }

    ob_start(); ?>
    <div class="sl-content-box" style="max-width:500px; margin:0 auto; padding:40px;">
        <h3 style="color:var(--sl-color-blue-100); text-align:center;">Merhaba, <?php echo esc_html($login); ?>!</h3>
        <p style="text-align:center; color:#94a3b8; font-size:14px;">Şifrenizi değiştirmek mi istiyorsunuz? Aşağıdaki kutucuklardan yeni şifrenizi belirleyebilirsiniz.</p>
        
        <form method="post" class="sl-form" style="margin-top:20px;">
            <label>Yeni Şifre</label>
            <input type="password" name="sl_new_password" placeholder="••••••••" required>
            <label>Yeni Şifre (Tekrar)</label>
            <input type="password" name="sl_new_password_confirm" placeholder="••••••••" required>
            <button type="submit" class="sl-btn sl-btn-primary sl-btn-block">Şifreyi Güncelle</button>
        </form>
    </div>
    <?php
    return ob_get_clean();
}

// ==========================================
// 9. YENİ KULLANICI HOŞ GELDİN OTOMASYONU
// ==========================================
add_action( 'user_register', 'sl_welcome_email_on_registration', 10, 1 );
function sl_welcome_email_on_registration( $user_id ) {
    $user_info = get_userdata( $user_id );
    $to = $user_info->user_email;
    $site_name = get_bloginfo( 'name' );

    $subject = "Riaslink Dünyasına Hoş Geldin, " . $user_info->display_name . "! ✨";
    
    $message = "Selam " . $user_info->display_name . ",\n\n";
    $message .= "Riaslink ailesine katıldığın için çok mutluyuz! Artık oyun sürelerini kaydedebilir ve topluluğumuza katılabilirsin.\n\n";
    $message .= "--- ÖNEMLİ BİLGİ ---\n";
    $message .= "Sitemizde çevirdiğimiz bazı özel oyunlar 'Anahtar Sistemi' ile açılmaktadır. Eğer oyunları nasıl aktif edeceğin veya anahtarlar hakkında sorun varsa seni bilgilendirme sayfamıza bekliyoruz.\n\n";
    $message .= "Anahtar Sistemi Hakkında Bilgi Al: https://riaslink.fun/bilgi \n\n";
    $message .= "Keyifli oyunlar dileriz!\n";
    $message .= "Riaslink Ekibi";

    wp_mail( $to, $subject, $message );
}

// ==========================================
// 10. ŞİFRE SIFIRLAMA LİNKİ SÜRESİ (15 DAKİKA)
// ==========================================
add_filter( 'password_reset_expiration', 'sl_custom_password_reset_expiration' );
function sl_custom_password_reset_expiration() {
    return 15 * MINUTE_IN_SECONDS; 
}

// ==========================================
// 11. STEAMLIKE TELEGRAM OTOMASYON BOTU V2 (GÖRSEL VE FORMAT FİX)
// ==========================================

// Menüye Ekleme
add_action('admin_menu', 'sl_telegram_menu');
function sl_telegram_menu() {
    add_options_page('Telegram Otomasyonu', 'Telegram Botu', 'manage_options', 'sl-telegram-bot', 'sl_telegram_page');
}

// Ayarları Kaydetme
add_action('admin_init', 'sl_telegram_settings');
function sl_telegram_settings() {
    register_setting('sl_telegram_group', 'sl_tg_bot_token');
    register_setting('sl_telegram_group', 'sl_tg_chat_id');
}

// Admin Paneli Arayüzü
function sl_telegram_page() {
    // Varsayılan ve senin istediğin şablonu buraya donduruyoruz (Admin panelinden değiştirilemez, kodun içinde sabit)
    $fixed_template = "🎮 **Oyun Adı: {title}**\n📦 **Dosya Boyutu: {size}**\n📱 **Platform: {platform}**\n🌍 **Dil: Türkçe**\n📎 Link: {link}\n\n**📖 Konusu:**\n> {excerpt}\n\n**🎯 Etiketler: {tags}**\n___\n**Link geçmeyi bilmiyorsan**\n**@kural34link**";
    ?>
    <div class="wrap">
        <h1><span class="dashicons dashicons-megaphone" style="color:#3b82f6; font-size:28px; width:28px; height:28px; margin-top:-2px;"></span> SteamLike Telegram Otomasyonu V2</h1>
        <p>Görseller artık otomatik olarak en üstte, metin formatları ve etiketler ise tam istediğiniz gibi.</p>
        
        <div style="display: grid; grid-template-columns: 1fr 350px; gap: 20px; margin-top:20px;">
            <div style="background:#fff; padding:20px; border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,0.05);">
                <form method="post" action="options.php">
                    <?php settings_fields('sl_telegram_group'); ?>
                    <table class="form-table">
                        <tr>
                            <th scope="row">Bot Token</th>
                            <td><input type="text" id="sl_tg_bot_token" name="sl_tg_bot_token" value="<?php echo esc_attr(get_option('sl_tg_bot_token')); ?>" class="regular-text" placeholder="API Token..." required /></td>
                        </tr>
                        <tr>
                            <th scope="row">Kanal ID (Chat ID)</th>
                            <td><input type="text" id="sl_tg_chat_id" name="sl_tg_chat_id" value="<?php echo esc_attr(get_option('sl_tg_chat_id')); ?>" class="regular-text" placeholder="@Riaslink veya ID numarası..." required /></td>
                        </tr>
                        <tr>
                            <th scope="row">Kullanılan Mesaj Şablonu</th>
                            <td>
                                <textarea readonly rows="12" style="width:100%; font-family:monospace; background:#f9f9f9;"><?php echo esc_textarea($fixed_template); ?></textarea>
                                <p class="description">Bu şablon senin isteğin üzerine kodun içinde sabitlenmiştir.</p>
                            </td>
                        </tr>
                    </table>
                    <?php submit_button('Ayarları Kaydet', 'primary'); ?>
                </form>
            </div>

            <div style="background:#0f172a; color:#fff; padding:20px; border-radius:12px; box-shadow:0 10px 30px rgba(0,0,0,0.3);">
                <h3 style="color:#60a5fa; margin-top:0;"><span class="dashicons dashicons-admin-network"></span> Bağlantı Testi & Log</h3>
                <p style="font-size:12px; color:#94a3b8;">Hata alırsanız, Telegram sunucusunun gönderdiği "Hata Nedeni" (Log) aşağıda belirecektir.</p>
                <button id="sl-test-tg-btn" class="button button-primary" style="width:100%; background:#10b981; border:none; height:40px; margin-top:10px;">GÖRSEL VE FORMATLI TEST MESAJI GÖNDER</button>
                <div id="sl-tg-msg" style="margin-top:15px; font-size:12px; font-weight:bold; text-align:center; display:none; padding:10px; border-radius:6px; background:rgba(0,0,0,0.5);"></div>
            </div>
        </div>
    </div>

    <script>
    jQuery(document).ready(function($) {
        $('#sl-test-tg-btn').on('click', function(e) {
            e.preventDefault();
            var btn = $(this);
            var msgBox = $('#sl-tg-msg');
            
            btn.prop('disabled', true).text('GÖNDERİLİYOR...');
            msgBox.hide().removeClass('success error');

            $.ajax({
                url: ajaxurl,
                type: 'POST',
                data: {
                    action: 'sl_test_telegram',
                    token: $('#sl_tg_bot_token').val(),
                    chat_id: $('#sl_tg_chat_id').val()
                },
                success: function(response) {
                    msgBox.show();
                    if(response.success) {
                        msgBox.css({'color': '#10b981', 'border': '1px solid #10b981'}).html('&#10004; Başarılı! Kanalınızı kontrol edin (Görsel ve Formatlı Test)');
                    } else {
                        msgBox.css({'color': '#ef4444', 'border': '1px solid #ef4444'}).html('&#10006; <strong>GÖNDERİM HATASI:</strong><br><br>' + response.data);
                    }
                    btn.prop('disabled', false).text('GÖRSEL VE FORMATLI TEST MESAJI GÖNDER');
                }
            });
        });
    });
    </script>
    <?php
}

// ==========================================
// TELEGRAM GÖNDERİM MOTORU (GÖRSELİ ÜSTE ALMAK İÇİN SENDPHOTO KULLANIMI)
// ==========================================
function sl_send_telegram_message($token, $chat_id, $formatted_text, $image_url = '') {
    // SADECE sendPhoto kullanarak görseli en üste alıyoruz. Görsel yoksa gönderilmez.
    if (empty($image_url)) return array('success' => false, 'error' => 'Görsel Hatası: Oyunun "Öne Çıkan Görseli" eksik olduğu için gönderilmedi.');
    
    $api_url = "https://api.telegram.org/bot{$token}/sendPhoto";
    
    // Telegram API parametreleri
    $body = array(
        'chat_id'    => $chat_id,
        'photo'      => $image_url,
        'caption'    => $formatted_text, // Metni görselin alt yazısı (caption) olarak gönderiyoruz.
        'parse_mode' => 'HTML' // Kalın metinler için HTML formatını kullanıyoruz
    );

    // İsteği Gönder
    $response = wp_remote_post($api_url, array('body' => $body));
    
    if ( is_wp_error( $response ) ) {
        return array('success' => false, 'error' => 'Sunucu Bağlantı Hatası: ' . $response->get_error_message());
    }
    
    $response_code = wp_remote_retrieve_response_code( $response );
    $response_body = wp_remote_retrieve_body( $response );
    $result = json_decode($response_body, true);

    if ($response_code == 200 && isset($result['ok']) && $result['ok']) {
        return array('success' => true, 'error' => '');
    } else {
        $error_desc = isset($result['description']) ? $result['description'] : 'Bilinmeyen API hatası (Log yok)';
        return array('success' => false, 'error' => "Telegram Log Kodu [$response_code]: <br>" . $error_desc);
    }
}

// TEST MESAJI İÇİN AJAX İŞLEYİCİ (YENİ FORMATI TEST EDER)
add_action('wp_ajax_sl_test_telegram', 'sl_test_telegram_handler');
function sl_test_telegram_handler() {
    $token = sanitize_text_field($_POST['token']);
    $chat_id = sanitize_text_field($_POST['chat_id']);

    if (empty($token) || empty($chat_id)) wp_send_json_error("Token ve Chat ID alanları boş olamaz.");

    // Senin İsteğine Göre Oluşturulan Yeni Şablon ve Format
    $test_formatted_text = "🎮 <b>Oyun Adı: Örnek Oyun Adı (Format Testi)</b>\n📦 <b>Dosya Boyutu: 1.5 GB</b>\n📱 <b>Platform: Android / PC</b>\n🌍 <b>Dil: Türkçe</b>\n📎 Link: https://riaslink.fun/ornek-oyun\n\n<b>📖 Konusu:</b>\n> Bu, sisteminizin düzgün çalıştığını gösteren otomatik bir test mesajıdır.\n\n<b>🎯 Etiketler: #android #test #oyun</b>\n___\n<b>Link geçmeyi bilmiyorsan</b>\n<b>@kural34link</b>";

    // Test için sitenin logosunu değil, global bir görseli kullanıyoruz (Hata vermemesi için)
    $test_image = 'https://riaslink.fun/wp-content/themes/steamlike/assets/images/placeholder.jpg'; // Eğer bu dosya yoksa, Wikimedia linkini kullan: 'https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Telegram_logo.svg/512px-Telegram_logo.svg.png'

    // Mesajı gönder ve sonucunu/hatasını al
    $send_result = sl_send_telegram_message($token, $chat_id, $test_formatted_text, $test_image);

    if ($send_result['success']) {
        wp_send_json_success();
    } else {
        wp_send_json_error($send_result['error']); 
    }
}

// ==========================================
// YENİ OYUN YAYINLANDIĞINDA OTOMATİK GÖNDERME (FORMATLI VE GÖRSEL ÜSTE)
// ==========================================
add_action('transition_post_status', 'sl_auto_post_to_telegram', 10, 3);
function sl_auto_post_to_telegram($new_status, $old_status, $post) {
    if ($new_status == 'publish' && $old_status != 'publish' && $post->post_type == 'game') {
        
        if (get_post_meta($post->ID, '_sl_telegram_shared', true)) return;

        $token = get_option('sl_tg_bot_token');
        $chat_id = get_option('sl_tg_chat_id');
        if (empty($token) || empty($chat_id)) return;

        $title = get_the_title($post->ID);
        $size = get_post_meta($post->ID, 'game_size', true) ?: 'Bilinmiyor';
        $link = get_permalink($post->ID);
        $excerpt = wp_trim_words($post->post_content, 35, '...'); 
        
        $platforms = wp_get_post_terms($post->ID, 'game_platform', array('fields' => 'names'));
        $plat_str = (!empty($platforms) && !is_wp_error($platforms)) ? implode(' / ', $platforms) : 'Bilinmiyor';
        
        // GÜÇLENDİRİLMİŞ ETİKET SİSTEMİ: Kategoriler ve Etiketler birleştiriliyor
        $all_taxonomies = wp_parse_args(wp_get_post_terms($post->ID, array('game_platform', 'gerilim', 'oyun')), array()); // 'gerilim' ve 'oyun' taxonomies'leri dahil edildi
        $merged_terms = wp_list_pluck($all_taxonomies, 'name'); // Sadece isimlerini al
        
        // WordPress Varsayılan Kategorileri ve Etiketleri de dahil et
        $cats_tags = array_merge(wp_get_post_categories($post->ID, array('fields' => 'names')), wp_get_post_tags($post->ID, array('fields' => 'names')));
        
        // Tümünü birleştir ve boşlukları silip hashtag yap
        $merged_terms = array_merge($merged_terms, $cats_tags);
        $merged_terms = array_unique($merged_terms); // Tekrar edenleri temizle
        
        $tag_str = '';
        if(!empty($merged_terms)){
            $tag_str = '#' . implode(' #', array_map(function($t){ return str_replace(' ', '', $t); }, $merged_terms));
        }

        // Kapak Görseli
        $image_url = get_the_post_thumbnail_url($post->ID, 'full');

        // Metni senin istediğin kalın/alıntı HTML formatında oluşturuyoruz (Caption sınırı 1024 karakter olduğu için metni kısaltıyoruz)
        $formatted_caption = "🎮 <b>Oyun Adı: {$title}</b>\n📦 <b>Dosya Boyutu: {$size}</b>\n📱 <b>Platform: {$plat_str}</b>\n🌍 <b>Dil: Türkçe</b>\n📎 Link: {$link}\n\n<b>📖 Konusu:</b>\n> {$excerpt}\n\n<b>🎯 Etiketler: {$tag_str}</b>\n___\n<b>Link geçmeyi bilmiyorsan</b>\n<b>@kural34link</b>";

        // Görsel yoksa, bir görsel gönderilmemesi hatası almamak için placeholder linkini kullan
        $final_image = $image_url ?: 'https://riaslink.fun/wp-content/themes/steamlike/assets/images/placeholder.jpg';

        $send_result = sl_send_telegram_message($token, $chat_id, $formatted_caption, $final_image);

        if ($send_result['success']) {
            update_post_meta($post->ID, '_sl_telegram_shared', '1');
        } else {
            error_log("SteamLike Telegram Bot Hatası (Oyun ID: {$post->ID}): " . $send_result['error']);
        }
    }
}
