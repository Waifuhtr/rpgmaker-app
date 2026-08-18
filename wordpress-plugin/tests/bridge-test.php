<?php
/**
 * PixelStore Bridge testleri (WordPress kurulumu gerektirmez).
 *
 * Kapsam: jeton oturumu ve yetki, alan eşleme, puan aritmetiği, istek listesi, incelemeler ve
 * oylar, indirme/görüntülenme sayaçları, yönetim yazma yolu, filtreleme ve canlı arama.
 *
 * Çalıştırma: php wordpress-plugin/tests/bridge-test.php
 */

require_once __DIR__ . '/wp-stubs.php';

$passed = 0;
$failed = 0;
$group  = '';

function group( $name ) {
	global $group;
	$group = $name;
	echo "\n$name\n";
}

function check( $label, $condition, $detail = '' ) {
	global $passed, $failed;
	if ( $condition ) {
		++$passed;
		echo "  ok   $label\n";
	} else {
		++$failed;
		echo "  FAIL $label" . ( $detail ? "  ($detail)" : '' ) . "\n";
	}
}

/* ================================================================================================
 * Kurgu: sitede zaten yayınlanmış iki oyun, bir taslak, iki kullanıcı
 * ============================================================================================= */

psb_test_add_user( new WP_User( 1, 'rias', 'Rias', array( 'manage_options', 'edit_others_posts' ) ) );
psb_test_add_user( new WP_User( 2, 'gezgin', 'Gezgin Kaya', array( 'read' ) ) );

$genre_rpg     = psb_test_register_term( 'game_genre', 'RPG' );
$genre_action  = psb_test_register_term( 'game_genre', 'Aksiyon' );
$plat_android  = psb_test_register_term( 'game_platform', 'Android', '🤖' );
$plat_pc       = psb_test_register_term( 'game_platform', 'PC' );
$lang_tr       = psb_test_register_term( 'game_language', 'Türkçe', 'https://riaslink.fun/tr.png' );
$status_done   = psb_test_register_term( 'game_status', 'Tamamlandı' );
$dev_rias      = psb_test_register_term( 'game_developer', 'Riaslink Çeviri' );
$tag_gerilim   = psb_test_register_term( 'post_tag', 'gerilim' );

psb_test_register_attachment( 500, 'https://riaslink.fun/wp-content/uploads/kapak.jpg' );
psb_test_register_attachment( 501, 'https://riaslink.fun/wp-content/uploads/ss1.jpg' );
psb_test_register_attachment( 502, 'https://riaslink.fun/wp-content/uploads/ss2.jpg' );

// 1) Yayında oyun — tüm alanlar dolu
$game1 = new WP_Post(
	array(
		'ID'           => 101,
		'post_title'   => 'Gölge Vadisi',
		'post_name'    => 'golge-vadisi',
		'post_content' => "<p>Karanlık bir vadide geçen hikâye.</p><ul><li>Türkçe çeviri</li><li>Tam sürüm</li></ul><p>İyi oyunlar.</p>",
		'post_status'  => 'publish',
		'post_date'    => '2026-05-01 10:00:00',
	)
);
$GLOBALS['db']['posts'][101] = $game1;
$GLOBALS['db']['thumbnails'][101] = 500;
$GLOBALS['db']['post_terms'][101] = array(
	'game_genre'     => array( $genre_rpg, $genre_action ),
	'game_platform'  => array( $plat_android, $plat_pc ),
	'game_language'  => array( $lang_tr ),
	'game_status'    => array( $status_done ),
	'game_developer' => array( $dev_rias ),
	'post_tag'       => array( $tag_gerilim ),
);
update_post_meta( 101, 'game_size', '2.4 GB' );
update_post_meta( 101, 'game_version', 'v1.3' );
update_post_meta( 101, 'game_subtitle', 'Tam Türkçe' );
update_post_meta( 101, 'game_screenshots', '501,502' );
update_post_meta( 101, 'game_download_url', 'https://riaslink.fun/dl/golge.apk' );
update_post_meta( 101, 'game_download_mirror', 'https://mirror.example/golge.apk' );
update_post_meta( 101, 'game_download_password', 'riaslink' );
update_post_meta( 101, 'game_download_count', 40 );
update_post_meta( 101, 'game_view_count', 900 );
update_post_meta( 101, 'sl_user_rating_avg', 4.5 );
update_post_meta( 101, 'sl_user_rating_count', 2 );
update_post_meta( 101, 'sl_user_rating_sum', 9 );
update_post_meta( 101, 'minimum_os', 'Android 8' );
update_post_meta( 101, 'minimum_ram', '2 GB' );
update_post_meta( 101, 'game_featured', '1' );
update_post_meta( 101, 'game_trailer_url', 'https://youtu.be/abc' );

