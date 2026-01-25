document.addEventListener("DOMContentLoaded", function() {
    // 1. URLパラメータの取得
    const urlParams = new URLSearchParams(window.location.search);
    const shouldDownload = urlParams.get('download');
    
    // ★修正箇所: Thymeleafの変数ではなく、HTMLの隠し項目からIDを取得する
    const dataListIdInput = document.getElementById('dataListIdValue');
    const dataListId = dataListIdInput ? dataListIdInput.value : null;

    // 2. ダウンロード条件を満たしている場合
    if (shouldDownload === 'true' && dataListId) {
        
        // URLから 'download' パラメータを即座に削除する
        urlParams.delete('download');
        
        // パラメータ削除後のURLを作成
        const newQuery = urlParams.toString() ? '?' + urlParams.toString() : '';
        const newUrl = window.location.pathname + newQuery;
        
        // ブラウザの履歴を書き換える（画面遷移せずにURLだけ変える）
        window.history.replaceState({}, '', newUrl);

        // 3. その後でダウンロードを開始する
        setTimeout(function() {
            window.location.href = '/admin/downloadCsv/' + dataListId;
        }, 500);
    }
});