<?php
/**
 * Kullanıcı Açık Profil Sayfası
 */
if ( ! defined( 'ABSPATH' ) ) exit;
get_header();

$curauth = (isset($_GET['author_name'])) ? get_user_by('slug', $author_name) : get_userdata(intval($author));
$api_username = get_user_meta( $curauth->ID, 'sl_api_username', true );
$avatar_url = get_user_meta( $curauth->ID, 'sl_custom_avatar', true ) ?: get_avatar_url( $curauth->ID, array('size'=>150) );

$played_games = get_user_meta( $curauth->ID, 'sl_played_games', true );
$game_playtimes = get_user_meta( $curauth->ID, 'sl_game_playtimes', true ) ?: array();
$favorite_games = get_user_meta( $curauth->ID, 'sl_favorites', true );
?>

<main id="primary" class="site-main sl-profile-page">
	<div class="sl-profile-hero-bg"></div>

	<div class="container sl-appyn-container sl-appyn-container-profile">

		<?php sl_breadcrumb(); ?>

		<div class="sl-appyn-box sl-profile-box">
			<div class="sl-profile-avatar">
				<img src="<?php echo esc_url( $avatar_url ); ?>" class="sl-profile-avatar-img" alt="<?php echo esc_attr( $curauth->display_name ); ?>" loading="lazy" decoding="async">
			</div>
			<div class="sl-profile-info">
				<h1><?php echo esc_html( $curauth->display_name ); ?></h1>
				<div class="sl-profile-badge-row">
					<span class="sl-neon-tag"><span class="dashicons dashicons-admin-users"></span> <?php esc_html_e( 'Oyuncu', 'steamlike' ); ?></span>
					<?php if ( $api_username ) : ?>
						<span class="sl-neon-tag sl-tag-success"><span class="dashicons dashicons-key"></span> <?php echo esc_html( $api_username ); ?></span>
					<?php endif; ?>
				</div>
				<p class="sl-profile-joined"><?php printf( esc_html__( 'Katılım tarihi: %s', 'steamlike' ), date_i18n( 'F Y', strtotime( $curauth->user_registered ) ) ); ?></p>
			</div>
		</div>

		<div class="sl-content-box sl-mt-30">
			<h3 class="sl-profile-section-title">
				<span class="dashicons dashicons-desktop"></span> <?php esc_html_e( 'Oynanan Oyunlar', 'steamlike' ); ?> <span class="sl-profile-section-count">(<?php echo is_array($played_games) ? count($played_games) : '0'; ?>)</span>
			</h3>
			<?php
			if ( ! empty( $played_games ) && is_array( $played_games ) ) {
				$args = array( 'post_type' => 'game', 'post__in' => $played_games, 'posts_per_page' => -1, 'orderby' => 'post__in' );
				$played_query = new WP_Query( $args );
				if ( $played_query->have_posts() ) {
					echo '<div class="post-grid">';
					while ( $played_query->have_posts() ) {
						$played_query->the_post();
						echo '<div class="sl-relative">';
						get_template_part( 'template-parts/cards/card-game' );
						
						// Kartın üzerine oynama süresini basıyoruz (SÜRE EKLENDİ)
						$pid = get_the_ID();
						if ( isset( $game_playtimes[$pid] ) ) {
							echo '<div class="sl-profile-playtime-badge"><span class="dashicons dashicons-clock"></span> ' . esc_html( $game_playtimes[$pid] ) . '</div>';
						}
						echo '</div>';
					}
					echo '</div>';
					wp_reset_postdata();
				}
			} else {
				echo '<p class="sl-profile-empty-note">' . esc_html__( 'Henüz onaylanmış oynama süresi bulunmuyor.', 'steamlike' ) . '</p>';
			}
			?>
		</div>

		<div class="sl-content-box sl-mt-30 sl-wishlist-box">
			<h3 class="sl-profile-section-title sl-profile-section-danger">
				<span class="dashicons dashicons-heart"></span> <?php esc_html_e( 'İstek Listesi', 'steamlike' ); ?> <span class="sl-profile-section-count">(<?php echo is_array($favorite_games) ? count($favorite_games) : '0'; ?>)</span>
			</h3>
			<?php
			if ( ! empty( $favorite_games ) && is_array( $favorite_games ) ) {
				$fav_args = array( 'post_type' => 'game', 'post__in' => $favorite_games, 'posts_per_page' => -1 );
				$fav_query = new WP_Query( $fav_args );
				if ( $fav_query->have_posts() ) {
					echo '<div class="post-grid">';
					while ( $fav_query->have_posts() ) {
						$fav_query->the_post();
						get_template_part( 'template-parts/cards/card-game' );
					}
					echo '</div>';
					wp_reset_postdata();
				}
			} else {
				echo '<p class="sl-profile-empty-note">' . esc_html__( 'İstek listesi şu an boş.', 'steamlike' ) . '</p>';
			}
			?>
		</div>

	</div>
</main>
<?php get_footer(); ?>
