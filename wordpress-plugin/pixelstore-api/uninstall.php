<?php
/**
 * Eklenti silinirken temizlik.
 *
 * Kayıtlar bilinçli olarak SİLİNMEZ: kullanıcının kataloğu kaybolmasın. Yalnızca eklentiye ait
 * ayar ve oturum jetonları temizlenir. Kayıtları da silmek istersen wp-admin → PixelStore
 * ekranındaki "Tüm kayıtları sil" düğmesini eklentiyi kaldırmadan önce kullan.
 */

if ( ! defined( 'WP_UNINSTALL_PLUGIN' ) ) {
	exit;
}

delete_option( 'pixelstore_seeded_at' );

// Oturum jetonlarını temizle.
$users = get_users( array( 'fields' => 'ID' ) );
foreach ( $users as $user_id ) {
	delete_user_meta( $user_id, '_ps_tokens' );
}
