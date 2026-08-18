<?php
/**
 * Standart Sayfa Şablonu
 * Kısa kodlar ve HTML burada sorunsuz çalışır.
 */
get_header(); ?>

<main id="primary" class="site-main sl-standard-page">
	<div class="container">
		<?php sl_breadcrumb(); ?>
		<div class="sl-content-box">
			<?php
			while ( have_posts() ) :
				the_post();

				// Sayfa başlığı
				echo '<h1 class="sl-page-content-title">' . esc_html( get_the_title() ) . '</h1>';

				// İÇERİK (Kısa kodların ve eklentilerin tam çalıştığı yer)
				echo '<div class="sl-entry-content">';
				the_content();
				echo '</div>';

			endwhile;
			?>
		</div>
	</div>
</main>

<?php get_footer(); ?>
