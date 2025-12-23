/**
 * 現在のページ番号と1ページあたりの表示件数を管理
 */
let currentPage = 1;
const PAGE_SIZE = 10;

/**
 * 初期読み込み処理
 * 「ブラウザが記憶している状態(history.state)」があればそれを最優先し、
 * なければURLパラメータを見て復元します。
 */
document.addEventListener('DOMContentLoaded', () => {
    restoreStateAndFetch();
});

/**
 * 状態を復元して検索を実行する関数
 */
function restoreStateAndFetch() {
    // 優先順位1: ブラウザが記憶している履歴データ (戻るボタン対策の要)
    const historyState = window.history.state;
    
    // 優先順位2: URLパラメータ (ブックマークやリンクからのアクセス用)
    const params = new URLSearchParams(window.location.search);

    let targetPage = 1;
    let savedKeyword = "";
    let savedType = [];
    let savedStatus = "";
    let savedBookmarked = false;

    if (historyState) {
        // --- A. 履歴データがある場合（戻るボタンで戻ってきた時など） ---
        targetPage = historyState.page || 1;
        savedKeyword = historyState.keyword || "";
        savedType = historyState.type || [];
        savedStatus = historyState.status || "";
        savedBookmarked = historyState.bookmarked || false;
    } else {
        // --- B. 履歴がない場合（URLから復元） ---
        const p = parseInt(params.get('page'));
        if (!isNaN(p) && p > 0) targetPage = p;

        savedKeyword = params.get('keyword') || "";
        
        const tParam = params.get('type');
        if (tParam) savedType = tParam.split(',');

        savedStatus = params.get('status') || "";
        savedBookmarked = (params.get('bookmarked') === 'true');
    }

    // 画面の入力フォームに値をセット（見た目の復元）
    restoreFormInputs(savedKeyword, savedType, savedStatus, savedBookmarked);

    // 検索実行
    fetchNotifications(targetPage);
}

/**
 * 画面の入力フォームを復元するヘルパー
 */
function restoreFormInputs(keyword, types, status, bookmarked) {
    const keywordEl = document.getElementById('keywordSearch');
    if (keywordEl) keywordEl.value = keyword;

    // 一旦全クリアしてからチェック
    document.querySelectorAll('.type-checkbox').forEach(cb => cb.checked = false);
    if (types && types.length > 0) {
        document.querySelectorAll('.type-checkbox').forEach(cb => {
            if (types.includes(cb.value)) cb.checked = true;
        });
    }

    document.querySelectorAll('.status-checkbox').forEach(cb => cb.checked = false);
    if (status) {
        document.querySelectorAll('.status-checkbox').forEach(cb => {
            if (cb.value === status) cb.checked = true;
        });
    }

    const bookmarkEl = document.getElementById('bookmarkFilter');
    if (bookmarkEl) bookmarkEl.checked = bookmarked;
}

/**
 * 検索フィルタ、ページ番号に基づき通知一覧を取得・描画するメイン関数
 */
