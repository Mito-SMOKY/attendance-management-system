/**
 * notification.js
 */

// --- 変数・定数の設定 ---
let currentPage = 1; 
const PAGE_SIZE = 10; 
const SESSION_KEY = 'notification_search_state'; 

// 最後にチェックされたラジオボタンを記憶する変数（選択解除用）
let lastCheckedRadio = null;

document.addEventListener('DOMContentLoaded', () => {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', (e) => {
            e.preventDefault(); 
            fetchNotifications(1); 
        });
    });

    restoreStateAndFetch();
});

window.addEventListener('popstate', () => {
    restoreStateAndFetch(true); 
});

/**
 * ラジオボタンの切り替え処理
 * 既に選択されているものを再度クリックしたら「選択解除」にする
 */
function toggleRadio(radio) {
    if (radio === lastCheckedRadio) {
        radio.checked = false; // 解除
        lastCheckedRadio = null;
    } else {
        lastCheckedRadio = radio; // 記憶
    }
    fetchNotifications(1);
}

function restoreStateAndFetch(forceRestore = false) {
    let state = null; 
    const isBackFromDetail = document.referrer && document.referrer.includes('/notification/detail');

    if (isBackFromDetail || forceRestore) {
        const sessionData = sessionStorage.getItem(SESSION_KEY);
        if (sessionData) {
            try {
                state = JSON.parse(sessionData); 
            } catch (e) {
                console.error("JSON読み込みエラー", e);
            }
        }
    }

    if (!state) {
        const params = new URLSearchParams(window.location.search);
        if (params.has('page') || params.has('keyword') || params.has('type') || params.has('status')) {
            const p = parseInt(params.get('page'));
            const tParam = params.get('type');
            
            state = {
                page: (!isNaN(p) && p > 0) ? p : 1,
                keyword: params.get('keyword') || "",
                type: tParam ? tParam.split(',') : [],
                status: params.get('status') || "",
                bookmarked: (params.get('bookmarked') === 'true')
            };
        }
    }

    if (!state) {
        state = {
            page: 1, keyword: "", type: [], status: "", bookmarked: false
        };
        sessionStorage.removeItem(SESSION_KEY);
    }

    currentPage = state.page;
    restoreFormInputs(state.keyword, state.type, state.status, state.bookmarked);
    fetchNotifications(currentPage);
}

function restoreFormInputs(keyword, types, status, bookmarked) {
    const keywordEl = document.getElementById('keywordSearch');
    if (keywordEl) keywordEl.value = keyword || "";

    document.querySelectorAll('.type-checkbox').forEach(cb => cb.checked = false); 
    if (types && Array.isArray(types)) {
        document.querySelectorAll('.type-checkbox').forEach(cb => {
            if (types.includes(cb.value)) cb.checked = true; 
        });
    }

    // ステータス（ラジオボタン）の復元
    const radios = document.querySelectorAll('input[name="statusFilter"]');
    radios.forEach(r => r.checked = false);
    lastCheckedRadio = null;

    if (status) {
        const targetRadio = document.querySelector(`input[name="statusFilter"][value="${status}"]`);
        if (targetRadio) {
            targetRadio.checked = true;
            lastCheckedRadio = targetRadio;
        }
    }

    const bookmarkEl = document.getElementById('bookmarkFilter');
    if (bookmarkEl) bookmarkEl.checked = !!bookmarked;
}

function fetchNotifications(page = 1) {
    currentPage = page;

    const bookmarkEl = document.getElementById('bookmarkFilter');
    const keywordEl = document.getElementById('keywordSearch');
    const tableBody = document.getElementById('notificationList');
    const countContainer = document.getElementById('resultCountContainer');

    if (!tableBody) return;

    const isBookmarkedOnly = bookmarkEl ? bookmarkEl.checked : false;
    
    const checkedTypeBoxes = document.querySelectorAll('.type-checkbox:checked');
    const typeList = Array.from(checkedTypeBoxes).map(cb => cb.value);
    const typeValue = typeList.join(','); 

    const checkedRadio = document.querySelector('input[name="statusFilter"]:checked');
    const statusValue = checkedRadio ? checkedRadio.value : "";

    const keyword = keywordEl ? keywordEl.value : "";

    const params = new URLSearchParams();
    params.append('page', currentPage);
    params.append('size', PAGE_SIZE);
    
    if (isBookmarkedOnly) params.append('bookmarked', 'true');
    if (keyword) params.append('keyword', keyword);
    if (typeValue) params.append('type', typeValue);
    if (statusValue) params.append('status', statusValue);

    const stateObj = {
        page: currentPage,
        keyword: keyword,
        type: typeList,
        status: statusValue,
        bookmarked: isBookmarkedOnly
    };

    sessionStorage.setItem(SESSION_KEY, JSON.stringify(stateObj));
    const newUrl = `${window.location.pathname}?${params.toString()}`;
    window.history.replaceState(stateObj, '', newUrl);

    fetch(`/api/notifications?${params.toString()}`)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const notifications = data.content || [];
            const totalPages = data.totalPages || 0;
            const totalCount = data.totalCount || 0;

            if (countContainer) {
                if (totalCount === 0) {
                    countContainer.textContent = "0件中 0件を表示";
                } else {
                    const start = (currentPage - 1) * PAGE_SIZE + 1;
                    const end = Math.min(currentPage * PAGE_SIZE, totalCount);
                    countContainer.textContent = `${totalCount}件中 ${start}〜${end}件を表示`;
                }
            }

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
                titleLink.href = `/notification/detail?id=${notification.notificationId}`;
                titleLink.textContent = notification.title || "件名なし";
                titleLink.className = "notification-link";
                
                titleLink.onclick = function(e) {
                    if (notification.read === false) {
                        e.preventDefault(); 
                        markAsReadAndNavigate(notification.notificationId, titleLink.href);
                    }
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
        .then(() => { window.location.href = url; }) 
        .catch(() => { window.location.href = url; }); 
}

function toggleBookmarkApi(id) {
    fetch(`/api/notifications/${id}/bookmark`, { method: 'POST' })
    .then(r => { 
        if (r.ok) fetchNotifications(currentPage); 
    })
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