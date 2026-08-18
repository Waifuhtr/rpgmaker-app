<?php
/**
 * Template Name: Hesap ve Kayıt Sayfası
 */
if ( ! defined( 'ABSPATH' ) ) exit;

$message = ''; $msg_type = '';

if ( isset( $_POST['sl_register'] ) && $_SERVER['REQUEST_METHOD'] == 'POST' ) {
	$username = sanitize_user( $_POST['reg_username'] );
	$email    = sanitize_email( $_POST['reg_email'] );
	$password = $_POST['reg_password'];
	$api_name = sanitize_text_field( $_POST['reg_api_name'] );

	if ( username_exists( $username ) || email_exists( $email ) ) {
		$message = __( 'Bu kullanıcı adı veya e-posta zaten kullanımda.', 'steamlike' ); $msg_type = 'error';
	} else {
		$user_id = wp_create_user( $username, $password, $email );
		if ( ! is_wp_error( $user_id ) ) {
			update_user_meta( $user_id, 'sl_api_username', $api_name );
			wp_set_current_user( $user_id ); wp_set_auth_cookie( $user_id );
			wp_redirect( get_permalink() ); exit;
		} else { $message = $user_id->get_error_message(); $msg_type = 'error'; }
	}
}

if ( isset( $_POST['sl_login'] ) && $_SERVER['REQUEST_METHOD'] == 'POST' ) {
	$creds = array( 'user_login' => sanitize_user( $_POST['log_username'] ), 'user_password' => $_POST['log_password'], 'remember' => true );
	$user = wp_signon( $creds, false );
	if ( is_wp_error( $user ) ) { $message = __( 'Hatalı kullanıcı adı veya şifre.', 'steamlike' ); $msg_type = 'error'; } 
	else { wp_redirect( get_permalink() ); exit; }
}

if ( isset( $_POST['sl_update_profile'] ) && is_user_logged_in() ) {
	$current_id = get_current_user_id();
	update_user_meta( $current_id, 'sl_api_username', sanitize_text_field( $_POST['up_api_name'] ) );
	
	require_once( ABSPATH . 'wp-admin/includes/file.php' );
	require_once( ABSPATH . 'wp-admin/includes/image.php' );
	require_once( ABSPATH . 'wp-admin/includes/media.php' );

	if ( ! empty( $_FILES['avatar_file']['name'] ) ) {
		$attach_id = media_handle_upload( 'avatar_file', 0 );
		if ( ! is_wp_error( $attach_id ) ) { update_user_meta( $current_id, 'sl_custom_avatar', esc_url_raw( wp_get_attachment_url( $attach_id ) ) ); }
	} elseif ( ! empty( $_POST['up_avatar_url'] ) ) { update_user_meta( $current_id, 'sl_custom_avatar', esc_url_raw( $_POST['up_avatar_url'] ) ); }

	if ( ! empty( $_FILES['cover_file']['name'] ) ) {
		$attach_cover_id = media_handle_upload( 'cover_file', 0 );
		if ( ! is_wp_error( $attach_cover_id ) ) { update_user_meta( $current_id, 'sl_cover_image', esc_url_raw( wp_get_attachment_url( $attach_cover_id ) ) ); }
	} elseif ( ! empty( $_POST['up_cover_url'] ) ) { update_user_meta( $current_id, 'sl_cover_image', esc_url_raw( $_POST['up_cover_url'] ) ); }

	if(empty($message)){ $message = __( 'Profiliniz başarıyla güncellendi.', 'steamlike' ); $msg_type = 'success'; }
}

get_header(); ?>

