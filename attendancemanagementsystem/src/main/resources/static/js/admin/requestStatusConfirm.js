document.addEventListener('DOMContentLoaded', function() {
    
    // 上位管理者かどうかのフラグ取得
    const isSuperAdminMeta = document.querySelector('meta[name="is-super-admin"]');
    const isSuperAdmin = isSuperAdminMeta ? isSuperAdminMeta.content === 'true' : false;

    const confirmBtn = document.getElementById('confirmBtn');

    // モーダル関連要素
    const modal = document.getElementById('passwordModal');
    const modalSubmitBtn = document.getElementById('submitModalBtn');
    const modalCancelBtn = document.getElementById('cancelModalBtn');
    const passwordInput = document.getElementById('actionPassword');
    
    // 送信データを一時保持する変数
    let pendingRequestData = null;

    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            // 1. バリデーション（変更後ステータス）
            const statusSelect = document.getElementById('targetStatus');
            const targetStatusId = statusSelect ? statusSelect.value : null;

            if (!targetStatusId) {
                Swal.fire({
                    icon: 'warning',
                    title: '入力エラー',
                    text: '変更後のステータスを選択してください。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }

            // 2. バリデーション（承認者）※一般管理者のみ必須
            let approverId = null;
            if (!isSuperAdmin) {
                const approverSelect = document.getElementById('approver');
                approverId = approverSelect ? approverSelect.value : null;
                
                if (!approverId) {
                    Swal.fire({
                        icon: 'warning',
                        title: '入力エラー',
                        text: '申請先（承認者）を選択してください。',
                        confirmButtonColor: '#539DA4'
                    });
                    return;
                }
            }
    
            // 3. 送信データの準備
            const remarksElem = document.getElementById('remarks');
            const remarks = remarksElem ? remarksElem.value : "";
            
            // チェックされた生徒IDを取得
            const checkedBoxes = document.querySelectorAll('.confirm-checkbox:checked');
            const targetIds = Array.from(checkedBoxes).map(cb => parseInt(cb.value));
    
            if (targetIds.length === 0) {
                Swal.fire({
                    icon: 'warning',
                    title: '選択エラー',
                    text: '対象の生徒が選択されていません。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }

            // 共通データオブジェクト作成
            pendingRequestData = {
                targetIds: targetIds,
                targetStatusId: parseInt(targetStatusId),
                remarks: remarks,
                approverId: approverId ? parseInt(approverId) : null
            };

            // 4. フロー分岐
            if (isSuperAdmin) {
                // 上位管理者: パスワードモーダルを開く
                openPasswordModal();
            } else {
                // 一般管理者: 最終確認ダイアログを表示
                showFinalConfirm();
            }
        });
    }

    // --- モーダル制御関数 (上位管理者用) ---
    function openPasswordModal() {
        if(modal) {
            passwordInput.value = ''; // 入力欄クリア
            modal.classList.add('active'); // 表示
        }
    }

    if (modalCancelBtn) {
        modalCancelBtn.addEventListener('click', function() {
            modal.classList.remove('active'); // 非表示
        });
    }

    if (modalSubmitBtn) {
        modalSubmitBtn.addEventListener('click', function() {
            const password = passwordInput.value;
            if (!password) {
                Swal.fire('エラー', 'パスワードを入力してください', 'warning');
                return;
            }
            // パスワードをデータに追加
            pendingRequestData.password = password;
            
            // モーダルを閉じて実行処理へ
            modal.classList.remove('active');
            submitExecute(pendingRequestData);
        });
    }

    // --- 即時実行処理 (上位管理者用) ---
    function submitExecute(data) {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        
        // 即時実行用URL
        const submitUrl = '/admin/request/status/execute/status'; 

        const headers = { 'Content-Type': 'application/json' };
        
        // ★修正箇所: ヘッダー名(headerMeta.content)が存在することを確認
        if (tokenMeta && headerMeta && headerMeta.content) {
            headers[headerMeta.content] = tokenMeta.content;
        }

        // ローディング表示
        Swal.fire({ title: '処理中...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(submitUrl, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(data)
        })
        .then(response => {
            if (response.status === 401) throw new Error('パスワードが間違っています');
            if (!response.ok) throw new Error('サーバーエラーが発生しました');
            return response.text();
        })
        .then(msg => {
            Swal.fire({
                icon: 'success', title: '完了', text: msg, confirmButtonColor: '#539DA4'
            }).then(() => {
                // 完了後はユーザー管理画面トップ等へ
                window.location.href = '/admin/userManagement'; 
            });
        })
        .catch(err => {
            Swal.fire({ icon: 'error', title: 'エラー', text: err.message, confirmButtonColor: '#539DA4' });
            // パスワードエラーの場合は、親切設計として再度モーダルを開くことも可能
        });
    }

    // --- 申請処理 (一般管理者用) ---
    function showFinalConfirm() {
        Swal.fire({
            title: 'ステータス変更申請を行いますか？',
            text: "この操作は取り消せません。",
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#539DA4',
            cancelButtonColor: '#d33',
            confirmButtonText: 'はい、申請します',
            cancelButtonText: 'キャンセル'
        }).then((result) => {
            if (result.isConfirmed) {
                submitRequest();
            }
        });
    }

    function submitRequest() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        
        // 申請用URL (HTMLのmetaタグから取得)
        const submitUrlMeta = document.querySelector('meta[name="submit-url"]');
        const submitUrl = submitUrlMeta ? submitUrlMeta.content : '';

        const headers = { 'Content-Type': 'application/json' };
        
        // ★修正箇所: 一般管理者側も同様に安全対策
        if (tokenMeta && headerMeta && headerMeta.content) {
            headers[headerMeta.content] = tokenMeta.content;
        }

        Swal.fire({ title: '処理中...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(submitUrl, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(pendingRequestData)
        })
        .then(response => {
            if (response.ok) return response.text();
            throw new Error('Server error');
        })
        .then(() => {
            Swal.fire({
                icon: 'success', title: '完了', text: '申請が完了しました。', confirmButtonColor: '#539DA4'
            }).then(() => {
                window.location.href = '/admin/request/menu'; // メニューへ戻る
            });
        })
        .catch(() => {
            Swal.fire({ icon: 'error', title: 'エラー', text: '申請に失敗しました。', confirmButtonColor: '#539DA4' });
        });
    }
});