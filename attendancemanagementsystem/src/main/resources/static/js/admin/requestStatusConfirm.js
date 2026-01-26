document.addEventListener('DOMContentLoaded', function() {
    
    const confirmBtn = document.getElementById('confirmBtn');
    
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
                    confirmButtonColor: '#539DA4'
                });
                return;
            }

            // 2. バリデーション（承認者）
            const approverSelect = document.getElementById('approver');
            const approverId = approverSelect ? approverSelect.value : null;
            
            if (!approverId) {
                Swal.fire({
                    icon: 'warning',
                    title: '入力エラー',
                    text: '申請先（承認者）を選択してください。',
                    confirmButtonColor: '#539DA4'
                });
                return;
            }
    
            // 3. 送信データの準備
            const remarksElem = document.getElementById('remarks');
            const remarks = remarksElem ? remarksElem.value : "";
            
            // チェックされている生徒IDを取得
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
    
            // 4. 確認ダイアログ
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
                    submitRequest(targetIds, targetStatusId, remarks, approverId);
                }
            });
        });
    }

    // サーバーへの送信処理
    function submitRequest(targetIds, targetStatusId, remarks, approverId) {
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
            targetStatusId: parseInt(targetStatusId), // ★追加: ステータスID
            remarks: remarks,
            approverId: parseInt(approverId)
        };

        const headers = {
            'Content-Type': 'application/json'
        };

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
            // 成功モーダル
            Swal.fire({
                icon: 'success',
                title: '完了',
                text: 'ステータス変更申請が完了しました。',
                confirmButtonColor: '#539DA4',
                confirmButtonText: 'メニューへ戻る'
            }).then(() => {
                window.location.href = '/admin/request/menu'; 
            });
        })
        .catch(error => {
            console.error('Error:', error);
            Swal.fire({
                icon: 'error',
                title: 'エラー',
                text: '申請に失敗しました。時間をおいて再度お試しください。',
                confirmButtonColor: '#539DA4'
            });
        });
    }
});