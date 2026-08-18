<?php
/**
 * wp-admin durum ekranı.
 *
 * Uygulama sunucu adresini kodun içinde taşır; bu ekranın işi bağlantının çalıştığını
 * doğrulamak ve neyin nereden okunduğunu göstermek.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PSB_Admin {

	public static function init() {
		add_action( 'admin_menu', array( __CLASS__, 'menu' ) );
	}

	public static function menu() {
		add_submenu_page(
			'options-general.php',
			'PixelStore Bridge',
			'PixelStore Bridge',
			'manage_options',
			'pixelstore-bridge',
			array( __CLASS__, 'render' )
		);
	}

	public static function render() {
		if ( ! current_user_can( 'manage_options' ) ) {
			wp_die( 'Yetkiniz yok.' );
		}

		$theme_ok = psb_theme_active();
		$counts   = $theme_ok ? wp_count_posts( PSB_POST_TYPE ) : null;
		$reviews  = (int) get_comments( array( 'type' => 'review', 'status' => 'approve', 'count' => true ) );
		?>
		<div class="wrap">
			<h1>PixelStore Bridge <span style="font-size:13px;color:#666">v<?php echo esc_html( PSB_VERSION ); ?></span></h1>

			<?php if ( ! $theme_ok ) : ?>
				<div class="notice notice-error">
					<p><strong>SteamLike teması etkin değil.</strong> Bu eklenti temanın <code>game</code> kayıt tipini
					okur; tema etkin olmadan uygulama oyun listesi alamaz.</p>
				</div>
			<?php else : ?>
				<div class="notice notice-success"><p>SteamLike teması algılandı, köprü hazır.</p></div>
			<?php endif; ?>

			<h2>Durum</h2>
			<table class="widefat striped" style="max-width:760px">
				<tbody>
					<tr><th style="width:240px">Yayında oyun</th><td><?php echo $counts ? esc_html( (int) $counts->publish ) : '—'; ?></td></tr>
					<tr><th>Taslak oyun</th><td><?php echo $counts ? esc_html( (int) $counts->draft + (int) $counts->pending ) : '—'; ?></td></tr>
					<tr><th>Kullanıcı incelemesi</th><td><?php echo esc_html( $reviews ); ?></td></tr>
					<tr><th>Kayıtlı kullanıcı</th><td><?php echo esc_html( (int) count_users()['total_users'] ); ?></td></tr>
					<tr>
						<th>Sağlık ucu</th>
						<td><a href="<?php echo esc_url( rest_url( PSB_NAMESPACE . '/health' ) ); ?>" target="_blank" rel="noopener"><?php echo esc_html( rest_url( PSB_NAMESPACE . '/health' ) ); ?></a></td>
					</tr>
				</tbody>
			</table>

			<h2>Veri nereden okunuyor</h2>
			<p>Eklenti kendi tablosunu veya kayıt tipini oluşturmaz. Uygulama ile site aynı anahtarları paylaşır:</p>
			<table class="widefat striped" style="max-width:760px">
				<thead><tr><th style="width:220px">Uygulamada</th><th>Sitede</th></tr></thead>
				<tbody>
					<tr><td>Oyun kaydı</td><td><code>game</code> kayıt tipi (SteamLike)</td></tr>
					<tr><td>Tür / platform / dil / durum</td><td><code>game_genre</code>, <code>game_platform</code>, <code>game_language</code>, <code>game_status</code></td></tr>
					<tr><td>Kapak ve ekran görüntüleri</td><td>öne çıkan görsel + <code>game_screenshots</code></td></tr>
					<tr><td>İstek listesi</td><td><code>sl_favorites</code> (kullanıcı meta)</td></tr>
					<tr><td>Puan</td><td><code>sl_user_rating_avg</code> / <code>_count</code> / <code>_sum</code></td></tr>
					<tr><td>İnceleme / yorum</td><td><code>wp_comments</code>, <code>comment_type = review</code></td></tr>
					<tr><td>İndirme sayacı</td><td><code>game_download_count</code></td></tr>
					<tr><td>Profil fotoğrafı</td><td><code>sl_custom_avatar</code> (kullanıcı meta)</td></tr>
					<tr><td>Hata raporu</td><td><code>sl_report</code> kayıt tipi</td></tr>
				</tbody>
			</table>

			<h2>Yetki</h2>
			<p>
				Uygulamadaki <strong>yönetim paneli</strong> <code>manage_options</code> veya
				<code>edit_others_posts</code> yeteneği olan kullanıcılara açılır (yönetici ve editör).
				Diğer roller yalnızca mağazayı görür; taslak kayıtlar listelerine hiç girmez ve yönetim
				uçları onlara <code>403</code> döner.
			</p>

			<h2>Sorun giderme</h2>
			<ul style="list-style:disc;margin-left:20px;max-width:760px">
				<li><strong>401 alıyorsun ama parola doğru:</strong> bazı sunucular <code>Authorization</code>
					başlığını PHP'ye geçirmez. Uygulama yedek olarak <code>X-PixelStore-Token</code> başlığı da
					gönderir; yine olmuyorsa <code>.htaccess</code> dosyasına <code>CGIPassAuth On</code> ekle.</li>
				<li><strong>404 alıyorsun:</strong> Ayarlar → Kalıcı bağlantılar sayfasını bir kez kaydet.</li>
				<li><strong>Görsel yüklenmiyor:</strong> PHP <code>upload_max_filesize</code> ve
					<code>post_max_size</code> değerlerinin en az 8 MB olduğundan emin ol.</li>
				<li><strong>Eski PixelStore API eklentisi:</strong> daha önce <code>pixelstore-api</code>
					kurduysan sil; artık gerek yok ve kendi <code>pixelstore_app</code> kayıtlarını taşıyor.</li>
			</ul>
		</div>
		<?php
	}
}
