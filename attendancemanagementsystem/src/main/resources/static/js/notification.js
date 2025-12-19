/**
 * 現在のページ番号と1ページあたりの表示件数を管理
 */
let currentPage = 1;
const PAGE_SIZE = 10;

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

    // 複数の通知タイプをまとめて取得 
    const checkedTypeBoxes = document.querySelectorAll('.type-checkbox:checked');
    // チェックされた全てのvalue（SYSTEM, APPLICATION等）を配列にし、カンマ区切り文字列に変換
    const typeValue = Array.from(checkedTypeBoxes).map(cb => cb.value).join(',');

    // ステータス（未読・既読）の取得
    const checkedStatusBoxes = document.querySelectorAll('.status-checkbox:checked');
    let statusValue = checkedStatusBoxes.length === 1 ? checkedStatusBoxes[0].value : "";

    const keyword = keywordEl ? keywordEl.value : "";

    // APIリクエストパラメータの構築
    const params = new URLSearchParams({
        keyword: keyword,
        type: typeValue,    // カンマ区切りのグループ名が送られる
        status: statusValue,
        bookmarked: isBookmarkedOnly,
        page: currentPage,
        size: PAGE_SIZE
    });

    fetch(`/api/notifications?${params.toString()}`)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const notifications = data.content || [];
            const totalPages = data.totalPages || 0;
            const totalCount = data.totalCount || 0;

            // --- 1. 表示件数テキストの更新 ---
            if (countContainer) {
                if (totalCount === 0) {
                    countContainer.textContent = "0件中 0件を表示";
                } else {
                    const start = (currentPage - 1) * PAGE_SIZE + 1;
                    const end = Math.min(currentPage * PAGE_SIZE, totalCount);
                    countContainer.textContent = `${totalCount}件中 ${start}〜${end}件を表示`;
                }
            }

            // --- 2. テーブルのクリアと描画 ---
            tableBody.innerHTML = '';
            
            if (notifications.length === 0) {
                 tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding: 20px;">通知はありません。</td></tr>';
                 renderPagination(0);
                 return;
            }

            notifications.forEach(notification => {
                const row = tableBody.insertRow();
                
                // 未読・既読のスタイル切り替え
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
                titleLink.href = `/notification/detail?id=${notification.notificationId}`;
                titleLink.textContent = notification.title;
                titleLink.className = "notification-link";

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

            // --- 3. ページネーションの更新 ---
            renderPagination(totalPages);
        })
        .catch(error => {
            console.error('Fetch error:', error);
            if (tableBody) tableBody.innerHTML = '<tr><td colspan="3" style="color: red; text-align: center;">データの取得に失敗しました。</td></tr>';
        });
}

/**
 * ページネーションUIの生成
 */
function renderPagination(totalPages) {
    const container = document.getElementById('paginationContainer');
    if (!container) return;
    
    container.innerHTML = '';
    if (totalPages <= 1) return;

    // 前へボタン
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '<i class="fa-solid fa-chevron-left"></i>';
    prevBtn.className = 'page-btn prev-next';
    prevBtn.disabled = (currentPage === 1);
    prevBtn.onclick = () => fetchNotifications(currentPage - 1);
    container.appendChild(prevBtn);

    // 数字ボタン（前後2ページ分表示、それ以外は省略）
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

    // 次へボタン
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '<i class="fa-solid fa-chevron-right"></i>';
    nextBtn.className = 'page-btn prev-next';
    nextBtn.disabled = (currentPage === totalPages);
    nextBtn.onclick = () => fetchNotifications(currentPage + 1);
    container.appendChild(nextBtn);
}

/**
 * 日付フォーマット: 12/18(木)
 */
function formatNotificationDate(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    const dayOfWeek = ["日", "月", "火", "水", "木", "金", "土"][date.getDay()];
    return `${date.getMonth() + 1}/${date.getDate()}(${dayOfWeek})`;
}

/**
 * 時刻フォーマット: 09:30
 */
function formatNotificationTime(dateSource) {
    if (!dateSource) return "-";
    const date = new Date(dateSource);
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

/**
 * ブックマーク状態の切り替えAPI呼び出し
 */
function toggleBookmarkApi(id) {
    fetch(`/api/notifications/${id}/bookmark`, { method: 'POST' })
    .then(response => { 
        if (response.ok) {
            // 現在のページの状態を維持して再描画
            fetchNotifications(currentPage); 
        } 
    })
    .catch(err => console.error('Bookmark Error:', err));
}

// 初期読み込み
document.addEventListener('DOMContentLoaded', () => fetchNotifications(1));