// 2) Yayında oyun — eksik alanlar (görselsiz, puansız): mapper çökmemeli
$game2 = new WP_Post(
	array(
		'ID'          => 102,
		'post_title'  => 'Kırık Fener',
		'post_name'   => 'kirik-fener',
		'post_status' => 'publish',
		'post_date'   => '2026-06-01 10:00:00',
	)
);
$GLOBALS['db']['posts'][102] = $game2;
$GLOBALS['db']['post_terms'][102] = array( 'game_genre' => array( $genre_action ), 'game_platform' => array( $plat_pc ) );
update_post_meta( 102, 'game_download_url', 'https://riaslink.fun/dl/fener.zip' );

// 3) Taslak oyun — yalnızca yöneticiye görünmeli
$game3 = new WP_Post(
	array(
		'ID'          => 103,
		'post_title'  => 'Gizli Proje',
		'post_name'   => 'gizli-proje',
		'post_status' => 'draft',
		'post_date'   => '2026-07-01 10:00:00',
	)
);
$GLOBALS['db']['posts'][103] = $game3;
$GLOBALS['db']['post_terms'][103] = array( 'game_genre' => array( $genre_rpg ) );

/* ================================================================================================ */

group( 'Oturum ve yetki' );

$bad = PSB_Auth::login( 'rias', 'yanlis' );
check( 'hatalı parola reddedilir', is_wp_error( $bad ) );
check( 'hata mesajı kullanıcı adını sızdırmaz', is_wp_error( $bad ) && 'Kullanıcı adı veya parola hatalı.' === $bad->message );

$admin_login = PSB_Auth::login( 'rias', 'gizli-parola' );
check( 'yönetici girişi jeton döner', is_array( $admin_login ) && ! empty( $admin_login['token'] ) );
check( 'oturum yükü admin rolü taşır', 'admin' === $admin_login['session']['role'] );

$user_login = PSB_Auth::login( 'gezgin', 'gizli-parola' );
check( 'standart kullanıcı user rolü alır', 'user' === $user_login['session']['role'] );

$req = new WP_REST_Request( array(), array( 'authorization' => 'Bearer ' . $admin_login['token'] ) );
check( 'geçerli jeton kullanıcıyı çözer', PSB_Auth::authenticate( $req ) instanceof WP_User );

$stolen = new WP_REST_Request( array(), array( 'authorization' => 'Bearer 2.' . explode( '.', $admin_login['token'], 2 )[1] ) );
check( 'jeton başka kullanıcıya devredilemez', is_wp_error( PSB_Auth::authenticate( $stolen ) ) );

$fallback = new WP_REST_Request( array(), array( 'x_pixelstore_token' => $user_login['token'] ) );
check( 'X-PixelStore-Token yedeği çalışır', PSB_Auth::authenticate( $fallback ) instanceof WP_User );

$meta_dump = json_encode( get_user_meta( 1, '_psb_tokens', true ) );
$raw       = explode( '.', $admin_login['token'], 2 )[1];
check( 'sunucuda düz jeton saklanmaz', false === strpos( $meta_dump, $raw ) );

group( 'Alan eşleme (mevcut site verisi)' );

$detail = PSB_Mapper::detail( $game1, 2 );

