/**
 * Favoriler / İstek Listesi JS
 */
jQuery(document).ready(function($) {
    
    // Ziyaretçiler (Giriş yapmamış olanlar) için sayfa yüklendiğinde LocalStorage kontrolü
    $('.sl-favorite-btn').each(function() {
        var $btn = $(this);
        var postId = $btn.data('post-id');
        var isLoggedIn = $btn.data('logged-in');

        if (!isLoggedIn) {
            var favs = JSON.parse(localStorage.getItem('sl_favorites')) || [];
            if (favs.indexOf(postId) > -1) {
                $btn.addClass('active');
                $btn.find('.dashicons-heart').css('color', '#ef4444');
                $btn.find('.sl-fav-text').text('İstek Listesinden Çıkar');
            }
        }
    });

    // Butona tıklandığında
    $('.sl-favorite-btn').on('click', function(e) {
        e.preventDefault();
        var $btn = $(this);
        var postId = $btn.data('post-id');
        var isLoggedIn = $btn.data('logged-in');

        // Ziyaretçi işlemi (Tarayıcıya kaydet)
        if (!isLoggedIn) {
            var favs = JSON.parse(localStorage.getItem('sl_favorites')) || [];
            var index = favs.indexOf(postId);
            
            if (index > -1) {
                favs.splice(index, 1); // Çıkar
                $btn.removeClass('active');
                $btn.find('.dashicons-heart').css('color', '');
                $btn.find('.sl-fav-text').text('İstek Listesine Ekle');
            } else {
                favs.push(postId); // Ekle
                $btn.addClass('active');
                $btn.find('.dashicons-heart').css('color', '#ef4444');
                $btn.find('.sl-fav-text').text('İstek Listesinden Çıkar');
            }
            localStorage.setItem('sl_favorites', JSON.stringify(favs));
            return;
        }

        // Giriş yapmış kullanıcı işlemi (AJAX ile veritabanına kaydet)
        $btn.css('opacity', '0.5');

        $.ajax({
            url: steamlike_data.ajax_url,
            type: 'POST',
            data: {
                action: 'sl_toggle_favorite',
                security: steamlike_data.nonce,
                post_id: postId
            },
            success: function(response) {
                $btn.css('opacity', '1');
                if (response.success) {
                    if (response.data.status === 'added') {
                        $btn.addClass('active');
                        $btn.find('.dashicons-heart').css('color', '#ef4444');
                        $btn.find('.sl-fav-text').text('İstek Listesinden Çıkar');
                    } else {
                        $btn.removeClass('active');
                        $btn.find('.dashicons-heart').css('color', '');
                        $btn.find('.sl-fav-text').text('İstek Listesine Ekle');
                    }
                }
            }
        });
    });
});
