<?php
/**
 * Template Name: İndirme Yönlendirme Sayfası
 */
if ( ! defined( 'ABSPATH' ) ) exit;
get_header();

$game_id = isset( $_GET['game_id'] ) ? intval( $_GET['game_id'] ) : 0;
$download_url = get_post_meta( $game_id, 'game_download_url', true );

if ( ! $game_id || ! $download_url ) {
	echo '<div class="container sl-invalid-link-box"><h2>' . esc_html__( 'Geçersiz Bağlantı veya Link Bulunamadı.', 'steamlike' ) . '</h2></div>';
	get_footer();
	exit;
}
?>
<main id="primary" class="site-main sl-download-page">
	<div class="container sl-download-card">
		
		<div class="sl-download-cover">
			<?php echo get_the_post_thumbnail( $game_id, 'medium', array( 'loading' => 'eager', 'alt' => get_the_title( $game_id ) ) ); ?>
		</div>
		
		<h1 class="sl-download-title"><?php echo esc_html( get_the_title( $game_id ) ); ?></h1>
		<p class="sl-download-note"><?php esc_html_e( 'Güvenli indirme sayfasına yönlendiriliyorsunuz. Lütfen bekleyin...', 'steamlike' ); ?></p>
		
		<div class="sl-countdown" id="sl-countdown">5</div>
		
		<p class="sl-download-hint"><?php esc_html_e( 'Eğer yönlendirme başlamazsa aşağıdaki butona tıklayın:', 'steamlike' ); ?></p>
		<a href="<?php echo esc_url( $download_url ); ?>" class="sl-btn sl-btn-primary sl-manual-link-hidden" id="sl-manual-link"><?php esc_html_e( 'Manuel Olarak İndir', 'steamlike' ); ?></a>
		
	</div>
</main>

<script>
	var timeLeft = 5;
	var countdownElem = document.getElementById('sl-countdown');
	var manualLink = document.getElementById('sl-manual-link');
	var redirectUrl = "<?php echo esc_url_raw( $download_url ); ?>";

	var timer = setInterval(function() {
		timeLeft--;
		countdownElem.textContent = timeLeft;
		if (timeLeft <= 0) {
			clearInterval(timer);
			countdownElem.innerHTML = '<span class="dashicons dashicons-yes-alt sl-text-success"></span>';
			manualLink.style.display = 'inline-flex';
			window.location.href = redirectUrl; // 5 saniye dolunca asıl linke atar
		}
	}, 1000);
</script>

<?php get_footer(); ?>
