/**
 * AJAX Filtreleme Sistemi (jQuery)
 */
jQuery(document).ready(function($) {

    var $filterForm = $('#sl-filter-form');
    var $resultsContainer = $('#sl-filter-results');

    // Formdaki herhangi bir checkbox veya select değiştiğinde tetiklenir
    $filterForm.on('change', 'input, select', function(e) {
        e.preventDefault();
        sl_fetch_games();
    });

    function sl_fetch_games() {
        // Formdaki verileri dizi haline getir
        var formData = $filterForm.serializeArray();
        
        // AJAX isteği için gerekli verileri ekle
        formData.push({ name: 'action', value: 'sl_filter_games' });
        formData.push({ name: 'security', value: steamlike_data.nonce });

        // Yükleniyor durumu ekle
        $resultsContainer.html('<div style="grid-column: 1 / -1; text-align: center; padding: 50px;"><span class="dashicons dashicons-update sl-spin" style="font-size: 40px; width:40px; height:40px; color: #3b82f6;"></span><p>Oyunlar filtreleniyor...</p></div>');
        $resultsContainer.css('opacity', '0.5');

        // AJAX İsteği
        $.ajax({
            url: steamlike_data.ajax_url,
            type: 'POST',
            data: formData,
            success: function(response) {
                // Gelen HTML (Oyun kartları) container içine basılır
                $resultsContainer.html(response);
                $resultsContainer.css('opacity', '1');
            },
            error: function() {
                $resultsContainer.html('<p>' + steamlike_data.error_msg + '</p>');
                $resultsContainer.css('opacity', '1');
            }
        });
    }

});
