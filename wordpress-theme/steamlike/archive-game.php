<?php
/**
 * Oyun Arşiv Sayfası (Kompakt Açılır Menü Filtreleme)
 */
if ( ! defined( 'ABSPATH' ) ) exit;
get_header();

function sl_get_filter_terms( $taxonomy ) { return get_terms( array( 'taxonomy' => $taxonomy, 'hide_empty' => true ) ); }

// URL'den gelen parametreyi kontrol eden yardımcı fonksiyon
function sl_is_term_active( $taxonomy, $slug ) {
	if ( isset( $_GET[$taxonomy] ) ) {
		$values = explode( ',', $_GET[$taxonomy] );
		return in_array( $slug, $values );
	}
	return false;
}
?>

<main id="primary" class="site-main sl-archive-page">
	<div class="container">

		<?php sl_breadcrumb(); ?>

		<header class="sl-archive-header">
			<h1 class="sl-page-title"><?php esc_html_e( 'Oyun Kütüphanesi', 'steamlike' ); ?></h1>
			<p><?php esc_html_e( 'Gelişmiş filtreleme seçenekleriyle aradığınız oyunu bulun.', 'steamlike' ); ?></p>
		</header>

		<div class="sl-archive-layout">
			
			<details class="sl-filter-sidebar" open>
				<summary>
					<span><?php esc_html_e( 'Filtrele ve Sırala', 'steamlike' ); ?></span>
					<span class="dashicons dashicons-arrow-down-alt2 sl-filter-toggle-hint"></span>
				</summary>
				<form id="sl-filter-form" class="sl-filter-form">
					
					<input type="text" name="search_text" id="search_text" class="sl-filter-input full-width" placeholder="<?php esc_attr_e( 'Oyun ara...', 'steamlike' ); ?>" autocomplete="off" value="<?php echo isset($_GET['search_text']) ? esc_attr($_GET['search_text']) : ''; ?>">

					<select name="sort_by" id="sort_by" class="sl-filter-select full-width">
						<option value="newest" <?php selected(isset($_GET['sort_by']) && $_GET['sort_by'] == 'newest'); ?>><?php esc_html_e( 'En Yeni Eklenenler', 'steamlike' ); ?></option>
						<option value="oldest" <?php selected(isset($_GET['sort_by']) && $_GET['sort_by'] == 'oldest'); ?>><?php esc_html_e( 'En Eskiler', 'steamlike' ); ?></option>
						<option value="rating_high" <?php selected(isset($_GET['sort_by']) && $_GET['sort_by'] == 'rating_high'); ?>><?php esc_html_e( 'En Yüksek Puanlılar', 'steamlike' ); ?></option>
					</select>

					<?php $genres = sl_get_filter_terms( 'game_genre' ); ?>
					<?php if ( ! empty( $genres ) && ! is_wp_error( $genres ) ) : ?>
						<select name="game_genre" class="sl-filter-select full-width">
							<option value=""><?php esc_html_e( 'Tüm Türler', 'steamlike' ); ?></option>
							<?php foreach ( $genres as $genre ) : ?>
								<option value="<?php echo esc_attr( $genre->slug ); ?>" <?php selected( sl_is_term_active('game_genre', $genre->slug), true ); ?>><?php echo esc_html( $genre->name ); ?></option>
							<?php endforeach; ?>
						</select>
					<?php endif; ?>

					<?php $platforms = sl_get_filter_terms( 'game_platform' ); ?>
					<?php if ( ! empty( $platforms ) && ! is_wp_error( $platforms ) ) : ?>
						<select name="game_platform" class="sl-filter-select">
							<option value=""><?php esc_html_e( 'Platform', 'steamlike' ); ?></option>
							<?php foreach ( $platforms as $platform ) : ?>
								<option value="<?php echo esc_attr( $platform->slug ); ?>" <?php selected( sl_is_term_active('game_platform', $platform->slug), true ); ?>><?php echo esc_html( $platform->name ); ?></option>
							<?php endforeach; ?>
						</select>
					<?php endif; ?>

					<?php $statuses = sl_get_filter_terms( 'game_status' ); ?>
					<?php if ( ! empty( $statuses ) && ! is_wp_error( $statuses ) ) : ?>
						<select name="game_status" class="sl-filter-select">
							<option value=""><?php esc_html_e( 'Durum', 'steamlike' ); ?></option>
							<?php foreach ( $statuses as $status ) : ?>
								<option value="<?php echo esc_attr( $status->slug ); ?>" <?php selected( sl_is_term_active('game_status', $status->slug), true ); ?>><?php echo esc_html( $status->name ); ?></option>
							<?php endforeach; ?>
						</select>
					<?php endif; ?>

					<?php $tags = sl_get_filter_terms( 'post_tag' ); ?>
					<?php if ( ! empty( $tags ) && ! is_wp_error( $tags ) ) : ?>
						<select name="post_tag" class="sl-filter-select full-width">
							<option value=""><?php esc_html_e( 'Tüm Etiketler', 'steamlike' ); ?></option>
							<?php foreach ( $tags as $tag ) : ?>
								<option value="<?php echo esc_attr( $tag->slug ); ?>" <?php selected( sl_is_term_active('post_tag', $tag->slug), true ); ?>><?php echo esc_html( $tag->name ); ?></option>
							<?php endforeach; ?>
						</select>
					<?php endif; ?>

				</form>
			</details>

			<div class="sl-archive-content">
				<div id="sl-filter-results" class="post-grid">
					<?php
					// Sayfa ilk yüklendiğinde varsayılan içerikler
					if ( have_posts() ) :
						while ( have_posts() ) : the_post(); get_template_part( 'template-parts/cards/card-game' ); endwhile;
					else :
						echo '<p class="sl-empty-note">' . esc_html__( 'Henüz oyun bulunmuyor.', 'steamlike' ) . '</p>';
					endif;
					?>
				</div>
			</div>

		</div>
	</div>
