<?php
/**
 * Ek SEO Katmanı
 *
 * inc/../functions.php içindeki sl_native_seo_tags() fonksiyonu yalnızca tekil
 * oyun sayfalarını (is_singular('game')) kapsıyor. Bu dosya; ana sayfa, oyun
 * kütüphanesi arşivi, geliştirici/kullanıcı profili ve standart sayfalar için
 * eksik olan meta açıklama, canonical bağlantı, Open Graph / Twitter Card
 * etiketlerini ve temel yapılandırılmış veriyi (JSON-LD) tamamlar.
 *
 * Tekil oyun sayfalarına KASITLI OLARAK dokunulmaz; oradaki mevcut SEO çıktısı
 * zaten eksiksizdir ve burada tekrarlanmaz.
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

add_action( 'wp_head', 'sl_extra_seo_tags', 5 );
function sl_extra_seo_tags() {

	// Tekil oyun sayfaları zaten sl_native_seo_tags() ile tam kapsamlı işleniyor.
	if ( is_singular( 'game' ) ) {
		return;
	}

	$title       = '';
	$description = '';
	$canonical   = '';
	$image       = '';

	if ( is_front_page() ) {
		$title       = get_bloginfo( 'name' );
		$tagline     = get_bloginfo( 'description' );
		$description = $tagline ? $tagline : sprintf(
			/* translators: %s: Site adı */
			__( '%s - Türkçe oyun ve mod indirme platformu. Yeni eklenenler, en çok oylanan ve en çok oynanan oyunları keşfedin.', 'steamlike' ),
			get_bloginfo( 'name' )
		);
		$canonical = home_url( '/' );
		$logo_id   = get_theme_mod( 'custom_logo' );
		if ( $logo_id ) {
			$image = wp_get_attachment_image_url( $logo_id, 'full' );
		}
	} elseif ( is_post_type_archive( 'game' ) ) {
		$title       = __( 'Oyun Kütüphanesi', 'steamlike' ) . ' - ' . get_bloginfo( 'name' );
		$description = __( 'Oyun kütüphanesindeki tüm başlıkları tür, platform, dil ve puana göre filtreleyerek aradığınız oyunu kolayca bulun.', 'steamlike' );
		$canonical   = get_post_type_archive_link( 'game' );
	} elseif ( is_author() ) {
		$display_name = get_the_author();
		$title        = $display_name . ' ' . __( 'Profili', 'steamlike' ) . ' - ' . get_bloginfo( 'name' );
		$description  = sprintf(
			/* translators: %s: Kullanıcı adı */
			__( '%s adlı kullanıcının herkese açık profili: favori oyunları ve oynama geçmişi.', 'steamlike' ),
			$display_name
		);
		$canonical = get_author_posts_url( get_the_author_meta( 'ID' ) );
	} elseif ( is_page() ) {
		global $post;
		$title = get_the_title() . ' - ' . get_bloginfo( 'name' );
		$raw   = '';
		if ( $post ) {
			$raw = $post->post_excerpt ? $post->post_excerpt : $post->post_content;
		}
		$stripped    = $raw ? wp_strip_all_tags( strip_shortcodes( $raw ) ) : '';
		$description = $stripped ? wp_trim_words( $stripped, 30, '...' ) : get_bloginfo( 'description' );
		$canonical   = get_permalink();
	} else {
		// Arama sonuçları, 404 vb. sayfalar zaten sl_clean_seo_robots() ile noindex.
		return;
	}

	if ( $description ) {
		$description = wp_trim_words( $description, 30, '...' );
	}

	echo "\n<!-- SteamLike Ek SEO Etiketleri -->\n";

	if ( $description ) {
		echo '<meta name="description" content="' . esc_attr( $description ) . '">' . "\n";
	}
	if ( $canonical ) {
		echo '<link rel="canonical" href="' . esc_url( $canonical ) . '">' . "\n";
	}

	echo '<meta property="og:type" content="website">' . "\n";
	echo '<meta property="og:title" content="' . esc_attr( $title ) . '">' . "\n";
	if ( $description ) {
		echo '<meta property="og:description" content="' . esc_attr( $description ) . '">' . "\n";
	}
	if ( $canonical ) {
		echo '<meta property="og:url" content="' . esc_url( $canonical ) . '">' . "\n";
	}
	echo '<meta property="og:site_name" content="' . esc_attr( get_bloginfo( 'name' ) ) . '">' . "\n";
	echo '<meta property="og:locale" content="' . esc_attr( str_replace( '-', '_', get_locale() ) ) . '">' . "\n";
	if ( $image ) {
		echo '<meta property="og:image" content="' . esc_url( $image ) . '">' . "\n";
	}

	echo '<meta name="twitter:card" content="' . ( $image ? 'summary_large_image' : 'summary' ) . '">' . "\n";
	echo '<meta name="twitter:title" content="' . esc_attr( $title ) . '">' . "\n";
	if ( $description ) {
		echo '<meta name="twitter:description" content="' . esc_attr( $description ) . '">' . "\n";
	}
	if ( $image ) {
		echo '<meta name="twitter:image" content="' . esc_url( $image ) . '">' . "\n";
	}

	// --- Ana sayfaya özel WebSite + Organization şeması (site linki arama kutusu dahil) ---
	if ( is_front_page() ) {
		$site_schema = array(
			'@context' => 'https://schema.org',
			'@graph'   => array(
				array(
					'@type'           => 'WebSite',
					'@id'             => home_url( '/#website' ),
					'url'             => home_url( '/' ),
					'name'            => get_bloginfo( 'name' ),
					'potentialAction' => array(
						'@type'       => 'SearchAction',
						'target'      => home_url( '/?s={search_term_string}' ),
						'query-input' => 'required name=search_term_string',
					),
				),
				array(
					'@type' => 'Organization',
					'@id'   => home_url( '/#organization' ),
					'name'  => get_bloginfo( 'name' ),
					'url'   => home_url( '/' ),
				),
			),
		);
		echo '<script type="application/ld+json">' . wp_json_encode( $site_schema, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . '</script>' . "\n";
	}

	// --- Sayfa yolu (breadcrumb) için BreadcrumbList şeması ---
	$crumbs = sl_get_breadcrumb_items_for_schema();
	if ( count( $crumbs ) > 1 ) {
		$list_items = array();
		foreach ( $crumbs as $index => $crumb ) {
			$item = array(
				'@type'    => 'ListItem',
				'position' => $index + 1,
				'name'     => $crumb['name'],
			);
			if ( ! empty( $crumb['url'] ) ) {
				$item['item'] = $crumb['url'];
			}
			$list_items[] = $item;
		}
		$breadcrumb_schema = array(
			'@context'        => 'https://schema.org',
			'@type'           => 'BreadcrumbList',
			'itemListElement' => $list_items,
		);
		echo '<script type="application/ld+json">' . wp_json_encode( $breadcrumb_schema, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE ) . '</script>' . "\n";
	}
}

