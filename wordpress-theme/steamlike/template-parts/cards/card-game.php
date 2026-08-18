<?php
/**
 * Oyun Kartı Şablonu (Özelleştirici Bağlantılı - 5 Tasarım & Dinamik Durum + İmza Varsayılan Tasarım)
 */
$post_id = get_the_ID();
$size = get_post_meta( $post_id, 'game_size', true ) ?: 'Bilinmiyor';
$rating = get_post_meta( $post_id, 'sl_user_rating_avg', true ) ?: '0.0';
$thumbnail = get_the_post_thumbnail_url( $post_id, 'medium' ) ?: STEAMLIKE_URI . 'assets/images/placeholder.jpg';
$version = get_post_meta( $post_id, 'game_version', true );

$statuses = get_the_terms( $post_id, 'game_status' );
$status_text = ( $statuses && ! is_wp_error( $statuses ) ) ? $statuses[0]->name : '';

$card_style = get_theme_mod( 'sl_card_style', 'default' );
?>

<a href="<?php the_permalink(); ?>" class="sl-card-game sl-card-style-<?php echo esc_attr( $card_style ); ?>">
	
	<?php if ( $card_style === 'horizontal' ) : ?>
		<div class="sl-card-thumbnail">
            <?php if ( $status_text ) echo '<span class="sl-card-status">' . esc_html( $status_text ) . '</span>'; ?>
            <img src="<?php echo esc_url( $thumbnail ); ?>" alt="<?php the_title_attribute(); ?>" loading="lazy" decoding="async">
        </div>
		<div class="sl-card-content">
			<h2 class="sl-card-title"><?php the_title(); ?></h2>
			<div class="sl-card-excerpt"><?php echo wp_trim_words( get_the_excerpt(), 12, '...' ); ?></div>
			<div class="sl-card-meta">
				<span class="sl-meta-size"><span class="dashicons dashicons-download"></span> <?php echo esc_html( $size ); ?></span>
				<span class="sl-meta-rating"><span class="dashicons dashicons-star-filled"></span> <?php echo esc_html( $rating ); ?></span>
			</div>
		</div>

	<?php elseif ( $card_style === 'minimal' ) : ?>
		<div class="sl-card-thumbnail">
            <?php if ( $status_text ) echo '<span class="sl-card-status">' . esc_html( $status_text ) . '</span>'; ?>
			<img src="<?php echo esc_url( $thumbnail ); ?>" alt="<?php the_title_attribute(); ?>" loading="lazy" decoding="async">
			<div class="sl-card-overlay">
				<h2 class="sl-card-title"><?php the_title(); ?></h2>
				<div class="sl-card-meta">
					<span class="sl-meta-rating"><span class="dashicons dashicons-star-filled"></span> <?php echo esc_html( $rating ); ?></span>
				</div>
			</div>
		</div>

	<?php elseif ( $card_style === 'compact' ) : ?>
		<div class="sl-card-thumbnail"><img src="<?php echo esc_url( $thumbnail ); ?>" alt="<?php the_title_attribute(); ?>" loading="lazy" decoding="async"></div>
		<div class="sl-card-content">
			<h2 class="sl-card-title"><?php the_title(); ?></h2>
			<div class="sl-compact-info"><?php echo esc_html( $version ?: 'v1.0' ); ?> | <?php echo esc_html( $size ); ?></div>
		</div>
		<div class="sl-compact-rating"><?php echo esc_html( $rating ); ?></div>

    <?php elseif ( $card_style === 'modern_glass' ) : 
        // ========================================================
        // TASARIM 5: MODERN KUTU
        // ========================================================
        $languages = get_the_terms( $post_id, 'game_language' );
        $lang_text = ( ! empty( $languages ) && ! is_wp_error( $languages ) ) ? $languages[0]->name : 'TR'; 
        
        $platforms = get_the_terms( $post_id, 'game_platform' );
        $platform_text = ( ! empty( $platforms ) && ! is_wp_error( $platforms ) ) ? $platforms[0]->name : 'Android';
        
        $ver_text = $version ?: 'v1.0';

        // Toplam Süreyi Çek ve Formata Çevir (Saat/Dakika)
        $total_minutes = get_post_meta( $post_id, 'sl_total_playtime', true ) ?: 0;
        $total_hours = floor($total_minutes / 60);
        if ($total_hours > 0) {
            $play_text = $total_hours . ' Saat';
        } elseif ($total_minutes > 0) {
            $play_text = $total_minutes . ' Dk';
        } else {
            $play_text = 'Yeni'; // Hiç oynanmamışsa
        }
    ?>
        <div class="sl-card-thumbnail">
            <img src="<?php echo esc_url( $thumbnail ); ?>" alt="<?php the_title_attribute(); ?>" loading="lazy" decoding="async">
        </div>
        <div class="sl-card-content sl-modern-content">
            
            <div class="sl-modern-pills">
                <span class="sl-pill"><span class="sl-dot sl-dot-platform">●</span> <?php echo esc_html( $platform_text ); ?></span>
                <span class="sl-pill"><span class="sl-dot sl-dot-lang">●</span> <?php echo esc_html( $lang_text ); ?></span>
            </div>
            
            <h2 class="sl-card-title"><?php the_title(); ?></h2>
            
            <div class="sl-modern-version">
                <span class="sl-version-badge"><?php echo esc_html( $ver_text ); ?></span>
            </div>

            <div class="sl-modern-footer">
                <span class="sl-mf-item" title="Kullanıcı Puanı"><span class="dashicons dashicons-star-filled sl-mf-icon-rating"></span> <?php echo esc_html( $rating ); ?></span>
                <span class="sl-mf-item" title="Toplam Oynanma Süresi"><span class="dashicons dashicons-clock sl-mf-icon-time"></span> <?php echo esc_html( $play_text ); ?></span>
            </div>
        </div>

	<?php else : 
		// ========================================================
		// TASARIM 1 (VARSAYILAN): İMZA HUD KARTI
		// ========================================================
		$languages = get_the_terms( $post_id, 'game_language' );
		$lang_text = ( ! empty( $languages ) && ! is_wp_error( $languages ) ) ? $languages[0]->name : '';

		$platforms = get_the_terms( $post_id, 'game_platform' );
		$platform_text = ( ! empty( $platforms ) && ! is_wp_error( $platforms ) ) ? $platforms[0]->name : '';

		$ver_text = $version ?: 'v1.0';
	?>
		<div class="sl-card-thumbnail sl-hud-frame">
			<span class="sl-hud-corner sl-hud-tr"></span>
			<span class="sl-hud-corner sl-hud-bl"></span>
			<img src="<?php echo esc_url( $thumbnail ); ?>" alt="<?php the_title_attribute(); ?>" loading="lazy" decoding="async">
			<span class="sl-card-scrim"></span>
			<?php if ( $status_text ) : ?><span class="sl-card-status"><?php echo esc_html( $status_text ); ?></span><?php endif; ?>
			<span class="sl-card-rating-chip"><span class="dashicons dashicons-star-filled"></span><?php echo esc_html( $rating ); ?></span>
		</div>
		<div class="sl-card-content">
			<h2 class="sl-card-title"><?php the_title(); ?></h2>
			<div class="sl-card-tagrow">
				<?php if ( $platform_text ) : ?><span class="sl-pill"><span class="dashicons dashicons-laptop"></span><?php echo esc_html( $platform_text ); ?></span><?php endif; ?>
				<?php if ( $lang_text ) : ?><span class="sl-pill"><span class="dashicons dashicons-translation"></span><?php echo esc_html( $lang_text ); ?></span><?php endif; ?>
				<span class="sl-version-badge sl-mono"><?php echo esc_html( $ver_text ); ?></span>
			</div>
			<div class="sl-modern-footer">
				<span class="sl-mf-item sl-mono"><span class="dashicons dashicons-media-archive"></span><?php echo esc_html( $size ); ?></span>
			</div>
		</div>
	<?php endif; ?>

</a>