check( 'id slug olarak gelir', 'golge-vadisi' === $detail['id'] );
check( 'başlık', 'Gölge Vadisi' === $detail['title'] );
check( 'alt başlık meta"dan', 'Tam Türkçe' === $detail['subtitle'] );
check( 'boyut serbest metin korunur', '2.4 GB' === $detail['sizeLabel'], $detail['sizeLabel'] );
check( 'kapak gerçek görsel adresi', false !== strpos( $detail['coverUrl'], 'kapak' ), $detail['coverUrl'] );
check( 'geliştirici taksonomiden', 'Riaslink Çeviri' === $detail['developer'] );
check( 'platform ilk terim', 'Android' === $detail['platform'] );
check( 'platform listesi tam', array( 'Android', 'PC' ) === $detail['platforms'] );
check( 'tür listesi', array( 'RPG', 'Aksiyon' ) === $detail['genres'] );
check( 'etiketler', array( 'gerilim' ) === $detail['tags'] );
check( 'puan ortalaması', 4.5 === $detail['rating'] );
check( 'indirme sayacı', 40 === $detail['downloadCount'] );
check( 'öne çıkan bayrağı', true === $detail['featured'] );
check( '2 ekran görüntüsü çözüldü', 2 === count( $detail['screenshots'] ) );
check( 'ekran görüntüsünde tam boy adres var', ! empty( $detail['screenshots'][0]['fullUrl'] ) );
check( 'sistem gereksinimi algılandı', true === $detail['requirements']['hasMinimum'] );
check( 'önerilen gereksinim yok', false === $detail['requirements']['hasRecommended'] );
check( 'fragman adresi', 'https://youtu.be/abc' === $detail['trailerUrl'] );
check( 'arşiv şifresi', 'riaslink' === $detail['archivePassword'] );

// HTML -> düz metin
check( 'paragraflar satır atlamasına döner', false !== strpos( $detail['description'], "hikâye.\n" ), json_encode( $detail['description'] ) );
check( 'liste maddeleri korunur', false !== strpos( $detail['description'], '• Türkçe çeviri' ), json_encode( $detail['description'] ) );
check( 'HTML etiketi kalmaz', false === strpos( $detail['description'], '<' ) );
check( 'özet üretildi', '' !== $detail['excerpt'] );

$sparse = PSB_Mapper::detail( $game2, 2 );
check( 'eksik alanlı kayıt çökmez', 'kirik-fener' === $sparse['id'] );
check( 'kapak yoksa boş metin', '' === $sparse['coverUrl'] );
check( 'puan yoksa 0', 0.0 === $sparse['rating'] );
check( 'ekran görüntüsü yoksa boş dizi', array() === $sparse['screenshots'] );
check( 'gereksinim yoksa false', false === $sparse['requirements']['hasMinimum'] );

group( 'Taslak görünürlüğü' );

$as_user  = PSB_Query::games( new WP_REST_Request( array( 'per_page' => 20 ) ), false, 2 );
$as_admin = PSB_Query::games( new WP_REST_Request( array( 'per_page' => 20 ) ), true, 1 );
check( 'kullanıcı 2 yayında oyun görür', 2 === count( $as_user['games'] ), (string) count( $as_user['games'] ) );
check( 'yönetici taslakla 3 oyun görür', 3 === count( $as_admin['games'] ), (string) count( $as_admin['games'] ) );
$user_ids = array_column( $as_user['games'], 'id' );
check( 'taslak kullanıcı yükünde yok', ! in_array( 'gizli-proje', $user_ids, true ) );
check( 'find() kullanıcıya taslak vermez', null === PSB_Query::find( 'gizli-proje', false ) );
check( 'find() yöneticiye taslak verir', null !== PSB_Query::find( 'gizli-proje', true ) );

group( 'Filtre, sıralama, arama' );

$filtered = PSB_Query::games( new WP_REST_Request( array( 'game_platform' => 'android' ) ), false, 2 );
check( 'platform filtresi tek sonuç', 1 === count( $filtered['games'] ), (string) count( $filtered['games'] ) );
check( 'filtre doğru kaydı verdi', 'golge-vadisi' === $filtered['games'][0]['id'] );

$genre_filtered = PSB_Query::games( new WP_REST_Request( array( 'game_genre' => array( 'aksiyon' ) ) ), false, 2 );
check( 'tür filtresi iki sonuç', 2 === count( $genre_filtered['games'] ) );

$featured = PSB_Query::games( new WP_REST_Request( array( 'featured' => '1' ) ), false, 2 );
check( 'öne çıkan filtresi', 1 === count( $featured['games'] ) && 'golge-vadisi' === $featured['games'][0]['id'] );

$by_downloads = PSB_Query::games( new WP_REST_Request( array( 'sort' => 'downloads' ) ), false, 2 );
check( 'indirmeye göre sıralama', 'golge-vadisi' === $by_downloads['games'][0]['id'] );

