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
    
    // データ保持用
    let pendingRequestData = null;

    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            // 1. バリデーション（移動先学科）
            const deptSelect = document.getElementById('targetDepartment');
            const targetDepartmentId = deptSelect ? deptSelect.value : null;

            if (!targetDepartmentId) {
                Swal.fire({
                    icon: 'warning',
                    title: '入力エラー',
                    text: '移動先の学科・コースを選択してください。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }

            // 2. バリデーション（承認者）※一般管理者のみ
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
    
            // 3. データ準備 & 重複チェック
            const remarksElem = document.getElementById('remarks');
            const remarks = remarksElem ? remarksElem.value : "";
            
            const checkedBoxes = document.querySelectorAll('.confirm-checkbox:checked');
            const targetIds = [];
            const duplicateStudents = []; // 重複生徒リスト

            checkedBoxes.forEach(cb => {
                targetIds.push(parseInt(cb.value));
                
                // 行から現在の学科IDを取得
                const row = cb.closest('tr');
                const currentDeptId = row.getAttribute('data-current-dept-id');
                
                // 現在の学科と移動先が同じかチェック
                if (currentDeptId && parseInt(currentDeptId) === parseInt(targetDepartmentId)) {
                    // 名前を取得
                    const nameCell = row.querySelector('.student-name'); 
                    if (nameCell) {
                        duplicateStudents.push(nameCell.textContent.trim());
                    }
                }
            });
    
            if (targetIds.length === 0) {
                Swal.fire({
                    icon: 'warning',
                    title: '選択エラー',
                    text: '対象の生徒が選択されていません。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }

            // 送信データの構築
            pendingRequestData = {
                targetIds: targetIds,
                targetDepartmentId: parseInt(targetDepartmentId),
                remarks: remarks,
                approverId: approverId ? parseInt(approverId) : null
            };

            // 重複がある場合の確認
            if (duplicateStudents.length > 0) {
                const studentNames = duplicateStudents.join('、<br>');
                Swal.fire({
                    title: '確認',
                    html: `以下の生徒は既に選択された学科・コースに所属しています。<br><br><strong>${studentNames}</strong><br><br>処理を続行しますか？`,
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#f39c12',
                    cancelButtonColor: '#aaa',
                    confirmButtonText: 'はい、続行します',
                    cancelButtonText: 'キャンセル'
                }).then((result) => {
                    if (result.isConfirmed) {
                        proceedToConfirm();
                    }
                });
            } else {
                proceedToConfirm();
            }
        });
    }

    // 次のステップへ（ロールによる分岐）
    function proceedToConfirm() {
        if (isSuperAdmin) {
            // 上位管理者: パスワードモーダルへ
            openPasswordModal();
        } else {
            // 一般管理者: 最終確認ダイアログへ
            showFinalConfirm();
        }
    }

    // --- モーダル制御関数 (上位管理者用) ---
    function openPasswordModal() {
        if(modal) {
            passwordInput.value = ''; // 入力欄クリア
            passwordInput.type = 'password'; // タイプを初期化
            
            const toggleEye = document.getElementById('togglePasswordEye');
            if (toggleEye) {
                toggleEye.classList.remove("fa-eye");
                toggleEye.classList.add("fa-eye-slash");
            }
            
            modal.classList.add('active'); // 表示
            
            setTimeout(() => {
                passwordInput.focus();
            }, 100);
        }
    }

    function closePasswordModal() {
        if (modal) {
            modal.classList.remove('active');
            passwordInput.value = '';
        }
    }

    if (modalCancelBtn) {
        modalCancelBtn.addEventListener('click', closePasswordModal);
    }

    // モーダル外クリックで閉じる処理
    window.addEventListener("click", function (e) {
        if (modal && e.target === modal) {
            closePasswordModal();
        }
    });

    // 目玉アイコンでのパスワード表示/非表示切替
    const togglePasswordEye = document.getElementById('togglePasswordEye');
    if (togglePasswordEye) {
        togglePasswordEye.addEventListener('click', function() {
            if (passwordInput.type === "password") {
                passwordInput.type = "text";
                togglePasswordEye.classList.remove("fa-eye-slash");
                togglePasswordEye.classList.add("fa-eye");
            } else {
                passwordInput.type = "password";
                togglePasswordEye.classList.remove("fa-eye");
                togglePasswordEye.classList.add("fa-eye-slash");
            }
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
            closePasswordModal();
            submitExecute(pendingRequestData);
        });
    }
    // --- 即時実行 (上位管理者) ---
    function submitExecute(data) {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        
        const submitUrl = '/admin/request/department/execute/department'; 

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
            title: '学科・コース変更申請を行いますか？',
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