</main>

<script>
jQuery(document).ready(function($) {
	var filterTimer;
    var $filterForm = $('#sl-filter-form');
    var $resultsContainer = $('#sl-filter-results');

    // Güvenlik anahtarını formun içine görünmez şekilde ekliyoruz
    if( $filterForm.find('input[name="security"]').length === 0 ) {
        $filterForm.append('<input type="hidden" name="security" value="<?php echo wp_create_nonce("steamlike_ajax_nonce"); ?>">');
    }

    $filterForm.on('change', function(e) {
        e.preventDefault();
        
        var formData = $(this).serializeArray();
        formData.push({ name: 'action', value: 'sl_filter_games' });

        $.ajax({
            url: '<?php echo admin_url("admin-ajax.php"); ?>',
            type: 'POST',
            data: formData,
            beforeSend: function() {
                $resultsContainer.css('opacity', '0.4'); // Filtrelerken hafif solma efekti
            },
            success: function(response) {
                if(response === '0' || response === '-1') {
                    $resultsContainer.html('<div class="sl-filter-error">Filtreleme işlemi başarısız oldu. Lütfen sayfayı yenileyin.</div>');
                } else {
                    $resultsContainer.html(response); 
                }
                $resultsContainer.css('opacity', '1'); 
            },
            error: function() {
                $resultsContainer.html('<div class="sl-filter-error">Sunucuya bağlanılamadı. Lütfen tekrar deneyin.</div>');
                $resultsContainer.css('opacity', '1');
            }
        });
    });

	$('#search_text').on('keyup', function() {
		clearTimeout(filterTimer);
		filterTimer = setTimeout(function() { $filterForm.trigger('change'); }, 600);
	});

	if (window.location.search.length > 1) {
		setTimeout(function() { $filterForm.trigger('change'); }, 100);
	}
});
</script>

<?php get_footer(); ?>