$newest = PSB_Query::games( new WP_REST_Request( array( 'sort' => 'newest' ) ), false, 2 );
check( 'en yeni sıralaması', 'kirik-fener' === $newest['games'][0]['id'] );

$paged = PSB_Query::games( new WP_REST_Request( array( 'per_page' => 1, 'page' => 2 ) ), false, 2 );
check( 'sayfalama çalışır', 1 === count( $paged['games'] ) && 2 === $paged['pages'] );

check( 'canlı arama tek harfte boş döner', array() === PSB_Query::live_search( 'g' ) );
$search = PSB_Query::live_search( 'gölge' );
check( 'canlı arama eşleşme bulur', 1 === count( $search ) && 'golge-vadisi' === $search[0]['id'] );
check( 'canlı arama yükü hafif', ! isset( $search[0]['description'] ) && isset( $search[0]['coverUrl'] ) );

$taxonomies = PSB_Query::taxonomies();
check( 'taksonomi listesi türleri içerir', isset( $taxonomies['game_genre'] ) );
$android = null;
foreach ( $taxonomies['game_platform'] as $item ) {
	if ( 'android' === $item['slug'] ) {
		$android = $item;
	}
}
check( 'terim emoji ikonu taşınır', $android && '🤖' === $android['icon'] );
$tr = null;
foreach ( $taxonomies['game_language'] as $item ) {
	if ( 'turkce' === $item['slug'] ) {
		$tr = $item;
	}
}
check( 'terim görsel ikonu ayrı alanda', $tr && '' === $tr['icon'] && false !== strpos( $tr['iconUrl'], 'tr.png' ) );

group( 'İstek listesi' );

check( 'başlangıçta liste boş', array() === PSB_Social::favorites( 2 ) );
$fav = PSB_Social::toggle_favorite( 2, 101 );
check( 'ekleme çalışır', true === $fav['favorited'] && 1 === $fav['count'] );
check( 'temanın sl_favorites anahtarına yazılır', array( 101 ) === get_user_meta( 2, 'sl_favorites', true ) );
check( 'kart yükünde favorited true', true === PSB_Mapper::summary( $game1, 2 )['favorited'] );
check( 'başka kullanıcı etkilenmez', false === PSB_Mapper::summary( $game1, 1 )['favorited'] );
PSB_Social::toggle_favorite( 2, 102 );
check( 'liste kartları döner', 2 === count( PSB_Social::favorite_games( 2 ) ) );
$fav_off = PSB_Social::toggle_favorite( 2, 101 );
check( 'çıkarma çalışır', false === $fav_off['favorited'] && 1 === $fav_off['count'] );

group( 'Puanlama (site ile aynı anahtarlar)' );

$rate = PSB_Social::rate( 2, 101, 5 );
check( 'yeni oy sayacı artırır', 3 === $rate['ratingCount'], json_encode( $rate ) );
check( 'toplam güncellenir', 14 === (int) get_post_meta( 101, 'sl_user_rating_sum', true ) );
check( 'ortalama temanın anahtarında', 4.7 === (float) get_post_meta( 101, 'sl_user_rating_avg', true ), (string) get_post_meta( 101, 'sl_user_rating_avg', true ) );
check( 'kullanıcının oyu okunur', 5 === PSB_Social::user_rating( 2, 101 ) );

$revote = PSB_Social::rate( 2, 101, 1 );
check( 'oy değişince sayaç artmaz', 3 === $revote['ratingCount'] );
check( 'oy değişince toplam düzeltilir', 10 === (int) get_post_meta( 101, 'sl_user_rating_sum', true ), (string) get_post_meta( 101, 'sl_user_rating_sum', true ) );
check( 'geçersiz puan reddedilir', is_wp_error( PSB_Social::rate( 2, 101, 9 ) ) );
check( 'sıfır puan reddedilir', is_wp_error( PSB_Social::rate( 2, 101, 0 ) ) );

group( 'İncelemeler ve oylar' );

$user  = get_user_by( 'id', 2 );
$owner = get_user_by( 'id', 1 );

check( 'çok kısa inceleme reddedilir', is_wp_error( PSB_Social::add_review( $user, 101, 'ok', true ) ) );
$review = PSB_Social::add_review( $user, 101, 'Çeviri gerçekten iyi olmuş, akıcı oynanıyor.', true );
check( 'inceleme eklenir', is_array( $review ) && ! empty( $review['id'] ) );
check( 'ikinci inceleme engellenir', is_wp_error( PSB_Social::add_review( $user, 101, 'Bir daha yazayım.', false ) ) );
check( 'has_reviewed doğru', true === PSB_Social::has_reviewed( 2, 101 ) );

