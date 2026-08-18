<?php
/**
 * Oyun İncelemeleri ve Rehberler Sayfası
 */
if ( ! defined( 'ABSPATH' ) ) exit;

// Hangi sayfada olduğumuzu belirliyoruz (Varsayılan: İncelemeler)
$current_page = isset( $_GET['sayfa'] ) && $_GET['sayfa'] === 'rehberler' ? 'rehberler' : 'incelemeler';

// 1. İNCELEME GÖNDERME İŞLEMİ
if ( $_SERVER['REQUEST_METHOD'] === 'POST' && isset( $_POST['sl_submit_review'] ) ) {
	if ( is_user_logged_in() && wp_verify_nonce( $_POST['sl_review_nonce'], 'sl_submit_review_action' ) ) {
		$post_id = get_the_ID();
		$user_id = get_current_user_id();
		$content = sanitize_textarea_field( $_POST['review_content'] );
		$recommended = isset( $_POST['recommended'] ) && $_POST['recommended'] === '1' ? '1' : '0';
		
		$existing_reviews = get_comments( array( 'post_id' => $post_id, 'user_id' => $user_id, 'type' => 'review' ) );
		
		if ( empty( $existing_reviews ) && ! empty( $content ) ) {
			$playtime = SteamLike_Reviews::get_playtime( $user_id, $post_id );
			$playtime_text = $playtime ? $playtime : __( 'Süre kaydedilmedi', 'steamlike' );
			$current_user = wp_get_current_user();
			
			$comment_id = wp_insert_comment( array(
				'comment_post_ID'      => $post_id,
				'comment_author'       => $current_user->display_name,
				'comment_author_email' => $current_user->user_email,
				'comment_content'      => $content,
				'comment_type'         => 'review',
				'user_id'              => $user_id,
				'comment_approved'     => 1,
			) );

			if ( $comment_id ) {
				add_comment_meta( $comment_id, 'sl_recommended', $recommended );
				add_comment_meta( $comment_id, 'sl_playtime', $playtime_text );
				wp_safe_redirect( get_permalink() . 'inceleme/?sayfa=incelemeler&onay=basarili' ); exit;
			}
		}
	}
}

// 2. REHBER GÖNDERME İŞLEMİ
if ( $_SERVER['REQUEST_METHOD'] === 'POST' && isset( $_POST['sl_submit_guide'] ) ) {
	if ( is_user_logged_in() && wp_verify_nonce( $_POST['sl_guide_nonce'], 'sl_submit_guide_action' ) ) {
		$post_id = get_the_ID();
		$user_id = get_current_user_id();
		$guide_content = sanitize_textarea_field( $_POST['guide_content'] );
		
		if ( ! empty( $guide_content ) ) {
			$current_user = wp_get_current_user();
			$guide_id = wp_insert_comment( array(
				'comment_post_ID'      => $post_id,
				'comment_author'       => $current_user->display_name,
				'comment_author_email' => $current_user->user_email,
				'comment_content'      => $guide_content,
				'comment_type'         => 'guide',
				'user_id'              => $user_id,
				'comment_approved'     => 1,
			) );

			if ( $guide_id ) {
                if ( ! empty( $_FILES['guide_image']['name'] ) ) {
                    if ( ! function_exists( 'media_handle_upload' ) ) {
                        require_once( ABSPATH . 'wp-admin/includes/image.php' );
                        require_once( ABSPATH . 'wp-admin/includes/file.php' );
                        require_once( ABSPATH . 'wp-admin/includes/media.php' );
                    }
                    $attachment_id = media_handle_upload( 'guide_image', $post_id );
                    if ( ! is_wp_error( $attachment_id ) ) {
                        $image_url = wp_get_attachment_url( $attachment_id );
                        add_comment_meta( $guide_id, 'sl_guide_image', $image_url );
                    }
                }
				wp_safe_redirect( get_permalink() . 'inceleme/?sayfa=rehberler&rehber_onay=basarili' ); exit;
			}
		}
	}
}

get_header();

