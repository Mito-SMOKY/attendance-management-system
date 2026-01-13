/**
 * ファイル名表示の更新と、エラーメッセージのクリア
 * HTMLの onchange="updateFileName(this)" から呼ばれます
 */
function updateFileName(input) {
    const display = document.getElementById('fileNameDisplay');
    const errorMsg = document.getElementById('errorMsg');

    if (input.files && input.files.length > 0) {
        if (display) {
            display.textContent = input.files[0].name;
        }
        // ファイルが選ばれたらエラーメッセージを消す
        if (errorMsg) {
            errorMsg.style.display = 'none';
        }
    } else {
        if (display) {
            display.textContent = '選択されていません';
        }
    }
}

/**
 * ページ読み込み完了時の処理
 */
document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');

    // 送信時のチェック
    if (uploadForm) {
        uploadForm.addEventListener('submit', function(event) {
            const input = document.getElementById('csvFile');
            const errorMsg = document.getElementById('errorMsg');

            // ファイルが選択されていない場合
            if (!input.files || input.files.length === 0) {
                // 送信をキャンセル
                event.preventDefault();
                // エラーメッセージを表示
                if (errorMsg) {
                    errorMsg.style.display = 'block';
                }
            }
        });
    }
});