function fetchNotifications(page = 1) {
    currentPage = page;

    const bookmarkEl = document.getElementById('bookmarkFilter');
    const isBookmarkedOnly = bookmarkEl ? bookmarkEl.checked : false;
    const keywordEl = document.getElementById('keywordSearch');
    const tableBody = document.getElementById('notificationList');
    const countContainer = document.getElementById('resultCountContainer');

    if (!tableBody) return;

    // フィルタ値の取得
    const checkedTypeBoxes = document.querySelectorAll('.type-checkbox:checked');
    // 配列として保存しておく（履歴復元用）
    const typeList = Array.from(checkedTypeBoxes).map(cb => cb.value);
    const typeValue = typeList.join(',');

    const checkedStatusBoxes = document.querySelectorAll('.status-checkbox:checked');
    let statusValue = checkedStatusBoxes.length === 1 ? checkedStatusBoxes[0].value : "";

    const keyword = keywordEl ? keywordEl.value : "";

    // --- APIリクエストパラメータ構築 ---
    const params = new URLSearchParams();
    params.append('page', currentPage);
    params.append('size', PAGE_SIZE);
    
    if (isBookmarkedOnly) params.append('bookmarked', 'true');
    if (keyword) params.append('keyword', keyword);
    if (typeValue) params.append('type', typeValue);
    if (statusValue) params.append('status', statusValue);


    // ★★★ ここが最重要 ★★★
    // 「現在の状態」をオブジェクトにまとめて、ブラウザの履歴(History)に保存します。
    // 第1引数にオブジェクトを渡すことで、戻るボタンを押した時に window.history.state で取り出せるようになります。
    const stateObj = {
        page: currentPage,
        keyword: keyword,
        type: typeList,
        status: statusValue,
        bookmarked: isBookmarkedOnly
    };
    const newUrl = `${window.location.pathname}?${params.toString()}`;
    
    // 現在の履歴を書き換え（replaceState）
    window.history.replaceState(stateObj, '', newUrl);


    // API呼び出し
    fetch(`/api/notifications?${params.toString()}`)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const notifications = data.content || [];
            const totalPages = data.totalPages || 0;
            const totalCount = data.totalCount || 0;

            // 件数表示
            if (countContainer) {
                if (totalCount === 0) {
                    countContainer.textContent = "0件中 0件を表示";
                } else {
                    const start = (currentPage - 1) * PAGE_SIZE + 1;
                    const end = Math.min(currentPage * PAGE_SIZE, totalCount);
                    countContainer.textContent = `${totalCount}件中 ${start}〜${end}件を表示`;
                }
            }

            // テーブル描画
            tableBody.innerHTML = '';
            
            if (notifications.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding: 20px;">通知はありません。</td></tr>';
                renderPagination(0);
                return;
            }

            notifications.forEach(notification => {
                const row = tableBody.insertRow();
                if (notification.read === false) { 
                    row.classList.add('unread-row'); 
                    row.style.fontWeight = "bold";
                } else {
                    row.style.backgroundColor = "#f5f5f5";
                    row.style.color = "#666";
                }

                row.insertCell(0).textContent = formatNotificationDate(notification.createdAt);
                row.insertCell(1).textContent = formatNotificationTime(notification.createdAt);
                
                const detailCell = row.insertCell(2);
                const container = document.createElement('div');
                container.style.cssText = "display: flex; justify-content: space-between; align-items: center; width: 100%;";

                const titleLink = document.createElement('a');
                // リンク先を設定
                titleLink.href = `/notification/detail?id=${notification.notificationId}`;
                titleLink.textContent = notification.title;
                titleLink.className = "notification-link";
                
                // クリック時の処理
                titleLink.onclick = function(e) {
                    if (notification.read === false) {
                        e.preventDefault();
                        markAsReadAndNavigate(notification.notificationId, titleLink.href);
                    }
                    // 既読の場合は、そのままhrefへの遷移を許可する
                };

                const starIcon = document.createElement('i');
                starIcon.className = notification.bookmarked ? 'fa-solid fa-star star-btn active' : 'fa-regular fa-star star-btn';
                starIcon.style.cursor = "pointer";
                starIcon.onclick = function(e) {
                    e.stopPropagation(); 
                    e.preventDefault();
                    toggleBookmarkApi(notification.notificationId);
                };

                container.appendChild(titleLink);
                container.appendChild(starIcon);
                detailCell.appendChild(container);
            });

            renderPagination(totalPages);
        })
        .catch(error => {
            console.error('Fetch error:', error);
            if (tableBody) tableBody.innerHTML = '<tr><td colspan="3" style="color: red; text-align: center;">データの取得に失敗しました。</td></tr>';
        });
}

// --- 以下、ヘルパー関数などは変更なし ---

function renderPagination(totalPages) {
    const container = document.getElementById('paginationContainer');
    if (!container) return;
    
    container.innerHTML = '';
    if (totalPages <= 1) return;

    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '<i class="fa-solid fa-chevron-left"></i>';
    prevBtn.className = 'page-btn prev-next';
    prevBtn.disabled = (currentPage === 1);
    prevBtn.onclick = () => fetchNotifications(currentPage - 1);
    container.appendChild(prevBtn);

    const sidePages = 2;
    for (let i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || (i >= currentPage - sidePages && i <= currentPage + sidePages)) {
            const pageBtn = document.createElement('button');
            pageBtn.textContent = i;
            pageBtn.className = (i === currentPage) ? 'page-btn active' : 'page-btn';
            pageBtn.onclick = () => fetchNotifications(i);
            container.appendChild(pageBtn);
        } else if (i === currentPage - sidePages - 1 || i === currentPage + sidePages + 1) {
            const ellipsis = document.createElement('span');
            ellipsis.textContent = '...';
            ellipsis.className = 'pagination-ellipsis';
            container.appendChild(ellipsis);
        }
    }

    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '<i class="fa-solid fa-chevron-right"></i>';
    nextBtn.className = 'page-btn prev-next';
    nextBtn.disabled = (currentPage === totalPages);
    nextBtn.onclick = () => fetchNotifications(currentPage + 1);
    container.appendChild(nextBtn);
}

function markAsReadAndNavigate(id, url) {
    fetch(`/api/notifications/${id}/read`, { method: 'POST' })
        .then(() => {
            window.location.href = url;
        })
        .catch(() => {
            window.location.href = url;
        });
}

function toggleBookmarkApi(id) {
    fetch(`/api/notifications/${id}/bookmark`, { method: 'POST' })
    .then(r => { if (r.ok) fetchNotifications(currentPage); })
    .catch(err => console.error(err));
}

function formatNotificationDate(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    const dayOfWeek = ["日", "月", "火", "水", "木", "金", "土"][date.getDay()];
    return `${date.getMonth() + 1}/${date.getDate()}(${dayOfWeek})`;
}

function formatNotificationTime(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}