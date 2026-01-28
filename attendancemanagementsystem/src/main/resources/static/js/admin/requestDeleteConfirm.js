document.addEventListener('DOMContentLoaded', function() {
    
    const confirmBtn = document.getElementById('confirmBtn');
    
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            // 1. バリデーション（承認者が選択されているか）
            const approverSelect = document.getElementById('approver');
            const approverId = approverSelect ? approverSelect.value : null;
            
            if (!approverId) {
                Swal.fire({
                    icon: 'warning',
                    title: '入力エラー',
                    text: '申請先（承認者）を選択してください。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }
    
            // 2. 送信データの準備
            const remarksElem = document.getElementById('remarks');
            const remarks = remarksElem ? remarksElem.value : "";
            
            // チェックされている生徒IDを取得
            const checkedBoxes = document.querySelectorAll('.confirm-checkbox:checked');
            const targetIds = Array.from(checkedBoxes).map(cb => parseInt(cb.value));
    
            if (targetIds.length === 0) {
                Swal.fire({
                    icon: 'warning',
                    title: '選択エラー',
                    text: '削除対象の生徒が選択されていません。',
                    confirmButtonColor: '#00bdca'
                });
                return;
            }
    
            // 3. 確認ダイアログ
            Swal.fire({
                title: '申請を行いますか？',
                text: "この操作は取り消せません。",
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#00bdca',
                cancelButtonColor: 'rgb(215, 76, 76)',
                confirmButtonText: 'はい、申請します',
                cancelButtonText: 'キャンセル'
            }).then((result) => {
                if (result.isConfirmed) {
                    submitRequest(targetIds, remarks, approverId);
                }
            });
        });
    }

    // サーバーへの送信処理
    function submitRequest(targetIds, remarks, approverId) {
        // メタタグから情報を取得
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const submitUrlMeta = document.querySelector('meta[name="submit-url"]');

        const token = tokenMeta ? tokenMeta.content : '';
        const headerName = headerMeta ? headerMeta.content : '';
        const submitUrl = submitUrlMeta ? submitUrlMeta.content : '';

        if (!submitUrl) {
            console.error("Submit URL not found");
            return;
        }

        const requestData = {
            targetIds: targetIds,
            remarks: remarks,
            approverId: parseInt(approverId)
        };

        // ヘッダーオブジェクトを動的に作成
        const headers = {
            'Content-Type': 'application/json'
        };

        // CSRFトークンが存在する場合のみヘッダーに追加（空文字キーのエラー回避）
        if (token && headerName) {
            headers[headerName] = token;
        }

        // ローディング表示
        Swal.fire({
            title: '処理中...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        fetch(submitUrl, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(requestData)
        })
        .then(response => {
            if (response.ok) {
                return response.text();
            } else {
                throw new Error('サーバーエラーが発生しました');
            }
        })
        .then(data => {
            // 成功モーダル表示 -> OK押下でメニューへ
            Swal.fire({
                icon: 'success',
                title: '完了',
                text: '削除申請が完了しました。',
                confirmButtonColor: '#00bdca',
                confirmButtonText: 'メニューへ戻る'
            }).then(() => {
                // ★修正: RequestControllerの @GetMapping("/menu") に合わせる
                window.location.href = '/admin/request/menu'; 
            });
        })
        .catch(error => {
            console.error('Error:', error);
            Swal.fire({
                icon: 'error',
                title: 'エラー',
                text: '申請に失敗しました。時間をおいて再度お試しください。',
                confirmButtonColor: '#00bdca'
            });
        });
    }
});