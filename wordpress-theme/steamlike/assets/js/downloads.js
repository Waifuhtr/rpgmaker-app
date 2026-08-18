/**
 * İndirme Sayacı Tetikleyici
 */
jQuery(document).ready(function($) {
    $('.sl-track-download').on('click', function() {
        var $btn = $(this);
        var postId = $btn.data('post-id');
        var $counterSpan = $('.sl-download-count-val'); // Ekranda sayının yazdığı yer

        // Arka plana tıklama bilgisini gönder
        $.ajax({
            url: steamlike_data.ajax_url,
            type: 'POST',
            data: {
                action: 'sl_track_download',
                security: steamlike_data.nonce,
                post_id: postId
            },
            success: function(response) {
                if (response.success) {
                    // Sayfadaki rakamı anında güncelle
                    $counterSpan.text(response.data.new_count);
                }
            }
        });
    });
});
