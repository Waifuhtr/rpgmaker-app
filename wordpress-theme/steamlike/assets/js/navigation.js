/**
 * Mobil Navigasyon JS
 */
jQuery(document).ready(function($) {
    var $toggleBtn = $('.sl-mobile-menu-toggle');
    var $navMenu = $('.sl-main-navigation');
    var $overlay = $('.sl-mobile-overlay');
    var $icon = $toggleBtn.find('.dashicons');

    function toggleMenu() {
        $navMenu.toggleClass('active');
        $overlay.toggleClass('active');
        
        // İkonu Menüden Çarplıya (X) çevir
        if ($navMenu.hasClass('active')) {
            $icon.removeClass('dashicons-menu-alt3').addClass('dashicons-no-alt');
        } else {
            $icon.removeClass('dashicons-no-alt').addClass('dashicons-menu-alt3');
        }
    }

    // Butona veya siyah arka plana tıklanırsa menüyü aç/kapat
    $toggleBtn.on('click', toggleMenu);
    $overlay.on('click', toggleMenu);
});
