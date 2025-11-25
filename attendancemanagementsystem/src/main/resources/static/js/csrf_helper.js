/**
 * CSRFトークンを自動でヘッダーに追加して fetch を実行する
 * この関数は 'export' されているため、他のJSファイルから 'import' して利用できる
 */
export function fetchWithCSRF(url, options = {}) {
    // 1. トークンとヘッダー名を取得
    // (document はグローバルなので、どこからでもアクセスできる)
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    // 2. options.headers がなければ、空のオブジェクト {} を作成
    const headers = options.headers || {};

    // 3. headers に CSRF トークンを追加
    if (token && header) {
        headers[header] = token;
    }

    // 4. 更新した headers を使って fetch を実行
    return fetch(url, {
        ...options, // (method: 'POST' や body: ... はここに含まれる)
        headers: headers 
    });
}