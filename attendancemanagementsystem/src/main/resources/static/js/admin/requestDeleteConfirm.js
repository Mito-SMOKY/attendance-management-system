document.addEventListener('DOMContentLoaded', function() {
    
    // 上位管理者フラグ
    const isSuperAdminMeta = document.querySelector('meta[name="is-super-admin"]');
    const isSuperAdmin = isSuperAdminMeta ? isSuperAdminMeta.content === 'true' : false;

    const confirmBtn = document.getElementById('confirmBtn');

    // モーダル要素
    const modal = document.getElementById('passwordModal');
    const modalSubmitBtn = document.getElementById('submitModalBtn');
    const modalCancelBtn = document.getElementById('cancelModalBtn');
    const passwordInput = document.getElementById('actionPassword');
    
    let pendingRequestData = null;

    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            
            // 1. バリデーション（承認者）※一般管理者のみ
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
    
            // 2. 送信データ準備
            const remarksElem = document.getElementById('remarks');
            const remarks = remarksElem ? remarksElem.value : "";
            
            const checkedBoxes = document.querySelectorAll('.confirm-checkbox:checked');
            const targetIds = Array.from(checkedBoxes).map(cb => parseInt(cb.value));
    
            if (targetIds.length === 0) {
                Swal.fire({
                    icon: 'warning',
                    title: '選択エラー',
                    text: '対象の生徒が選択されていません。',
                    confirmButtonColor: '#539DA4'
                });
                return;
            }

            // データオブジェクト
            pendingRequestData = {
                targetIds: targetIds,
                remarks: remarks,
                approverId: approverId ? parseInt(approverId) : null
            };

            // 3. フロー分岐
            if (isSuperAdmin) {
                openPasswordModal();
            } else {
                showFinalConfirm();
            }
        });
    }

    // --- モーダル制御 ---
    function openPasswordModal() {
        if(modal) {
            passwordInput.value = '';
            modal.classList.add('active');
        }
    }

    if (modalCancelBtn) {
        modalCancelBtn.addEventListener('click', () => modal.classList.remove('active'));
    }

    if (modalSubmitBtn) {
        modalSubmitBtn.addEventListener('click', function() {
            const password = passwordInput.value;
            if (!password) {
                Swal.fire('エラー', 'パスワードを入力してください', 'warning');
                return;
            }
            pendingRequestData.password = password;
            modal.classList.remove('active');
            submitExecute(pendingRequestData);
        });
    }

    // --- 即時実行 (上位管理者) ---
    function submitExecute(data) {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        
        // 即時削除用URL
        const submitUrl = '/admin/request/delete/execute/delete'; 

        const headers = { 'Content-Type': 'application/json' };
        
        // ★修正箇所: ヘッダー名(headerMeta.content)が存在することを確認
        if (tokenMeta && headerMeta && headerMeta.content) {
            headers[headerMeta.content] = tokenMeta.content;
        }

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
                window.location.href = '/admin/userManagement'; 
            });
        })
        .catch(err => {
            Swal.fire({ icon: 'error', title: 'エラー', text: err.message, confirmButtonColor: '#539DA4' });
        });
    }

    // --- 申請 (一般管理者) ---
    function showFinalConfirm() {
        Swal.fire({
            title: '削除申請を行いますか？',
            text: "この操作は取り消せません。",
            icon: 'warning', // 削除なのでWarningアイコン
            showCancelButton: true,
            confirmButtonColor: '#e74c3c', // 赤色
            cancelButtonColor: '#aaa',
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
                window.location.href = '/admin/request/menu'; 
            });
        })
        .catch(() => {
            Swal.fire({ icon: 'error', title: 'エラー', text: '申請に失敗しました。', confirmButtonColor: '#539DA4' });
        });
    }
});