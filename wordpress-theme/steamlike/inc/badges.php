<?php
/**
 * Özel Başarım (Badge) Sistemi ve Admin Paneli
 */
if ( ! defined( 'ABSPATH' ) ) exit;

// 1. Admin Paneline Ayarlar Menüsü Ekle
add_action( 'admin_menu', 'sl_badge_menu' );
function sl_badge_menu() {
	add_submenu_page( 'options-general.php', 'Başarım Ayarları', 'Başarımlar', 'manage_options', 'sl-badges', 'sl_badge_page' );
}

function sl_badge_page() {
	if ( isset( $_POST['sl_save_badges'] ) ) {
		update_option( 'sl_badges', $_POST['sl_badges'] );
		echo '<div class="updated"><p>Başarımlar başarıyla kaydedildi.</p></div>';
	}
	
	// Varsayılan Değerler
	$badges = get_option( 'sl_badges', array(
		'time1_name' => 'Çaylak', 'time1_val' => '1', 'time1_color' => '#3b82f6',
		'time2_name' => 'Ortalama', 'time2_val' => '10', 'time2_color' => '#10b981',
		'time3_name' => 'Usta Oyuncu', 'time3_val' => '50', 'time3_color' => '#f59e0b',
		'game1_name' => 'Oyun Meraklısı', 'game1_val' => '10', 'game1_color' => '#8b5cf6',
	) );
	?>
	<div class="wrap">
		<h1>SteamLike Başarım (Badge) Ayarları</h1>
		<p>Kullanıcıların ne kadar süre veya kaç oyun oynadıktan sonra hangi başarımları kazanacağını buradan düzenleyebilirsiniz.</p>
		
		<form method="POST" style="background:#fff; padding:20px; border-radius:8px; border:1px solid #ccc; max-width: 600px;">
			<h3>Saat Hedefli Başarımlar</h3>
			<p style="margin-bottom:20px;">
				<input type="number" name="sl_badges[time1_val]" value="<?php echo esc_attr($badges['time1_val']); ?>" style="width:70px;"> saat oynayana -> 
				İsim: <input type="text" name="sl_badges[time1_name]" value="<?php echo esc_attr($badges['time1_name']); ?>"> 
				Renk: <input type="color" name="sl_badges[time1_color]" value="<?php echo esc_attr($badges['time1_color']); ?>">
			</p>
			<p style="margin-bottom:20px;">
				<input type="number" name="sl_badges[time2_val]" value="<?php echo esc_attr($badges['time2_val']); ?>" style="width:70px;"> saat oynayana -> 
				İsim: <input type="text" name="sl_badges[time2_name]" value="<?php echo esc_attr($badges['time2_name']); ?>"> 
				Renk: <input type="color" name="sl_badges[time2_color]" value="<?php echo esc_attr($badges['time2_color']); ?>">
			</p>
			<p style="margin-bottom:20px;">
				<input type="number" name="sl_badges[time3_val]" value="<?php echo esc_attr($badges['time3_val']); ?>" style="width:70px;"> saat oynayana -> 
				İsim: <input type="text" name="sl_badges[time3_name]" value="<?php echo esc_attr($badges['time3_name']); ?>"> 
				Renk: <input type="color" name="sl_badges[time3_color]" value="<?php echo esc_attr($badges['time3_color']); ?>">
			</p>

			<hr>
			<h3>Oyun Sayısı Hedefli Başarımlar</h3>
			<p style="margin-bottom:20px;">
				<input type="number" name="sl_badges[game1_val]" value="<?php echo esc_attr($badges['game1_val']); ?>" style="width:70px;"> oyun bitirene -> 
				İsim: <input type="text" name="sl_badges[game1_name]" value="<?php echo esc_attr($badges['game1_name']); ?>"> 
				Renk: <input type="color" name="sl_badges[game1_color]" value="<?php echo esc_attr($badges['game1_color']); ?>">
			</p>
			
			<input type="submit" name="sl_save_badges" class="button button-primary button-large" value="Değişiklikleri Kaydet">
		</form>
	</div>
	<?php
}

// 2. Kullanıcının Kazandığı Başarımları Hesapla ve Döndür
function sl_get_user_badges( $user_id ) {
	$earned = array();
	$badges = get_option( 'sl_badges', array(
		'time1_name' => 'Çaylak', 'time1_val' => '1', 'time1_color' => '#3b82f6',
		'time2_name' => 'Ortalama', 'time2_val' => '10', 'time2_color' => '#10b981',
		'time3_name' => 'Usta Oyuncu', 'time3_val' => '50', 'time3_color' => '#f59e0b',
		'game1_name' => 'Oyun Meraklısı', 'game1_val' => '10', 'game1_color' => '#8b5cf6',
	) );

	// Oynanan oyun sayısı
	$played = get_user_meta( $user_id, 'sl_played_games', true );
	$game_count = is_array( $played ) ? count( $played ) : 0;

	// Toplam Oynama Süresi (Saniyeleri Saate Çevirir)
	$seconds = get_user_meta( $user_id, 'sl_game_seconds', true );
	$total_sec = 0;
	if ( is_array( $seconds ) ) {
		foreach ( $seconds as $s ) { $total_sec += intval( $s ); }
	}
	$total_hours = floor( $total_sec / 3600 );

	// Kazanma Şartları Kontrolü
	if ( $total_hours >= intval( $badges['time1_val'] ) ) {
		$earned[] = array( 'name' => $badges['time1_name'], 'icon' => 'dashicons-clock', 'color' => $badges['time1_color'] );
	}
	if ( $total_hours >= intval( $badges['time2_val'] ) ) {
		$earned[] = array( 'name' => $badges['time2_name'], 'icon' => 'dashicons-awards', 'color' => $badges['time2_color'] );
	}
	if ( $total_hours >= intval( $badges['time3_val'] ) ) {
		$earned[] = array( 'name' => $badges['time3_name'], 'icon' => 'dashicons-superhero', 'color' => $badges['time3_color'] );
	}
	if ( $game_count >= intval( $badges['game1_val'] ) ) {
		$earned[] = array( 'name' => $badges['game1_name'], 'icon' => 'dashicons-games', 'color' => $badges['game1_color'] );
	}

	return $earned;
}
