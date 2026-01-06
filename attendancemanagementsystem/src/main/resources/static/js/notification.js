/**
 * notification.js
 * 通知一覧の検索、ページング、状態保持を管理するスクリプト
 */

// --- 変数・定数の設定 ---
let currentPage = 1; // 現在表示しているページ番号
const PAGE_SIZE = 10; // 1ページに表示する件数
const SESSION_KEY = 'notification_search_state'; // 画面の状態を保存するときのキー名
const displayName = document.getElementById('display-value');

/**
 * 画面が読み込まれた最初に動く処理
 */
document.addEventListener('DOMContentLoaded', () => {
    // 検索フォームでエンターキーを押したときに、画面がリロードされるのを防ぐ
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', (e) => {
            e.preventDefault(); // これで勝手にリロードされなくなります
            fetchNotifications(1); // 代わりに検索処理（1ページ目）を実行
        });
    });

    // 前回の状態（ページ数や検索条件）を復元して表示する
    restoreStateAndFetch();
});

// ブラウザの「戻る」「進む」ボタンが押されたときも、状態を復元する
window.addEventListener('popstate', () => {
    restoreStateAndFetch(true); // true = 強制的に復元モードで実行
});

/**
 * ★重要：状態を復元して検索を実行する関数
 * @param {boolean} forceRestore - 強制的に復元するかどうかのフラグ
 */
function restoreStateAndFetch(forceRestore = false) {
    let state = null; // ここに復元するデータが入ります
    
    // ★判定ロジック: 「詳細画面から戻ってきた」かどうか？
    // document.referrer には「直前にいたページのURL」が入っています
    const isBackFromDetail = document.referrer && document.referrer.includes('/notification/detail');

    // ケース1: 詳細画面から戻ってきた、またはブラウザバック操作の場合
    // => sessionStorage（一時保存場所）に残っている「直前の状態」を使う
    if (isBackFromDetail || forceRestore) {
        const sessionData = sessionStorage.getItem(SESSION_KEY);
        if (sessionData) {
            try {
                state = JSON.parse(sessionData); // 文字列をオブジェクトに戻す
            } catch (e) {
                console.error("JSON読み込みエラー", e);
            }
        }
    }

    // ケース2: 上記でデータがない場合（ブックマークから来た、URL直打ちなど）
    // => URLについているパラメータ（?page=2&type=...）を見て復元する
    if (!state) {
        const params = new URLSearchParams(window.location.search);
        
        // パラメータが何か1つでもあれば、それを使って状態を作る
        if (params.has('page') || params.has('keyword') || params.has('type') || params.has('status')) {
            const p = parseInt(params.get('page'));
            const tParam = params.get('type');
            
            state = {
                page: (!isNaN(p) && p > 0) ? p : 1, // ページ番号がおかしければ1にする
                keyword: params.get('keyword') || "",
                type: tParam ? tParam.split(',') : [], // カンマ区切りを配列に戻す
                status: params.get('status') || "",
                bookmarked: (params.get('bookmarked') === 'true')
            };
        }
    }

    // ケース3: それでも復元データがない場合（完全に初めて来た、メニューから来たなど）
    // => 初期状態（1ページ目、検索条件なし）にする
    if (!state) {
        state = {
            page: 1,
            keyword: "",
            type: [],
            status: "",
            bookmarked: false
        };
        // メニューから来た時などは、古い保存データが邪魔しないように消しておく
        sessionStorage.removeItem(SESSION_KEY);
    }

    // 決まった値を画面の変数にセット
    currentPage = state.page;
    // 入力フォーム（検索窓やチェックボックス）の見た目も復元
    restoreFormInputs(state.keyword, state.type, state.status, state.bookmarked);

    // 準備ができたので、データを取得して表示！
    fetchNotifications(currentPage);
}

/**
 * 検索フォームの見た目（入力値やチェック状態）を復元する
 */
function restoreFormInputs(keyword, types, status, bookmarked) {
    // キーワード入力欄
    const keywordEl = document.getElementById('keywordSearch');
    if (keywordEl) keywordEl.value = keyword || "";

    // 「通知種別」チェックボックス
    document.querySelectorAll('.type-checkbox').forEach(cb => cb.checked = false); // 一回全部外す
    if (types && Array.isArray(types)) {
        document.querySelectorAll('.type-checkbox').forEach(cb => {
            if (types.includes(cb.value)) cb.checked = true; // 該当するものだけチェック
        });
    }

    // 「ステータス」チェックボックス
    document.querySelectorAll('.status-checkbox').forEach(cb => cb.checked = false);
    if (status) {
        document.querySelectorAll('.status-checkbox').forEach(cb => {
            if (cb.value === status) cb.checked = true;
        });
    }

    // 「スター付きのみ」スイッチ
    const bookmarkEl = document.getElementById('bookmarkFilter');
    if (bookmarkEl) bookmarkEl.checked = !!bookmarked;
}