/**
 * sl_breadcrumb() fonksiyonunun ürettiği görsel yol ile birebir aynı mantığı
 * kullanarak BreadcrumbList şeması için düz bir dizi üretir.
 */
function sl_get_breadcrumb_items_for_schema() {
	$items = array();

	if ( is_front_page() ) {
		return $items;
	}

	$items[] = array(
		'name' => __( 'Ana Sayfa', 'steamlike' ),
		'url'  => home_url( '/' ),
	);

	if ( is_post_type_archive( 'game' ) ) {
		$items[] = array( 'name' => __( 'Oyun Kütüphanesi', 'steamlike' ) );
	} elseif ( is_search() ) {
		$items[] = array( 'name' => __( 'Arama Sonuçları', 'steamlike' ) );
	} elseif ( is_singular( 'game' ) ) {
		$items[] = array(
			'name' => __( 'Oyunlar', 'steamlike' ),
			'url'  => get_post_type_archive_link( 'game' ),
		);
		$genres = get_the_terms( get_the_ID(), 'game_genre' );
		if ( $genres && ! is_wp_error( $genres ) ) {
			$first_genre = $genres[0];
			$items[]     = array(
				'name' => $first_genre->name,
				'url'  => add_query_arg( array( 'game_genre' => $first_genre->slug ), get_post_type_archive_link( 'game' ) ),
			);
		}
		$items[] = array( 'name' => get_the_title() );
	} elseif ( is_page() ) {
		$items[] = array( 'name' => get_the_title() );
	} elseif ( is_author() ) {
		$items[] = array( 'name' => get_the_author() );
	}

	return $items;
}
