/**
 * AJAX Rating System
 */
jQuery(document).ready(function($) {
    var $ratingWrap = $('.sl-rating-stars');
    var $stars = $ratingWrap.find('.sl-star');
    var $msgBox = $('.sl-rating-msg');

    // Yıldızların üzerine gelince (Hover) görsel efekt
    $stars.on('mouseenter', function() {
        if ($ratingWrap.hasClass('sl-voted')) return;
        
        var ratingValue = $(this).data('rating');
        $stars.each(function() {
            if ($(this).data('rating') <= ratingValue) {
                $(this).removeClass('dashicons-star-empty').addClass('dashicons-star-filled active');
            } else {
                $(this).removeClass('dashicons-star-filled active').addClass('dashicons-star-empty');
            }
        });
    });

    // Fareyi çekince (Mouseleave) orijinal ortalamaya dön
    $ratingWrap.on('mouseleave', function() {
        if ($ratingWrap.hasClass('sl-voted')) return;
        
        var currentAvg = Math.round($ratingWrap.data('avg'));
        $stars.each(function() {
            if ($(this).data('rating') <= currentAvg) {
                $(this).removeClass('dashicons-star-empty').addClass('dashicons-star-filled');
            } else {
                $(this).removeClass('dashicons-star-filled active').addClass('dashicons-star-empty');
            }
        });
    });

    // Yıldıza tıklandığında (Oy verme işlemi)
    $stars.on('click', function() {
        if ($ratingWrap.hasClass('sl-voted')) return;

        var ratingValue = $(this).data('rating');
        var postId = $ratingWrap.data('post-id');

        $ratingWrap.css('opacity', '0.5');

        $.ajax({
            url: steamlike_data.ajax_url,
            type: 'POST',
            data: {
                action: 'sl_rate_game',
                security: steamlike_data.nonce,
                post_id: postId,
                rating: ratingValue
            },
            success: function(response) {
                $ratingWrap.css('opacity', '1');
                
                if (response.success) {
                    $ratingWrap.addClass('sl-voted');
                    $msgBox.html('<span style="color: #10b981;">' + response.data.message + '</span>');
                    // Yeni ortalamayı arayüze yansıt
                    $('.sl-rating-avg-text').text(response.data.new_avg + ' / 5 (' + response.data.new_count + ' Oy)');
                } else {
                    $msgBox.html('<span style="color: #ef4444;">' + response.data + '</span>');
                }
            },
            error: function() {
                $ratingWrap.css('opacity', '1');
                $msgBox.html('<span style="color: #ef4444;">' + steamlike_data.error_msg + '</span>');
            }
        });
    });
});