/**
 * APIから通知データを取ってきて画面に描画する（メイン処理）
 */
function fetchNotifications(page = 1) {
    currentPage = page;

    // 画面の要素を取得
    const bookmarkEl = document.getElementById('bookmarkFilter');
    const keywordEl = document.getElementById('keywordSearch');
    const tableBody = document.getElementById('notificationList'); // 表の中身
    const countContainer = document.getElementById('resultCountContainer'); // 件数表示エリア

    if (!tableBody) return;

    // --- 1. 現在のフォームに入力されている値を取得 ---
    const isBookmarkedOnly = bookmarkEl ? bookmarkEl.checked : false;
    
    // チェックされている種別を集める
    const checkedTypeBoxes = document.querySelectorAll('.type-checkbox:checked');
    const typeList = Array.from(checkedTypeBoxes).map(cb => cb.value);
    const typeValue = typeList.join(','); // API用にカンマ区切りにする

    // チェックされているステータスを集める
    const checkedStatusBoxes = document.querySelectorAll('.status-checkbox:checked');
    let statusValue = checkedStatusBoxes.length === 1 ? checkedStatusBoxes[0].value : "";

    const keyword = keywordEl ? keywordEl.value : "";

    // --- 2. APIに送るパラメータを作る ---
    const params = new URLSearchParams();
    params.append('page', currentPage);
    params.append('size', PAGE_SIZE);
    
    if (isBookmarkedOnly) params.append('bookmarked', 'true');
    if (keyword) params.append('keyword', keyword);
    if (typeValue) params.append('type', typeValue);
    if (statusValue) params.append('status', statusValue);

    // --- 3. ★現在の状態を保存する (これが「戻る」対策) ---
    const stateObj = {
        page: currentPage,
        keyword: keyword,
        type: typeList,
        status: statusValue,
        bookmarked: isBookmarkedOnly
    };

    // (A) SessionStorageに保存（リロードや詳細画面からの戻り用）
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(stateObj));

    // (B) ブラウザのURL欄を更新（ブックマーク登録用）
    const newUrl = `${window.location.pathname}?${params.toString()}`;
    window.history.replaceState(stateObj, '', newUrl);

    // --- 4. API呼び出し ---
    fetch(`/api/notifications?${params.toString()}`)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const notifications = data.content || [];
            const totalPages = data.totalPages || 0;
            const totalCount = data.totalCount || 0;

            // 件数表示の更新
            if (countContainer) {
                if (totalCount === 0) {
                    countContainer.textContent = "0件中 0件を表示";
                } else {
                    const start = (currentPage - 1) * PAGE_SIZE + 1;
                    const end = Math.min(currentPage * PAGE_SIZE, totalCount);
                    countContainer.textContent = `${totalCount}件中 ${start}〜${end}件を表示`;
                }
            }

            // テーブルの中身を一度クリア
            tableBody.innerHTML = '';
            
            // データが無い場合
            if (notifications.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding: 20px;">通知はありません。</td></tr>';
                renderPagination(0);
                return;
            }

            // データがある場合：1行ずつ作成
            notifications.forEach(notification => {
                const row = tableBody.insertRow();
                
                // 未読なら太字にするクラスを追加
                if (notification.read === false) { 
                    row.classList.add('unread-row'); 
                    row.style.fontWeight = "bold";
                } else {
                    // 既読なら少しグレーにする
                    row.style.backgroundColor = "#f5f5f5";
                    row.style.color = "#666";
                }

                // 日付と時間の列
                row.insertCell(0).textContent = formatNotificationDate(notification.createdAt);
                row.insertCell(1).textContent = formatNotificationTime(notification.createdAt);
                
                // タイトルとスターボタンの列
                const detailCell = row.insertCell(2);
                const container = document.createElement('div');
                container.style.cssText = "display: flex; justify-content: space-between; align-items: center; width: 100%;";

                // タイトルリンク作成
                const titleLink = document.createElement('a');
                titleLink.href = `/notification/detail?id=${notification.notificationId}`;
                titleLink.textContent = notification.title || "件名なし";
                titleLink.className = "notification-link";
                
                // クリック時の処理：未読なら「既読API」を呼んでから遷移
                titleLink.onclick = function(e) {
                    if (notification.read === false) {
                        e.preventDefault(); // 通常の移動を一旦止める
                        markAsReadAndNavigate(notification.notificationId, titleLink.href);
                    }
                };

                // スター（ブックマーク）アイコン作成
                const starIcon = document.createElement('i');
                // ブックマーク済みなら黄色い星、違えば空の星
                starIcon.className = notification.bookmarked ? 'fa-solid fa-star star-btn active' : 'fa-regular fa-star star-btn';
                starIcon.style.cursor = "pointer";
                
                // スタークリック時の処理
                starIcon.onclick = function(e) {
                    e.stopPropagation(); // 行クリックなどが反応しないように
                    e.preventDefault();
                    toggleBookmarkApi(notification.notificationId);
                };

                container.appendChild(titleLink);
                container.appendChild(starIcon);
                detailCell.appendChild(container);
            });

            // ページネーション（下のボタン）を作る
            renderPagination(totalPages);
        })
        .catch(error => {
            console.error('Fetch error:', error);
            if (tableBody) tableBody.innerHTML = '<tr><td colspan="3" style="color: red; text-align: center;">データの取得に失敗しました。</td></tr>';
        });
}

