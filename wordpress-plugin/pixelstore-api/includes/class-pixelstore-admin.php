<?php
/**
 * wp-admin ekranı: bağlantı bilgisi, demo katalog kurulumu ve uç listesi.
 *
 * Uygulamayı bağlamak için gereken tek bilgi site adresidir; bu ekran onu kopyalanabilir biçimde
 * gösterir ve kurulumun doğru yapıldığını (kayıt sayısı, uç erişimi) tek bakışta doğrular.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

class PixelStore_Admin {

	public static function init() {
		add_action( 'admin_menu', array( __CLASS__, 'menu' ) );
		add_action( 'admin_post_pixelstore_seed', array( __CLASS__, 'handle_seed' ) );
		add_action( 'admin_post_pixelstore_purge', array( __CLASS__, 'handle_purge' ) );
	}

	public static function menu() {
		add_menu_page(
			'PixelStore',
			'PixelStore',
			'manage_options',
			'pixelstore',
			array( __CLASS__, 'render' ),
			'dashicons-games',
			30
		);
		add_submenu_page(
			'pixelstore',
			'PixelStore Durum',
			'Durum ve kurulum',
			'manage_options',
			'pixelstore',
			array( __CLASS__, 'render' )
		);
	}

	public static function render() {
		if ( ! current_user_can( 'manage_options' ) ) {
			wp_die( 'Yetkiniz yok.' );
		}

		$counts    = wp_count_posts( PIXELSTORE_POST_TYPE );
		$published = (int) $counts->publish;
		$drafts    = (int) $counts->draft;
		$base      = untrailingslashit( home_url() );
		$seeded_at = get_option( 'pixelstore_seeded_at' );
		$notice    = isset( $_GET['pixelstore_notice'] ) ? sanitize_text_field( wp_unslash( $_GET['pixelstore_notice'] ) ) : '';
		?>
		<div class="wrap">
			<h1>PixelStore API</h1>

			<?php if ( $notice ) : ?>
				<div class="notice notice-success is-dismissible"><p><?php echo esc_html( $notice ); ?></p></div>
			<?php endif; ?>

			<h2>Uygulamayı bağla</h2>
			<p>PixelStore uygulamasında <strong>Profil → Bağlantı ayarları</strong> ekranına şu adresi gir:</p>
			<p>
				<input type="text" readonly value="<?php echo esc_attr( $base ); ?>"
					   class="large-text code" onclick="this.select()" />
			</p>
			<p class="description">
				<code>/wp-json</code> eklemene gerek yok, uygulama kendisi ekliyor.
				Ardından WordPress kullanıcı adın ve parolanla giriş yap.
			</p>

			<h2>Durum</h2>
			<table class="widefat striped" style="max-width:720px">
				<tbody>
					<tr><th style="width:220px">Eklenti sürümü</th><td><?php echo esc_html( PIXELSTORE_VERSION ); ?></td></tr>
					<tr><th>Yayında kayıt</th><td><?php echo esc_html( $published ); ?></td></tr>
					<tr><th>Taslak kayıt</th><td><?php echo esc_html( $drafts ); ?></td></tr>
					<tr><th>Demo katalog</th><td><?php echo $seeded_at ? esc_html( $seeded_at ) . ' tarihinde kuruldu' : 'henüz kurulmadı'; ?></td></tr>
					<tr>
						<th>Sağlık ucu</th>
						<td>
							<a href="<?php echo esc_url( rest_url( PIXELSTORE_NAMESPACE . '/health' ) ); ?>" target="_blank" rel="noopener">
								<?php echo esc_html( rest_url( PIXELSTORE_NAMESPACE . '/health' ) ); ?>
							</a>
						</td>
					</tr>
				</tbody>
			</table>

			<h2>Demo katalog</h2>
			<p>
				Android uygulamasıyla aynı 8 örnek kaydı (biri bilinçli olarak taslak) ve 6 türü kurar.
				Var olan PixelStore kayıtları silinir; sitedeki diğer içeriklere dokunulmaz.
			</p>
			<form method="post" action="<?php echo esc_url( admin_url( 'admin-post.php' ) ); ?>" style="display:inline">
				<?php wp_nonce_field( 'pixelstore_seed' ); ?>
				<input type="hidden" name="action" value="pixelstore_seed" />
				<?php submit_button( 'Demo kataloğu kur', 'primary', 'submit', false ); ?>
			</form>
			<form method="post" action="<?php echo esc_url( admin_url( 'admin-post.php' ) ); ?>" style="display:inline;margin-left:8px"
				  onsubmit="return confirm('Tüm PixelStore kayıtları silinecek. Emin misin?');">
				<?php wp_nonce_field( 'pixelstore_purge' ); ?>
				<input type="hidden" name="action" value="pixelstore_purge" />
				<?php submit_button( 'Tüm kayıtları sil', 'delete', 'submit', false ); ?>
			</form>

			<h2>Roller</h2>
			<p>
				Uygulamadaki <strong>yönetim paneli</strong>, WordPress'te
				<code>manage_options</code> veya <code>edit_others_posts</code> yeteneğine sahip
				kullanıcılara açılır (yönetici ve editör). Diğer roller yalnızca mağazayı görür;
				yönetim uçları onlara <code>403</code> döner ve katalog yükünde taslak kayıtlar yer almaz.
			</p>

			<h2>Uçlar</h2>
			<table class="widefat striped" style="max-width:900px">
				<thead><tr><th style="width:340px">Uç</th><th>Yetki</th><th>Açıklama</th></tr></thead>
				<tbody>
					<tr><td><code>GET /health</code></td><td>herkes</td><td>Bağlantı sınaması</td></tr>
					<tr><td><code>POST /auth/login</code></td><td>herkes</td><td>Jeton üretir</td></tr>
					<tr><td><code>GET /auth/me</code></td><td>oturum</td><td>Oturumu doğrular</td></tr>
					<tr><td><code>POST /auth/logout</code></td><td>oturum</td><td>Jetonu iptal eder</td></tr>
					<tr><td><code>GET /catalog</code></td><td>oturum</td><td>Kayıtlar + türler</td></tr>
					<tr><td><code>GET /apps/{id}</code></td><td>oturum</td><td>Tek kayıt</td></tr>
					<tr><td><code>POST /apps/{id}/install</code></td><td>oturum</td><td>İndirme sayacı</td></tr>
					<tr><td><code>POST /apps</code></td><td><strong>yönetici</strong></td><td>Yeni kayıt</td></tr>
					<tr><td><code>PUT /apps/{id}</code></td><td><strong>yönetici</strong></td><td>Kaydı güncelle</td></tr>
					<tr><td><code>DELETE /apps/{id}</code></td><td><strong>yönetici</strong></td><td>Kaydı sil</td></tr>
					<tr><td><code>POST /apps/{id}/published</code></td><td><strong>yönetici</strong></td><td>Yayın durumu</td></tr>
					<tr><td><code>GET /users</code></td><td><strong>yönetici</strong></td><td>Kullanıcı listesi</td></tr>
					<tr><td><code>GET /stats</code></td><td><strong>yönetici</strong></td><td>Özet istatistik</td></tr>
					<tr><td><code>POST /seed</code></td><td><strong>yönetici</strong></td><td>Demo kataloğu kurar</td></tr>
				</tbody>
			</table>

			<h2>Sorun giderme</h2>
			<ul style="list-style:disc;margin-left:20px">
				<li>
					<strong>401 alıyorsun ama parola doğru:</strong> bazı sunucular
					<code>Authorization</code> başlığını PHP'ye geçirmez. Uygulama bu durumda
					<code>X-PixelStore-Token</code> başlığını da gönderir; yine olmuyorsa
					<code>.htaccess</code>'e şunu ekle:
					<br><code>CGIPassAuth On</code>
				</li>
				<li><strong>404 alıyorsun:</strong> Ayarlar → Kalıcı bağlantılar sayfasını bir kez kaydet.</li>
				<li><strong>Kayıtlar boş:</strong> yukarıdaki "Demo kataloğu kur" düğmesine bas.</li>
			</ul>
		</div>
		<?php
	}

	public static function handle_seed() {
		if ( ! current_user_can( 'manage_options' ) ) {
			wp_die( 'Yetkiniz yok.' );
		}
		check_admin_referer( 'pixelstore_seed' );
		$count = PixelStore_Seed::install( true );
		self::redirect( "$count kayıt yüklendi." );
	}

	public static function handle_purge() {
		if ( ! current_user_can( 'manage_options' ) ) {
			wp_die( 'Yetkiniz yok.' );
		}
		check_admin_referer( 'pixelstore_purge' );
		$count = PixelStore_Seed::purge();
		self::redirect( "$count kayıt silindi." );
	}

	private static function redirect( $notice ) {
		wp_safe_redirect(
			add_query_arg(
				array(
					'page'              => 'pixelstore',
					'pixelstore_notice' => rawurlencode( $notice ),
				),
				admin_url( 'admin.php' )
			)
		);
		exit;
	}
}
