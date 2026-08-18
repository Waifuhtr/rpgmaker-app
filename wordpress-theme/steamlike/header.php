<?php
/**
 * Tema Header Dosyası
 *
 * @package SteamLike
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
?>
<!DOCTYPE html>
<html <?php language_attributes(); ?>>
<head>
	<meta charset="<?php bloginfo( 'charset' ); ?>">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<link rel="profile" href="https://gmpg.org/xfn/11">
	<?php wp_head(); ?>
</head>

<body <?php body_class( 'steamlike-dark-theme' ); ?>>
<?php wp_body_open(); ?>

<div id="page" class="site-wrapper">
	<a class="skip-link screen-reader-text" href="#primary"><?php esc_html_e( 'İçeriğe atla', 'steamlike' ); ?></a>

	<header id="masthead" class="sl-site-header">
		<div class="container sl-header-inner">
			
			<div class="sl-site-branding">
				<?php
				if ( has_custom_logo() ) {
					the_custom_logo();
				} else {
					echo '<a href="' . esc_url( home_url( '/' ) ) . '" class="sl-text-logo">' . esc_html( get_bloginfo( 'name' ) ) . '</a>';
				}
				?>
			</div>

			<nav id="site-navigation" class="sl-main-navigation">
				<?php
				wp_nav_menu(
					array(
						'theme_location' => 'primary',
						'menu_id'        => 'primary-menu',
						'container'      => false,
						'fallback_cb'    => false,
					)
				);
				?>
			</nav>

			<div class="sl-header-actions">
			
			<?php 
// BİLDİRİM SİSTEMİ
if ( is_user_logged_in() ) : 
    $user_id = get_current_user_id();
    $last_read_raw = get_user_meta( $user_id, 'sl_notifications_last_read', true );
    $last_read = ( ! $last_read_raw ) ? 0 : ( is_numeric($last_read_raw) ? $last_read_raw : strtotime($last_read_raw) );

    $args = array( 
        'post_type' => array( 'post', 'game', 'oyun' ), 
        'posts_per_page' => 5, 
        'post_status' => 'publish', 
        'orderby' => 'date', 
        'order' => 'DESC' 
    );
    $query = new WP_Query( $args );
    
    $unread_count = 0;
    $latest_games = array();
    
    if ( $query->have_posts() ) {
        while ( $query->have_posts() ) {
            $query->the_post();
            $post_time = get_post_time( 'U', false ); 
            $is_unread = ( $post_time > $last_read ); 
            if ( $is_unread ) $unread_count++;
            
            $latest_games[] = array(
                'title'  => get_the_title(),
                'url'    => get_permalink(),
                'image'  => get_the_post_thumbnail_url( get_the_ID(), 'thumbnail' ) ?: STEAMLIKE_URI . 'assets/images/placeholder.jpg',
                'date'   => get_the_date('j F Y'),
                'unread' => $is_unread
            );
        }
    }
    wp_reset_postdata();
?>
    <div class="sl-notification-wrapper">
        <button class="sl-notification-btn" id="sl-bell-btn" aria-label="<?php esc_attr_e( 'Bildirimler', 'steamlike' ); ?>" aria-haspopup="true" aria-expanded="false">
            <span class="dashicons dashicons-bell"></span>
            <?php if ( $unread_count > 0 ) : ?>
                <span class="sl-notification-badge"><?php echo $unread_count; ?></span>
            <?php endif; ?>
        </button>
        
        <div class="sl-notification-dropdown" id="sl-bell-dropdown">
            <div class="sl-nd-header">
                <h4>Yeni Eklenen Oyunlar</h4>
                <?php if ( $unread_count > 0 ) : ?>
                    <button class="sl-mark-read-btn" id="sl-mark-read-action">Tümünü Okundu İşaretle</button>
                <?php endif; ?>
            </div>
            <div class="sl-nd-body">
                <?php if ( ! empty( $latest_games ) ) : ?>
                    <?php foreach ( $latest_games as $game ) : ?>
                        <a href="<?php echo esc_url($game['url']); ?>" class="sl-nd-item <?php echo $game['unread'] ? 'unread' : ''; ?>">
                            <img src="<?php echo esc_url($game['image']); ?>" alt="<?php echo esc_attr( $game['title'] ); ?>" loading="lazy" decoding="async">
                            <div class="sl-nd-info">
                                <div class="sl-nd-title"><?php echo esc_html($game['title']); ?></div>
                                <div class="sl-nd-date"><?php echo esc_html($game['date']); ?></div>
                            </div>
                            <?php if ( $game['unread'] ) : ?><span class="sl-nd-dot"></span><?php endif; ?>
                        </a>
                    <?php endforeach; ?>
                <?php else : ?>
                    <p class="sl-nd-empty">Henüz yeni bir oyun eklenmedi.</p>
                <?php endif; ?>
            </div>
        </div>
    </div>
    
    <script>
    // BİLDİRİM ÇANI İÇİN KIRILMAZ KOD (VANILLA JS)
    document.addEventListener('DOMContentLoaded', function() {
        var bellBtn = document.getElementById('sl-bell-btn');
        var dropdown = document.getElementById('sl-bell-dropdown');
        
        if (bellBtn && dropdown) {
            bellBtn.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                dropdown.classList.toggle('active');
                bellBtn.setAttribute('aria-expanded', dropdown.classList.contains('active') ? 'true' : 'false');
            });
            
            document.addEventListener('click', function(e) {
                if (!e.target.closest('.sl-notification-wrapper')) {
                    dropdown.classList.remove('active');
                    bellBtn.setAttribute('aria-expanded', 'false');
                }
            });

            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    dropdown.classList.remove('active');
                    bellBtn.setAttribute('aria-expanded', 'false');
                }
            });
        }

        // AJAX Okundu İşaretleme
        if (typeof jQuery !== 'undefined') {
            jQuery('#sl-mark-read-action').on('click', function(e) {
                e.preventDefault(); e.stopPropagation();
                var btn = jQuery(this);
                jQuery.ajax({
                    url: '<?php echo admin_url("admin-ajax.php"); ?>',
                    type: 'POST',
                    data: { action: 'sl_mark_notifications_read' },
                    success: function(response) {
                        if(response.success) {
                            jQuery('.sl-notification-badge').fadeOut(200, function() { jQuery(this).remove(); });
                            jQuery('.sl-nd-item').removeClass('unread');
                            jQuery('.sl-nd-dot').fadeOut(200, function() { jQuery(this).remove(); });
                            btn.fadeOut(200, function() { jQuery(this).remove(); });
                        }
                    }
                });
            });
        }
    });
    </script>
<?php endif; ?>
				
				<button type="button" class="sl-search-trigger" id="sl-open-search-modal" aria-label="<?php esc_attr_e( 'Ara', 'steamlike' ); ?>"><span class="dashicons dashicons-search"></span></button>

				<?php if ( ! is_user_logged_in() ) : ?>
					<button class="sl-btn sl-btn-primary sl-header-auth-btn" id="sl-open-auth-modal"><span class="dashicons dashicons-admin-users"></span> <span class="sl-hide-mobile"><?php esc_html_e('Giriş / Kayıt', 'steamlike'); ?></span></button>
				<?php else : 
					$current_id = get_current_user_id();
					$avatar = get_user_meta( $current_id, 'sl_custom_avatar', true ) ?: get_avatar_url( $current_id );
				?>
					<a href="<?php echo esc_url( home_url('/hesabim/') ); ?>" class="sl-header-avatar-link">
						<img src="<?php echo esc_url($avatar); ?>" class="sl-header-avatar-img" alt="<?php esc_attr_e( 'Profil', 'steamlike' ); ?>">
					</a>
				<?php endif; ?>

				<button class="sl-mobile-menu-toggle" aria-controls="primary-menu" aria-expanded="false" aria-label="<?php esc_attr_e( 'Menü', 'steamlike' ); ?>"><span class="dashicons dashicons-menu-alt3"></span></button>
			</div>
		</div>
	</header><div class="sl-mobile-overlay"></div>

	<div class="sl-search-modal-wrapper" id="sl-search-modal">
		<div class="sl-search-modal-bg" id="sl-close-search-modal"></div>
		<div class="sl-search-modal-content">
			<span class="sl-auth-modal-close" id="sl-close-search-btn">&times;</span>
			<h3 class="sl-auth-modal-title"><span class="dashicons dashicons-search"></span> <?php esc_html_e( 'Oyun Ara', 'steamlike' ); ?></h3>
			<form role="search" method="get" class="sl-search-form" action="<?php echo esc_url( home_url( '/' ) ); ?>">
				<input type="hidden" name="post_type" value="game" />
				<input type="search" class="sl-search-field" placeholder="<?php echo esc_attr_x( 'Oyun ara...', 'placeholder', 'steamlike' ); ?>" value="<?php echo get_search_query(); ?>" name="s" required />
				<button type="submit" class="sl-search-submit" aria-label="<?php esc_attr_e( 'Ara', 'steamlike' ); ?>"><span class="dashicons dashicons-search"></span></button>
			</form>
		</div>
	</div>

	<script>
		// ARAMA POPUP'I İÇİN KIRILMAZ KOD (VANILLA JS)
		document.addEventListener('DOMContentLoaded', function() {
			var searchModal = document.getElementById('sl-search-modal');
			var openSearchBtn = document.getElementById('sl-open-search-modal');
			var closeSearchBtns = [document.getElementById('sl-close-search-modal'), document.getElementById('sl-close-search-btn')];
			var searchField = searchModal.querySelector('.sl-search-field');

			if (openSearchBtn) {
				openSearchBtn.addEventListener('click', function() {
					searchModal.classList.add('active');
					setTimeout(function() { if (searchField) searchField.focus(); }, 150);
				});
			}

			closeSearchBtns.forEach(function(btn) {
				if (btn) {
					btn.addEventListener('click', function() { searchModal.classList.remove('active'); });
				}
			});

			document.addEventListener('keydown', function(e) {
				if (e.key === 'Escape' && searchModal.classList.contains('active')) {
					searchModal.classList.remove('active');
				}
			});
		});
	</script>

	<?php if ( ! is_user_logged_in() ) : ?>
	<div class="sl-auth-modal-wrapper" id="sl-auth-modal">
		<div class="sl-auth-modal-bg sl-close-auth-btn"></div>
		
		<div class="sl-auth-modal-content sl-content-box" id="sl-login-box">
			<span class="sl-auth-modal-close sl-close-auth-btn">&times;</span>
			<h3 class="sl-auth-modal-title"><span class="dashicons dashicons-admin-users"></span> <?php esc_html_e( 'Sisteme Katıl', 'steamlike' ); ?></h3>
			<p class="sl-auth-modal-sub"><?php esc_html_e( 'Oyun sürelerini kaydet, inceleme bırak ve başarımlar kazan.', 'steamlike' ); ?></p>
			
			<form action="<?php echo esc_url( home_url('/hesabim/') ); ?>" method="post" class="sl-form" style="margin-top: 20px;">
				<input type="text" name="log_username" placeholder="<?php esc_attr_e( 'Kullanıcı Adı', 'steamlike' ); ?>" required>
				<input type="password" name="log_password" placeholder="<?php esc_attr_e( 'Şifre', 'steamlike' ); ?>" required>
				<div style="text-align: right; margin-top: -10px; margin-bottom: 15px;">
					<a href="#" id="sl-show-forgot-box" class="sl-auth-forgot-link">Şifremi Unuttum?</a>
				</div>
				<button type="submit" name="sl_login" class="sl-btn sl-btn-primary sl-btn-block"><?php esc_html_e( 'Giriş Yap', 'steamlike' ); ?></button>
			</form>
			
			<div class="sl-auth-modal-footer">
				Hesabın yok mu? <a href="<?php echo esc_url( home_url('/hesabim/') ); ?>">Hemen Kayıt Ol</a>
			</div>
		</div>

		<div class="sl-auth-modal-content sl-content-box" id="sl-forgot-box" style="display: none;">
			<span class="sl-auth-modal-close sl-close-auth-btn">&times;</span>
			<h3 class="sl-auth-modal-title" style="color: var(--sl-gold);"><span class="dashicons dashicons-lock"></span> Şifre Sıfırlama</h3>
			<p class="sl-auth-modal-sub">Sisteme kayıtlı e-posta adresinizi girin. Size bir sıfırlama bağlantısı göndereceğiz.</p>
			
			<form id="sl-forgot-password-form" class="sl-form" style="margin-top: 20px;">
				<input type="text" id="sl-reset-login" placeholder="<?php esc_attr_e( 'Kullanıcı Adı veya E-Posta', 'steamlike' ); ?>" required>
				<button type="submit" class="sl-btn sl-btn-block sl-btn-warning">Bağlantı Gönder</button>
			</form>
			<div id="sl-forgot-msg" style="margin-top: 15px; font-size: 13px; text-align: center; display:none;"></div>
			
			<div class="sl-auth-modal-footer">
				<a href="#" id="sl-show-login-box" class="sl-auth-back-link">&larr; Giriş Ekranına Dön</a>
			</div>
		</div>
	</div>

	<script>
		// GİRİŞ/KAYIT PENCERESİ İÇİN KIRILMAZ KOD (VANILLA JS)
		document.addEventListener('DOMContentLoaded', function() {
			var modal = document.getElementById('sl-auth-modal');
			var openBtn = document.getElementById('sl-open-auth-modal');
			var closeBtns = document.querySelectorAll('.sl-close-auth-btn');
			var loginBox = document.getElementById('sl-login-box');
			var forgotBox = document.getElementById('sl-forgot-box');
			var showForgotBtn = document.getElementById('sl-show-forgot-box');
			var showLoginBtn = document.getElementById('sl-show-login-box');

			if (openBtn) {
				openBtn.addEventListener('click', function(e) {
					e.preventDefault();
					modal.classList.add('active');
				});
			}

			closeBtns.forEach(function(btn) {
				btn.addEventListener('click', function() {
					modal.classList.remove('active');
				});
			});

			document.addEventListener('keydown', function(e) {
				if (e.key === 'Escape' && modal.classList.contains('active')) {
					modal.classList.remove('active');
				}
			});

			if (showForgotBtn) {
				showForgotBtn.addEventListener('click', function(e) {
					e.preventDefault();
					loginBox.style.display = 'none';
					forgotBox.style.display = 'block';
				});
			}

			if (showLoginBtn) {
				showLoginBtn.addEventListener('click', function(e) {
					e.preventDefault();
					forgotBox.style.display = 'none';
					loginBox.style.display = 'block';
				});
			}

            // AJAX Kısmı
            if (typeof jQuery !== 'undefined') {
                jQuery('#sl-forgot-password-form').on('submit', function(e) {
                    e.preventDefault();
                    var btn = jQuery(this).find('button');
                    var msgBox = jQuery('#sl-forgot-msg');
                    var loginData = jQuery('#sl-reset-login').val();

                    btn.prop('disabled', true).text('Gönderiliyor...');
                    msgBox.hide().removeClass('success warning');

                    jQuery.ajax({
                        url: '<?php echo admin_url("admin-ajax.php"); ?>',
                        type: 'POST',
                        data: { action: 'sl_ajax_forgot_password', user_login: loginData, security: '<?php echo wp_create_nonce("sl_forgot_nonce"); ?>' },
                        success: function(response) {
                            msgBox.show();
                            if(response.success) {
                                msgBox.css('color', '#34d399').html('&#10004; ' + response.data);
                                jQuery('#sl-forgot-password-form').slideUp();
                            } else {
                                msgBox.css('color', '#f2555a').html('&#10006; ' + response.data);
                                btn.prop('disabled', false).text('Bağlantı Gönder');
                            }
                        }
                    });
                });
            }
		});
	</script>
	<?php endif; ?>
