<?php
/**
 * Eklenti silinirken temizlik.
 *
 * Oyunlar, incelemeler, istek listeleri ve puanlar SİLİNMEZ — hepsi temanın verisi, eklenti
 * yalnızca onları okuyup yazıyordu. Yalnızca eklentiye ait oturum jetonları ve oy sahibi
 * kayıtları temizlenir.
 */

if ( ! defined( 'WP_UNINSTALL_PLUGIN' ) ) {
	exit;
}

global $wpdb;

// Oturum jetonları ve avatar eklenti referansları.
$wpdb->query( "DELETE FROM {$wpdb->usermeta} WHERE meta_key IN ('_psb_tokens', 'psb_avatar_attachment')" );

// Oy sahibi listeleri (toplam/ortalama puan temanın anahtarlarında, onlara dokunulmaz).
$wpdb->query( "DELETE FROM {$wpdb->postmeta} WHERE meta_key = 'psb_rating_voters'" );
$wpdb->query( "DELETE FROM {$wpdb->commentmeta} WHERE meta_key = 'psb_review_voters'" );
