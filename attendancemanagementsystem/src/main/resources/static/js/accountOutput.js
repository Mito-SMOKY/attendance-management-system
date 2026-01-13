/**
 * 参照ボタンが押されたら、隠しinputをクリックしてエクスプローラーを開く
 */
function openExplorer() {
    document.getElementById('dummyFolderInput').click();
}

/**
 * フォルダが選択されたら、その名前を入力欄に表示する
 * @param {HTMLInputElement} input ファイル入力要素
 */
function updatePath(input) {
    if (input.files && input.files.length > 0) {
        // セキュリティ上フルパスは取得できないため、注意書きを表示します
        // 実際にはブラウザのダウンロード設定が優先されます
        document.getElementById('savePathDisplay').value = "選択中: (ブラウザのダウンロード先設定が優先されます)";
    }
}

/**
 * 出力実行
 */
function execOutput() {
    // 1. フォーム送信（ダウンロード開始）
    document.getElementById('outputForm').submit();

    // 2. 1秒後にアカウント管理画面へ遷移
    setTimeout(function() {
        alert("出力を開始しました。\nアカウント作成TOPへ戻ります。");
        window.location.href = '/admin/accountManage';
    }, 1000);
}