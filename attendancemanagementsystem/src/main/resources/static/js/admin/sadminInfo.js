document.addEventListener('DOMContentLoaded', function() {

    // ---------------------------------------------------
    // 0. 初期値の保存（キャンセル時に戻すため）
    // ---------------------------------------------------
    let originalData = {};
    const userNameInput = document.getElementById('userName');
    const userAuthInput = document.getElementById('userAuth');
    
    // 初期値を保存
    if (userNameInput && userAuthInput) {
        originalData = {
            name: userNameInput.value,
            auth: userAuthInput.value
        };
    }

    // ---------------------------------------------------
    // 1. ボタン操作・画面遷移系の処理
    // ---------------------------------------------------

    /**
     * 編集モードの切り替え（トグル）を行う関数
     */
    window.toggleEditMode = function() {
        const btnEdit = document.getElementById('btnEditMode');
        const btnSave = document.getElementById('btnSave');
        const btnDelete = document.querySelector('.btn-delete');
        
        // 現在「編集中」モードかどうかを判定
        const isEditing = btnEdit.classList.contains('editing');

        if (!isEditing) {
            // ▼ 編集モードを開始（ON）
            
            // 1. ボタン表記変更
            btnEdit.innerText = "キャンセル";
            btnEdit.classList.add('editing');

            // 2. 確定ボタン表示、削除ボタン非表示
            if (btnSave) btnSave.style.display = 'inline-block';
            if (btnDelete) btnDelete.style.display = 'none';

            // 3. 入力欄を有効化
            if (userNameInput) {
                userNameInput.disabled = false;
                userNameInput.focus(); // 気が利くUX
            }
            if (userAuthInput) userAuthInput.disabled = false;

        } else {
            // ▼ 編集モードをキャンセル（OFF）

            // 1. 値を初期状態に戻す（リセット）
            if (userNameInput) userNameInput.value = originalData.name;
            if (userAuthInput) userAuthInput.value = originalData.auth;

            // 2. 入力欄を無効化
            if (userNameInput) userNameInput.disabled = true;
            if (userAuthInput) userAuthInput.disabled = true;

            // 3. ボタン表記戻し
            btnEdit.innerText = "編集";
            btnEdit.classList.remove('editing');

            // 4. 確定ボタン非表示、削除ボタン再表示
            if (btnSave) btnSave.style.display = 'none';
            if (btnDelete) btnDelete.style.display = 'inline-block';
            
            // エラー表示などが出ている場合は消すなどの処理があればここに追加
        }
    };

    /**
     * 保存ボタンが押された時の処理
     */
    window.saveUser = function() {
        const form = document.getElementById('detailForm');
        
        // ▼▼▼ 追加機能: ブラウザの標準バリデーションチェック ▼▼▼
        // これがないと、名前が空でも送信されてしまいます
        if (!form.reportValidity()) {
            return; // バリデーションエラーがある場合は送信しない
        }

        if (!confirm('変更を保存しますか？')) {
            return;
        }
        
        // フォームを送信
        form.submit();
    };

    // ---------------------------------------------------
    // 2. 入力制限（リアルタイムバリデーション）
    // ---------------------------------------------------
    
    if (userNameInput) {
        userNameInput.addEventListener('input', function() {
            let val = this.value;

            // 1. 数字を削除 (全角半角)
            val = val.replace(/[0-9０-９]/g, '');

            // 2. 記号・特殊文字を削除 (Unicodeプロパティエスケープ)
            // 名前として不適切な記号を弾きます
            val = val.replace(/[\p{P}\p{S}]/gu, '');

            // 3. 15文字制限
            if (val.length > 15) {
                val = val.slice(0, 15);
            }

            // 値が加工されていれば反映
            if (this.value !== val) {
                this.value = val;
            }
        });
    }

    // ---------------------------------------------------
    // 3. 削除モーダル関連の処理
    // ---------------------------------------------------

    window.openDeleteModal = function() {
        const modal = document.getElementById('deleteModal');
        
        // 表示用のIDと名前を取得
        // ※無効化(disabled)されているinputからもvalueは取得可能です
        const userId = document.getElementById('userId').value;
        const userName = document.getElementById('userName').value;

        // モーダル内の表示を更新
        document.getElementById('modalUserIdDisplay').innerText = userId;
        document.getElementById('modalUserNameDisplay').innerText = userName;
        
        // 削除実行時に送信する隠しフィールドにIDをセット
        const targetIdInput = document.getElementById('deleteTargetId');
        if(targetIdInput) {
            targetIdInput.value = userId;
        }

        // パスワード欄をクリア
        const passInput = document.getElementById('deletePassword');
        if(passInput) passInput.value = '';

        if (modal) {
            modal.style.display = 'flex';
        }
    };

    window.closeDeleteModal = function() {
        const modal = document.getElementById('deleteModal');
        if (modal) {
            modal.style.display = 'none';
        }
    };

    // パスワードの表示/非表示切り替え
    window.togglePasswordVisibility = function() {
        const passInput = document.getElementById('deletePassword');
        const icon = document.querySelector('.toggle-eye');

        if (passInput && icon) {
            if (passInput.type === 'password') {
                passInput.type = 'text';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            } else {
                passInput.type = 'password';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            }
        }
    };

    // モーダルの外側をクリックしたら閉じる
    window.onclick = function(event) {
        const modal = document.getElementById('deleteModal');
        if (event.target == modal) {
            closeDeleteModal();
        }
    };
});