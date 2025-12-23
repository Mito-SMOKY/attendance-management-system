document.addEventListener('DOMContentLoaded', function() {
    
    // 要素の取得
    const editButton = document.getElementById('edit-button');
    const saveButton = document.getElementById('save-button');
    const cancelButton = document.getElementById('cancel-button');
    
    const displayName = document.getElementById('display-value');
    const nameInput = document.getElementById('username-input');

    // 初期状態の保存（キャンセル時に戻すため）
    let originalName = "";

    // 編集ボタンクリック時
    if(editButton) {
        editButton.addEventListener('click', function() {
            // 現在の表示名を保存
            originalName = displayName.textContent;
            
            // UI切り替え: 入力モードへ
            toggleEditMode(true);
            
            // 入力欄に現在の値を入れてフォーカス
            nameInput.value = originalName;
            nameInput.focus();
        });
    }

    // キャンセルボタンクリック時
    if(cancelButton) {
        cancelButton.addEventListener('click', function() {
            // UI切り替え: 表示モードへ
            toggleEditMode(false);
            // 値を元に戻す
            nameInput.value = originalName;
        });
    }

    // 保存ボタンクリック時
    if(saveButton) {
        saveButton.addEventListener('click', function() {
            const newName = nameInput.value;

            // バリデーション（空文字チェック）
            if (!newName || newName.trim() === "") {
                alert("名前を入力してください。");
                return;
            }

            // サーバーへ送信 (Fetch API)
            fetch('/api/profile/update-name', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // CSRFトークンが必要な場合は以下を有効化 (metaタグから取得が必要)
                    // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                },
                body: JSON.stringify({ name: newName })
            })
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                throw new Error('Network response was not ok.');
            })
            .then(data => {
                // 成功時の処理
                // 表示を更新
                displayName.textContent = newName;
                originalName = newName;
                
                // UIを戻す
                toggleEditMode(false);
                
                // 必要であれば成功メッセージなどを表示
                // alert("更新しました");
            })
            .catch(error => {
                console.error('Error:', error);
                alert("更新に失敗しました。");
            });
        });
    }

    /**
     * 編集モードと表示モードを切り替える関数
     * @param {boolean} isEdit - trueなら編集モード、falseなら表示モード
     */
    function toggleEditMode(isEdit) {
        if (isEdit) {
            // 編集モード
            displayName.style.display = 'none';
            nameInput.classList.remove('hidden');
            
            editButton.style.display = 'none'; // または classList.add('hidden')
            saveButton.classList.remove('hidden');
            cancelButton.classList.remove('hidden');
        } else {
            // 表示モード
            displayName.style.display = 'block'; // または元のdisplay属性
            nameInput.classList.add('hidden');
            
            editButton.style.display = 'inline-block'; // または classList.remove('hidden')
            saveButton.classList.add('hidden');
            cancelButton.classList.add('hidden');
        }
    }
});