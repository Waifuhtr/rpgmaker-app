<?php
/**
 * Oyun Detay - Ekran Görüntüleri Galerisi ve Lightbox
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) exit;

$post_id = get_the_ID();
$screenshots = get_post_meta( $post_id, 'game_screenshots', true );

// Ekran görüntüsü yoksa hiç HTML basma
if ( ! $screenshots ) return;
?>

<div class="sl-content-box">
	<h3 style="margin-bottom: 15px;"><?php esc_html_e( 'Ekran Görüntüleri', 'steamlike' ); ?></h3>
	
	<div class="sl-screenshots-gallery-wrap" style="position: relative; display: flex; align-items: center;">
		
		<button class="sl-slider-nav sl-prev" onclick="document.getElementById('sl-gallery-track').scrollBy({left: -300, behavior: 'smooth'})" style="position:absolute; left:-15px; z-index:5; background:var(--sl-color-blue-100); color:#fff; border:none; border-radius:50%; width:36px; height:36px; cursor:pointer; box-shadow:0 4px 10px rgba(0,0,0,0.5); font-weight:bold; font-size:18px;">&#8249;</button>
		
		<div id="sl-gallery-track" style="display: flex; gap: 15px; overflow-x: auto; scroll-snap-type: x mandatory; padding-bottom: 15px; scrollbar-width: none; width: 100%;">
			<?php 
			$image_ids = explode( ',', $screenshots );
			foreach ( $image_ids as $img_id ) {
				// Tam boy resim Lightbox için, Large resim kutular için
				$full_img_url = wp_get_attachment_image_url( $img_id, 'full' );
				$thumb_img_url = wp_get_attachment_image_url( $img_id, 'large' );
				
				if ( $full_img_url ) {
					// onclick ile tam boy resmi fonksiyona gönderiyoruz
					echo '<div style="scroll-snap-align: start; flex: 0 0 80%; max-width: 400px; border-radius: 12px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.3); cursor: pointer;" onclick="slOpenLightbox(\'' . esc_url( $full_img_url ) . '\')">';
					echo '<img src="' . esc_url( $thumb_img_url ) . '" style="width: 100%; height: 220px; object-fit: cover; display: block;" alt="Screenshot">';
					echo '</div>';
				}
			}
			?>
		</div>
		
		<button class="sl-slider-nav sl-next" onclick="document.getElementById('sl-gallery-track').scrollBy({left: 300, behavior: 'smooth'})" style="position:absolute; right:-15px; z-index:5; background:var(--sl-color-blue-100); color:#fff; border:none; border-radius:50%; width:36px; height:36px; cursor:pointer; box-shadow:0 4px 10px rgba(0,0,0,0.5); font-weight:bold; font-size:18px;">&#8250;</button>

	</div>
	
	<style>
	#sl-gallery-track::-webkit-scrollbar { display: none; }
	</style>
</div>

<div id="sl-lightbox" class="sl-lightbox" onclick="slCloseLightbox(event)">
	<span class="sl-lightbox-close" onclick="slCloseLightbox(event)">&times;</span>
	<img id="sl-lightbox-img" src="" alt="Tam Ekran Görsel" onclick="slToggleZoom(event)">
</div>

<script>
	function slOpenLightbox(imgUrl) {
		var lightbox = document.getElementById('sl-lightbox');
		var img = document.getElementById('sl-lightbox-img');
		
		img.src = imgUrl;
		img.classList.remove('zoomed'); // Her açılışta zoom'u sıfırla
		lightbox.classList.add('active'); // Karanlık ekranı göster
	}

	function slCloseLightbox(e) {
		// Sadece siyah arka plana veya X tuşuna basıldıysa kapat (Resme tıklandığında kapanmasın)
		if (e.target.id === 'sl-lightbox' || e.target.classList.contains('sl-lightbox-close')) {
			document.getElementById('sl-lightbox').classList.remove('active');
		}
	}

	function slToggleZoom(e) {
		e.stopPropagation(); // Tıklamanın arka plana geçip ekranı kapatmasını engelle
		var img = document.getElementById('sl-lightbox-img');
		img.classList.toggle('zoomed'); // Zoom sınıfını ekle/çıkar
	}
</script>