while ( have_posts() ) : the_post();
	$post_id = get_the_ID();
	$bg_image = get_post_meta( $post_id, 'game_background_image', true ) ?: get_the_post_thumbnail_url( $post_id, 'full' );
	?>

	<main id="primary" class="site-main sl-single-game">
		<div class="sl-single-bg sl-single-bg-short" style="background-image: url('<?php echo esc_url( $bg_image ); ?>');">
			<div class="sl-single-bg-overlay"></div>
		</div>

		<div class="container sl-appyn-container sl-appyn-container-tight">
			
			<div class="sl-breadcrumb">
				<a href="<?php the_permalink(); ?>"><span class="dashicons dashicons-arrow-left-alt2"></span> <?php esc_html_e( 'Oyuna Dön', 'steamlike' ); ?></a>
				<span class="sl-breadcrumb-sep"> / </span>
				<span class="sl-breadcrumb-current"><?php echo esc_html( get_the_title() ); ?> - <?php echo $current_page === 'rehberler' ? esc_html__( 'Oyuncu Rehberleri', 'steamlike' ) : esc_html__( 'Kullanıcı İncelemeleri', 'steamlike' ); ?></span>
			</div>

			<div class="sl-content-box">
				
                <div class="sl-reviews-hero">
					<img src="<?php echo esc_url( get_the_post_thumbnail_url( $post_id, 'thumbnail' ) ?: STEAMLIKE_URI . 'assets/images/placeholder.jpg' ); ?>" alt="<?php the_title_attribute(); ?>">
					<div>
						<h1><?php the_title(); ?></h1>
						<p><?php esc_html_e( 'Topluluk değerlendirmeleri ve taktik rehberleri.', 'steamlike' ); ?></p>
					</div>
				</div>

                <div class="sl-page-tabs">
                    <a href="?sayfa=incelemeler" class="sl-btn <?php echo $current_page === 'incelemeler' ? 'sl-btn-primary' : 'sl-btn-secondary'; ?>">
                        <span class="dashicons dashicons-testimonial"></span> <?php esc_html_e( 'Kullanıcı İncelemeleri', 'steamlike' ); ?>
                    </a>
                    <a href="?sayfa=rehberler" class="sl-btn <?php echo $current_page === 'rehberler' ? 'sl-btn-primary' : 'sl-btn-secondary'; ?>">
                        <span class="dashicons dashicons-book"></span> <?php esc_html_e( 'Oyuncu Rehberleri', 'steamlike' ); ?>
                    </a>
                </div>

                <?php if ( $current_page === 'incelemeler' ) : ?>
                    <div class="sl-review-form-wrapper">
                        <?php if ( is_user_logged_in() ) : 
                            $user_id = get_current_user_id();
                            $has_reviewed = get_comments( array( 'post_id' => $post_id, 'user_id' => $user_id, 'type' => 'review' ) );
                            
                            if ( empty( $has_reviewed ) ) : ?>
                                <form method="post" class="sl-review-form">
                                    <?php wp_nonce_field( 'sl_submit_review_action', 'sl_review_nonce' ); ?>
                                    <h3><?php esc_html_e( 'Bir İnceleme Yazın', 'steamlike' ); ?></h3>
                                    <div class="sl-review-radios">
                                        <p><?php esc_html_e( 'Bu oyunu başkalarına tavsiye eder misiniz?', 'steamlike' ); ?></p>
                                        <label class="sl-radio-btn positive"><input type="radio" name="recommended" value="1" required><span><span class="dashicons dashicons-thumbs-up"></span> Evet</span></label>
                                        <label class="sl-radio-btn negative"><input type="radio" name="recommended" value="0" required><span><span class="dashicons dashicons-thumbs-down"></span> Hayır</span></label>
                                    </div>
                                    <textarea name="review_content" rows="4" placeholder="<?php esc_attr_e( 'Bu oyun hakkında ne düşünüyorsunuz?', 'steamlike' ); ?>" required></textarea>
                                    <button type="submit" name="sl_submit_review" class="sl-btn sl-btn-primary"><?php esc_html_e( 'İncelemeyi Gönder', 'steamlike' ); ?></button>
                                </form>
                            <?php else: ?>
                                <div class="sl-review-alert success"><?php esc_html_e( 'Bu oyun için zaten bir inceleme yazdınız. Teşekkürler!', 'steamlike' ); ?></div>
                            <?php endif;
                        else : ?>
                            <div class="sl-review-alert warning">
                                <span class="dashicons dashicons-lock"></span> <?php esc_html_e( 'İnceleme bırakmak için sisteme giriş yapmalısınız.', 'steamlike' ); ?>
                            </div>
                        <?php endif; ?>
                    </div>

                    <div class="sl-reviews-list">
                        <?php
                        $reviews = get_comments( array( 'post_id' => $post_id, 'type' => 'review', 'status' => 'approve' ) );
                        if ( $reviews ) {
                            foreach ( $reviews as $review ) {
                                $reviewer_id = $review->user_id;
                                $is_recommended = get_comment_meta( $review->comment_ID, 'sl_recommended', true );
                                $playtime = get_comment_meta( $review->comment_ID, 'sl_playtime', true ) ?: __( 'Süre kaydedilmedi', 'steamlike' );
                                $avatar_url = get_user_meta( $reviewer_id, 'sl_custom_avatar', true ) ?: get_avatar_url( $review->comment_author_email, array('size'=>64) );
                                $cover_url = get_user_meta( $reviewer_id, 'sl_cover_image', true );
                                $card_class = $is_recommended === '1' ? 'positive' : 'negative';
                                $icon_class = $is_recommended === '1' ? 'dashicons-thumbs-up' : 'dashicons-thumbs-down';
                                $rec_text   = $is_recommended === '1' ? __( 'Tavsiye Ediliyor', 'steamlike' ) : __( 'Tavsiye Edilmiyor', 'steamlike' );
                                $upvotes = intval( get_comment_meta( $review->comment_ID, 'sl_upvotes', true ) );
                                $downvotes = intval( get_comment_meta( $review->comment_ID, 'sl_downvotes', true ) );
                                $has_voted = isset( $_COOKIE['sl_voted_review_' . $review->comment_ID] );
                                ?>
                                <div class="sl-review-row <?php echo $card_class; ?>">
                                    <div class="sl-review-profile-block">
                                        <div class="sl-review-profile-block-content">
                                            <img src="<?php echo esc_url($avatar_url); ?>" class="sl-review-avatar-medium" alt="<?php echo esc_attr( $review->comment_author ); ?>" loading="lazy" decoding="async">
                                            <span class="sl-review-author-small"><?php echo esc_html( $review->comment_author ); ?></span>
                                            <div class="sl-review-badges-container-vertical">
                                                <?php foreach ( sl_get_user_badges( $reviewer_id ) as $badge ) { echo '<span style="--badge-color: ' . esc_attr($badge['color']) . ';" class="sl-review-mini-badge-neon vertical"><span class="dashicons ' . esc_attr($badge['icon']) . '"></span> ' . esc_html($badge['name']) . '</span>'; } ?>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="sl-review-content-box">
                                        <div class="sl-steam-compact-header-bar" style="<?php echo $cover_url ? 'background-image: url('.esc_url($cover_url).');' : ''; ?>">
                                            <div class="sl-steam-header-overlay"></div>
                                            <div class="sl-steam-compact-thumb-icon"><span class="dashicons <?php echo $icon_class; ?>"></span></div>
                                            <div class="sl-steam-compact-info-text">
                                                <div class="sl-steam-compact-title-text"><?php echo esc_html( $rec_text ); ?></div>
                                                <div class="sl-steam-compact-playtime-text"><span class="dashicons dashicons-clock"></span> <?php echo esc_html( $playtime ); ?></div>
                                            </div>
                                            <div class="sl-steam-compact-date-text">YAYINLANMA: <?php echo get_comment_date( 'j F Y', $review->comment_ID ); ?></div>
                                        </div>
                                        <div class="sl-review-body"><?php echo wpautop( esc_html( $review->comment_content ) ); ?></div>
                                        <div class="sl-review-voting-bar <?php echo $has_voted ? 'voted' : ''; ?>">
                                            <span class="sl-vote-text-small"><?php echo $has_voted ? esc_html__( 'Değerlendirdiniz.', 'steamlike' ) : esc_html__( 'Faydalı oldu mu?', 'steamlike' ); ?></span>
                                            <button class="sl-vote-btn-small" data-id="<?php echo esc_attr( $review->comment_ID ); ?>" data-type="up" <?php echo $has_voted ? 'disabled' : ''; ?>><span class="dashicons dashicons-thumbs-up"></span> Evet <span class="sl-vote-count-up-small"><?php echo $upvotes > 0 ? $upvotes : ''; ?></span></button>
                                            <button class="sl-vote-btn-small" data-id="<?php echo esc_attr( $review->comment_ID ); ?>" data-type="down" <?php echo $has_voted ? 'disabled' : ''; ?>><span class="dashicons dashicons-thumbs-down"></span> Hayır <span class="sl-vote-count-down-small"><?php echo $downvotes > 0 ? $downvotes : ''; ?></span></button>
                                        </div>
                                    </div>
                                </div>
                                <?php
                            }
                        } else { echo '<p class="sl-empty-note">' . esc_html__( 'İlk incelemeyi siz yazın!', 'steamlike' ) . '</p>'; }
                        ?>
                    </div>

                <?php else : ?>
                    <div class="sl-guides-section">
                        <?php if ( is_user_logged_in() ) : ?>
                            <form method="post" enctype="multipart/form-data" class="sl-review-form sl-guide-form">
                                <?php wp_nonce_field( 'sl_submit_guide_action', 'sl_guide_nonce' ); ?>
                                <h4><?php esc_html_e( 'Topluluk İçin Rehber Oluştur', 'steamlike' ); ?></h4>
                                
                                <textarea name="guide_content" rows="5" placeholder="<?php esc_attr_e( 'Taktiklerinizi, gizli bölgeleri veya ipuçlarını detaylıca anlatın...', 'steamlike' ); ?>" required></textarea>
                                
                                <div class="sl-file-drop">
                                    <label><span class="dashicons dashicons-camera"></span> <?php esc_html_e( 'Görsel Ekle (Opsiyonel)', 'steamlike' ); ?></label>
                                    <input type="file" name="guide_image" accept="image/*">
                                </div>

                                <button type="submit" name="sl_submit_guide" class="sl-btn sl-btn-primary"><span class="dashicons dashicons-upload"></span> <?php esc_html_e( 'Rehberi Yayınla', 'steamlike' ); ?></button>
                            </form>
                        <?php else : ?>
                            <div class="sl-review-alert warning">
                                <span class="dashicons dashicons-lock"></span> <?php esc_html_e( 'Rehber eklemek için sisteme giriş yapmalısınız.', 'steamlike' ); ?>
                            </div>
                        <?php endif; ?>

                        <div class="sl-guides-list">
                            <?php
                            $guides = get_comments( array( 'post_id' => $post_id, 'type' => 'guide', 'status' => 'approve' ) );
                            if ( $guides ) {
                                foreach ( $guides as $guide ) {
                                    $guide_image = get_comment_meta( $guide->comment_ID, 'sl_guide_image', true );
                                    $avatar_url = get_user_meta( $guide->user_id, 'sl_custom_avatar', true ) ?: get_avatar_url( $guide->comment_author_email, array('size'=>48) );
                                    ?>
                                    <div class="sl-guide-card">
                                        <div class="sl-guide-header">
                                            <img src="<?php echo esc_url($avatar_url); ?>" alt="<?php echo esc_attr( $guide->comment_author ); ?>" loading="lazy" decoding="async">
                                            <div>
                                                <div class="sl-guide-author-name"><?php echo esc_html( $guide->comment_author ); ?> <span class="sl-guide-badge-tag"><?php esc_html_e( 'Rehber', 'steamlike' ); ?></span></div>
                                                <div class="sl-guide-date"><span class="dashicons dashicons-calendar-alt"></span> <?php echo get_comment_date( 'j F Y', $guide->comment_ID ); ?></div>
                                            </div>
                                        </div>
                                        
                                        <div class="sl-guide-body">
                                            <?php echo wpautop( esc_html( $guide->comment_content ) ); ?>
                                        </div>

                                        <?php if ( $guide_image ) : ?>
                                            <div class="sl-guide-image-wrap">
                                                <img src="<?php echo esc_url( $guide_image ); ?>" alt="<?php echo esc_attr( sprintf( __( '%s rehberi görseli', 'steamlike' ), get_the_title( $post_id ) ) ); ?>" loading="lazy" decoding="async">
                                            </div>
                                        <?php endif; ?>
                                    </div>
                                    <?php
                                }
                            } else {
                                echo '<p class="sl-empty-note">' . esc_html__( 'Bu oyun için henüz bir rehber yazılmamış.', 'steamlike' ) . '</p>';
                            }
                            ?>
                        </div>
                    </div>
                <?php endif; ?>

			</div>
		</div>
	</main>

<?php endwhile; get_footer(); ?>
