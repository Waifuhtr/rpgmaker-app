/**
 * AJAX Canlı Arama JS
 */
jQuery(document).ready(function($) {
    var searchTimer;
    var $searchForm = $('.sl-search-form');
    var $searchInput = $('.sl-search-field');
    
    // Sonuçların çıkacağı kutuyu inputun altına HTML olarak ekliyoruz
    $searchForm.append('<div class="sl-live-search-results"></div>');
    var $resultsBox = $('.sl-live-search-results');

    $searchInput.on('keyup', function() {
        var keyword = $(this).val();

        clearTimeout(searchTimer);

        if (keyword.length < 2) {
            $resultsBox.removeClass('active').html('');
            return;
        }

        // Sunucuyu yormamak için yazmayı bitirdikten 500ms sonra arar
        searchTimer = setTimeout(function() {
            $resultsBox.html('<div class="sl-ls-loading"><span class="dashicons dashicons-update sl-spin"></span> Aranıyor...</div>').addClass('active');

            $.ajax({
                url: steamlike_data.ajax_url,
                type: 'POST',
                data: {
                    action: 'sl_live_search',
                    security: steamlike_data.nonce,
                    keyword: keyword
                },
                success: function(response) {
                    $resultsBox.html(response);
                }
            });
        }, 500);
    });

    // Boş bir yere tıklayınca arama kutusunu kapat
    $(document).on('click', function(e) {
        if (!$(e.target).closest('.sl-search-form').length) {
            $resultsBox.removeClass('active');
        }
    });

    // Arama kutusuna tekrar tıklayınca sonuçlar geri gelsin
    $searchInput.on('focus', function() {
        if ($(this).val().length >= 2 && $resultsBox.html() !== '') {
            $resultsBox.addClass('active');
        }
    });
});