$reviews = PSB_Social::reviews( 101, 1 );
check( 'inceleme listelenir', 1 === count( $reviews ) );
check( 'tavsiye bayrağı', true === $reviews[0]['recommended'] );
check( 'yorum düz metin', false === strpos( $reviews[0]['content'], '<' ) );
check( 'temanın comment_type=review kaydı', 'review' === get_comment( $review['id'] )->comment_type );
check( 'sl_recommended meta yazıldı', '1' === get_comment_meta( $review['id'], 'sl_recommended', true ) );
check( 'mine bayrağı sahibi için doğru', true === PSB_Social::reviews( 101, 2 )[0]['mine'] );

check( 'kendi incelemesine oy veremez', is_wp_error( PSB_Social::vote_review( 2, $review['id'], 'up' ) ) );
$vote = PSB_Social::vote_review( 1, $review['id'], 'up' );
check( 'başkası oy verebilir', is_array( $vote ) && 1 === $vote['upvotes'] );
check( 'temanın sl_upvotes anahtarı', 1 === (int) get_comment_meta( $review['id'], 'sl_upvotes', true ) );
check( 'aynı kullanıcı tekrar oy veremez', is_wp_error( PSB_Social::vote_review( 1, $review['id'], 'down' ) ) );
check( 'voted bayrağı oy verene true', true === PSB_Social::reviews( 101, 1 )[0]['voted'] );

check( 'başkasının incelemesi silinemez', is_wp_error( PSB_Social::delete_review( get_user_by( 'id', 2 ), 999999 ) ) );
$other_review = PSB_Social::add_review( $owner, 102, 'Kısa ama tatlı bir oyun.', true );
check( 'yönetici başkasının incelemesini silebilir', true === PSB_Social::delete_review( $owner, $other_review['id'] ) );

group( 'Sayaçlar' );

$dl = PSB_Social::record_download( 101, false );
check( 'indirme sayacı artar', 41 === $dl['downloadCount'] );
check( 'ana indirme adresi döner', 'https://riaslink.fun/dl/golge.apk' === $dl['url'] );
check( 'arşiv şifresi indirmede döner', 'riaslink' === $dl['password'] );
$mirror = PSB_Social::record_download( 101, true );
check( 'yedek adres döner', 'https://mirror.example/golge.apk' === $mirror['url'] );
check( 'yedek indirme de sayılır', 42 === $mirror['downloadCount'] );
check( 'linki olmayan kayıt hata döner', is_wp_error( PSB_Social::record_download( 103, false ) ) );
check( 'görüntülenme sayacı artar', 901 === PSB_Social::record_view( 101 ) );

group( 'Hata raporu' );

check( 'çok kısa rapor reddedilir', is_wp_error( PSB_Social::add_report( $user, 101, 'yok' ) ) );
$report = PSB_Social::add_report( $user, 101, 'İndirme linki 404 veriyor, kontrol edebilir misiniz?' );
check( 'rapor oluşur', is_array( $report ) && ! empty( $report['id'] ) );
check( 'rapor doğru kayıt tipinde', PSB_REPORT_POST_TYPE === get_post( $report['id'] )->post_type );
check( 'rapor oyuna bağlanır', 101 === (int) get_post_meta( $report['id'], 'reported_game_id', true ) );
check( 'rapor başlığı tema biçiminde', false !== strpos( get_post( $report['id'] )->post_title, '[Gölge Vadisi] Hata Raporu' ) );

group( 'Yönetim: oyun ekleme ve düzenleme' );