/**
 * ページ切り替えボタン（1, 2, 3...）を描画する
 */
function renderPagination(totalPages) {
    const container = document.getElementById('paginationContainer');
    if (!container) return;
    
    container.innerHTML = '';
    if (totalPages <= 1) return; // 1ページしかなければボタン不要

    // 「前へ」ボタン
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '<i class="fa-solid fa-chevron-left"></i>';
    prevBtn.className = 'page-btn prev-next';
    prevBtn.disabled = (currentPage === 1); // 1ページ目なら押せない
    prevBtn.onclick = () => fetchNotifications(currentPage - 1);
    container.appendChild(prevBtn);

    // 数字ボタンの表示ロジック（今のページの前後2ページ分だけ表示する）
    const sidePages = 2;
    for (let i = 1; i <= totalPages; i++) {
        // 「最初」「最後」「今のページの周辺」だけボタンを作る
        if (i === 1 || i === totalPages || (i >= currentPage - sidePages && i <= currentPage + sidePages)) {
            const pageBtn = document.createElement('button');
            pageBtn.textContent = i;
            // 今のページなら色を変えるクラスをつける
            pageBtn.className = (i === currentPage) ? 'page-btn active' : 'page-btn';
            pageBtn.onclick = () => fetchNotifications(i);
            container.appendChild(pageBtn);
        } else if (i === currentPage - sidePages - 1 || i === currentPage + sidePages + 1) {
            // 省略記号 (...) を入れる場所
            const ellipsis = document.createElement('span');
            ellipsis.textContent = '...';
            ellipsis.className = 'pagination-ellipsis';
            container.appendChild(ellipsis);
        }
    }

    // 「次へ」ボタン
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '<i class="fa-solid fa-chevron-right"></i>';
    nextBtn.className = 'page-btn prev-next';
    nextBtn.disabled = (currentPage === totalPages); // 最後のページなら押せない
    nextBtn.onclick = () => fetchNotifications(currentPage + 1);
    container.appendChild(nextBtn);
}

/**
 * 既読APIを呼んでから、詳細画面へ移動する
 */
function markAsReadAndNavigate(id, url) {
    fetch(`/api/notifications/${id}/read`, { method: 'POST' })
        .then(() => { window.location.href = url; }) // 成功したら移動
        .catch(() => { window.location.href = url; }); // エラーでもとりあえず移動
}

/**
 * ブックマークAPIを呼んで、成功したら画面を更新する
 */
function toggleBookmarkApi(id) {
    fetch(`/api/notifications/${id}/bookmark`, { method: 'POST' })
    .then(r => { 
        if (r.ok) fetchNotifications(currentPage); // 成功したら今のページを再読み込みして星を反映
    })
    .catch(err => console.error(err));
}

/**
 * 日付を見やすい形式（12/28(日)）にする
 */
function formatNotificationDate(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    const dayOfWeek = ["日", "月", "火", "水", "木", "金", "土"][date.getDay()];
    return `${date.getMonth() + 1}/${date.getDate()}(${dayOfWeek})`;
}

/**
 * 時間を見やすい形式（12:05）にする
 */
function formatNotificationTime(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    // padStart(2, '0') は、1桁のときに0をつけて2桁にする処理（例: 9 -> 09）
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}