<main id="primary" class="site-main sl-account-page">
	<div class="sl-account-container">

		<?php if ( $message ) : ?>
			<div class="sl-review-alert sl-mb-30 <?php echo $msg_type === 'error' ? 'warning' : 'success'; ?>"><?php echo esc_html( $message ); ?></div>
		<?php endif; ?>

		<?php if ( ! is_user_logged_in() ) : ?>
			<div class="sl-auth-grid">
                <div class="sl-content-box sl-auth-box">
					<h3><?php esc_html_e( 'Giriş Yap', 'steamlike' ); ?></h3>
					<form method="post" class="sl-form">
						<label><?php esc_html_e( 'Kullanıcı Adı veya E-posta', 'steamlike' ); ?></label> <input type="text" name="log_username" required>
						<label><?php esc_html_e( 'Şifre', 'steamlike' ); ?></label> <input type="password" name="log_password" required>
						<button type="submit" name="sl_login" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-15"><?php esc_html_e( 'Giriş Yap', 'steamlike' ); ?></button>
					</form>
				</div>
				<div class="sl-content-box sl-auth-box sl-auth-box-accent">
					<h3><?php esc_html_e( 'Yeni Hesap Oluştur', 'steamlike' ); ?></h3>
					<form method="post" class="sl-form">
						<label><?php esc_html_e( 'Kullanıcı Adı', 'steamlike' ); ?></label> <input type="text" name="reg_username" required>
						<label><?php esc_html_e( 'E-posta Adresi', 'steamlike' ); ?></label> <input type="email" name="reg_email" required>
						<label><?php esc_html_e( 'Şifre', 'steamlike' ); ?></label> <input type="password" name="reg_password" required>
						<label class="sl-label-accent"><span class="dashicons dashicons-key"></span> <?php esc_html_e( 'Sistem Anahtar Adı', 'steamlike' ); ?></label>
						<input type="text" name="reg_api_name" required class="sl-input-accent">
						<button type="submit" name="sl_register" class="sl-btn sl-btn-primary sl-btn-block sl-mt-15"><?php esc_html_e( 'Kayıt Ol', 'steamlike' ); ?></button>
					</form>
				</div>
			</div>

		<?php else : 
			$current_user = wp_get_current_user();
			$api_name = get_user_meta( $current_user->ID, 'sl_api_username', true );
			$avatar_url = get_user_meta( $current_user->ID, 'sl_custom_avatar', true );
            $cover_url = get_user_meta( $current_user->ID, 'sl_cover_image', true );
            // Kullanıcının herkese açık profil linkini oluşturur
            $public_profile_url = home_url( '/author/' . $current_user->user_login ); 
			?>
			
            <div class="sl-content-box sl-profile-settings-card">
				<div class="sl-profile-settings-head">
					<div class="sl-profile-settings-cover" style="background-image:url('<?php echo esc_url($cover_url); ?>');"></div>
					<img src="<?php echo esc_url( $avatar_url ?: get_avatar_url( $current_user->ID, array('size'=>100) ) ); ?>" class="sl-profile-settings-avatar" alt="<?php echo esc_attr( $current_user->display_name ); ?>">
					<h2 class="sl-profile-settings-name"><?php echo esc_html( $current_user->display_name ); ?></h2>
				</div>

				<form method="post" enctype="multipart/form-data" class="sl-form">
					<label><?php esc_html_e( 'Avatar (Profil Resmi) Seç', 'steamlike' ); ?></label>
					<input type="file" name="avatar_file" accept="image/*">
					<input type="url" name="up_avatar_url" value="<?php echo esc_url( $avatar_url ); ?>" placeholder="Veya Avatar URL'si girin...">
					
					<label class="sl-mt-15"><?php esc_html_e( 'Kapak Fotoğrafı (Arka Plan) Seç', 'steamlike' ); ?></label>
					<input type="file" name="cover_file" accept="image/*">
					<input type="url" name="up_cover_url" value="<?php echo esc_url( $cover_url ); ?>" placeholder="Veya Kapak URL'si girin...">

					<label class="sl-label-accent sl-mt-15"><span class="dashicons dashicons-key"></span> <?php esc_html_e( 'Sistem Anahtar Adı', 'steamlike' ); ?></label>
					<input type="text" name="up_api_name" value="<?php echo esc_attr( $api_name ); ?>" class="sl-input-accent">
					
					<button type="submit" name="sl_update_profile" class="sl-btn sl-btn-primary sl-btn-block sl-mt-20"><?php esc_html_e( 'Ayarları Kaydet', 'steamlike' ); ?></button>
                    
                    <a href="<?php echo esc_url( $public_profile_url ); ?>" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-account-link-btn">
                        <span class="dashicons dashicons-visibility"></span> <?php esc_html_e( 'Herkese Açık Profilimi Gör', 'steamlike' ); ?>
                    </a>

					<a href="<?php echo wp_logout_url( home_url() ); ?>" class="sl-btn sl-btn-secondary sl-btn-block sl-mt-10 sl-logout-btn"><?php esc_html_e( 'Çıkış Yap', 'steamlike' ); ?></a>
				</form>
			</div>

		<?php endif; ?>
	</div>
</main>
<?php get_footer(); ?>