$created = PSB_Write::save(
	array(
		'title'                => 'Uygulamadan Eklenen Oyun',
		'description'          => 'Bu kayıt telefondan oluşturuldu.',
		'game_size'            => '780 MB',
		'game_version'         => 'v0.9',
		'game_download_url'    => 'https://riaslink.fun/dl/yeni.apk',
		'game_trailer_url'     => 'javascript:alert(1)',
		'genres'               => array( 'RPG', 'Yeni Tür' ),
		'platforms'            => array( 'Android' ),
		'developer'            => 'Riaslink Çeviri',
		'tags'                 => array( 'test', 'mobil' ),
		'game_featured'        => true,
		'minimum_os'           => 'Android 9',
		'published'            => true,
	),
	null,
	1
);
check( 'kayıt oluşur', is_array( $created ) && 'Uygulamadan Eklenen Oyun' === $created['title'] );
check( 'boyut yazıldı', '780 MB' === $created['sizeLabel'] );
check( 'olmayan tür oluşturuldu', in_array( 'Yeni Tür', $created['genres'], true ), json_encode( $created['genres'] ) );
check( 'geliştirici atandı', 'Riaslink Çeviri' === $created['developer'] );
check( 'etiketler atandı', 2 === count( $created['tags'] ) );
check( 'javascript: şeması düşürüldü', '' === $created['trailerUrl'], $created['trailerUrl'] );
check( 'yayında olarak açıldı', true === $created['published'] );
check( 'sistem gereksinimi yazıldı', 'Android 9' === $created['requirements']['minimum']['os'] );

$updated = PSB_Write::save( array( 'title' => 'Gölge Vadisi', 'game_size' => '2.6 GB' ), 'golge-vadisi', 1 );
check( 'mevcut kayıt güncellenir', '2.6 GB' === $updated['sizeLabel'] );
check( 'güncelleme sayacı ezmez', 42 === $updated['downloadCount'], (string) $updated['downloadCount'] );
check( 'güncelleme puanı ezmez', 3 === $updated['ratingCount'] );

check( 'başlıksız kayıt reddedilir', is_wp_error( PSB_Write::save( array( 'title' => '  ' ), null, 1 ) ) );
check( 'olmayan kayıt güncellenemez', is_wp_error( PSB_Write::save( array( 'title' => 'x' ), 'yok-boyle-bir-sey', 1 ) ) );

check( 'yayından kaldırma', false === PSB_Write::set_published( 'golge-vadisi', false ) );
check( 'taslağa düştü', 'draft' === get_post( 101 )->post_status );
PSB_Write::set_published( 'golge-vadisi', true );
check( 'yayına alma', 'publish' === get_post( 101 )->post_status );

check( 'silme çöpe taşır (kalıcı silmez)', true === PSB_Write::delete( 'kirik-fener' ) && 'trash' === get_post( 102 )->post_status );

group( 'İstatistik ve kullanıcı listesi' );

$stats = PSB_Query::stats();
check( 'toplam indirme toplanır', $stats['totalDownloads'] >= 42, (string) $stats['totalDownloads'] );
check( 'kullanıcı sayısı', 2 === $stats['userCount'] );
check( 'inceleme sayısı', $stats['reviewCount'] >= 1 );
check( 'türlere göre dağılım var', ! empty( (array) $stats['perGenre'] ) );

$users = PSB_Query::users();
check( 'kullanıcı listesi döner', 2 === count( $users ) );
check( 'roller doğru', 'admin' === $users[0]['role'] || 'admin' === $users[1]['role'] );
check( 'parola bilgisi sızmaz', ! isset( $users[0]['password'] ) && ! isset( $users[0]['user_pass'] ) );
check( 'avatar adresi var', ! empty( $users[0]['avatarUrl'] ) );

group( 'Profil fotoğrafı' );

update_user_meta( 2, 'sl_custom_avatar', 'https://riaslink.fun/wp-content/uploads/avatar.jpg' );
check( 'özel avatar tercih edilir', false !== strpos( PSB_Auth::avatar_url( 2 ), 'avatar.jpg' ) );
delete_user_meta( 2, 'sl_custom_avatar' );
check( 'avatar yoksa gravatar', false !== strpos( PSB_Auth::avatar_url( 2 ), 'gravatar' ) );

$session = PSB_Auth::session_payload( get_user_by( 'id', 2 ) );
check( 'oturum yükünde avatar var', ! empty( $session['avatarUrl'] ) );
check( 'oturum yükünde istek listesi sayısı', 1 === $session['favoriteCount'], (string) $session['favoriteCount'] );
check( 'oturum yükünde inceleme sayısı', 1 === $session['reviewCount'] );
check( 'oturum yükü parola taşımaz', ! isset( $session['user_pass'] ) );

echo "\n";
echo str_repeat( '-', 52 ) . "\n";
echo "$passed geçti, $failed başarısız\n";
exit( $failed > 0 ? 1 : 0 );
