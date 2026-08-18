// İnceleme Oylama (Upvote / Downvote) Sistemi
document.addEventListener('DOMContentLoaded', function() {
    const voteButtons = document.querySelectorAll('.sl-vote-btn');
    
    voteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const btn = this;
            const commentId = btn.getAttribute('data-id');
            const voteType = btn.getAttribute('data-type');
            
            // Kullanıcı bir kere oy verdikten sonra butonları kitleyelim
            const parent = btn.closest('.sl-review-voting');
            if (parent.classList.contains('voted')) return;
            parent.classList.add('voted');
            parent.style.opacity = '0.6';
            
            const formData = new FormData();
            formData.append('action', 'sl_vote_review');
            formData.append('comment_id', commentId);
            formData.append('vote_type', voteType);

            // sl_ajax_url -> header.php veya functions.php'de tanımlı wp_ajax_url olmalı. 
            // Eğer tanımlı değilse doğrudan '/wp-admin/admin-ajax.php' kullanıyoruz.
            fetch('/wp-admin/admin-ajax.php', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    parent.querySelector('.sl-vote-count-up').innerText = data.data.up > 0 ? data.data.up : '';
                    parent.querySelector('.sl-vote-count-down').innerText = data.data.down > 0 ? data.data.down : '';
                    btn.style.borderColor = voteType === 'up' ? '#3b82f6' : '#ef4444';
                }
            });
        });
    });
});
