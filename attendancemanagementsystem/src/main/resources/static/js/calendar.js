// === 1. グローバル変数 ===
let currentViewMode = 'schedule'; 
let currentWeekStart;

// === 2. モーダル制御関数 (定義を先に行う) ===
function openModal(dateStr) {
    const modal = document.getElementById('scheduleModal');
    const modalDateEl = document.getElementById('modalDate');
    const listContainer = document.getElementById('existing-events-list');

    if (!modal || !modalDateEl) return;

    // 日付の表示
    modalDateEl.textContent = dateStr.replace(/-/g, '/') + ' の予定';
    modalDateEl.dataset.rawDate = dateStr; // 保存用に生の日付を持っておく
    
    // 既存予定の表示
    if (listContainer) {
        const dailyEvents = (typeof calendarEventsData !== 'undefined' ? calendarEventsData : [])
                            .filter(e => e.date === dateStr);
        listContainer.innerHTML = dailyEvents.map(e => `
            <div class="existing-item" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span>${e.title}</span>
                <button type="button" class="delete-schedule-btn" data-id="${e.calendarId}" 
                        style="background-color: #ff4d4d; color: white; border: none; padding: 4px 8px; cursor: pointer; border-radius: 4px;">
                    削除
                </button>
            </div>
        `).join('') || '<p>予定はありません</p>';
    }

    modal.style.display = 'block';
}

function closeModal() {
    const modal = document.getElementById('scheduleModal');
    if (modal) modal.style.display = 'none';
}

/**
 * 3. 削除処理
 */
function handleDeleteSchedule(button) {
    const scheduleId = button.dataset.id;
    if (!confirm('この予定を削除してもよろしいですか？')) return;

    fetch(`/student/calendar/delete/${scheduleId}`, { method: 'DELETE' })
    .then(response => {
        if (response.ok) {
            alert('予定を削除しました。');
            window.location.reload(); // カレンダー再描画のためリロード
        } else {
            alert('削除に失敗しました。');
        }
    })
    .catch(error => console.error('削除エラー:', error));
}

/**
 * 4. カレンダー生成・描画ロジック
 */
function createCalendar(month, year) {
    const monthDays = ["日", "月", "火", "水", "木", "金", "土"];
    let tableHTML = '<table class="calendar"><thead><tr>';
    for (let i = 0; i < 7; i++) {
        tableHTML += `<th class="${i === 0 || i === 6 ? 'sun' : ''}">${monthDays[i]}</th>`;
    }
    tableHTML += '</tr></thead><tbody>';

    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInPrevMonth = new Date(year, month, 0).getDate();
    const today = new Date();

    let dayCount = 1;
    let prevDayCount = daysInPrevMonth - firstDay + 1;

    for (let i = 0; i < 6; i++) {
        tableHTML += '<tr>';
        for (let j = 0; j < 7; j++) {
            if (i === 0 && j < firstDay) {
                tableHTML += `<td class="mute">${prevDayCount++}</td>`;
            } else if (dayCount > daysInMonth) {
                tableHTML += `<td class="mute">${dayCount++ - daysInMonth}</td>`;
            } else {
                const dataDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayCount).padStart(2, '0')}`;
                let eventsHtml = '';

                if (currentViewMode === 'schedule') {
                    const dailyEvents = (typeof calendarEventsData !== 'undefined' ? calendarEventsData : []).filter(e => e.date === dataDate);
                    if (dailyEvents.length > 0) {
                        eventsHtml = '<div class="schedule-list">' + 
                            dailyEvents.map(e => `<div class="schedule-item">${e.title}</div>`).join('') + 
                            '</div>';
                    }
                } else {
                    const dailyRecords = (typeof attendanceRecordsData !== 'undefined' ? attendanceRecordsData : []).filter(r => r.date === dataDate);
                    if (dailyRecords.length > 0) {
                        const isAllPresent = dailyRecords.every(r => r.status === '出席');
                        const isAllAbsent = dailyRecords.every(r => r.status === '欠席');
                        let mark = isAllPresent ? '〇' : isAllAbsent ? '✕' : '△';
                        let cls = isAllPresent ? 'attendance-present' : isAllAbsent ? 'attendance-absent' : 'attendance-late';
                        eventsHtml = `<div class="attendance-list"><span class="${cls}">${mark}</span></div>`;
                    }
                }

                const isToday = (dayCount === today.getDate() && month === today.getMonth() && year === today.getFullYear());
                tableHTML += `<td class="${isToday ? 'today' : ''} ${j === 0 ? 'sun' : j === 6 ? 'sat' : ''}" data-date="${dataDate}">
                                <div class="day-number">${dayCount}</div>${eventsHtml}</td>`;
                dayCount++;
            }
        }
        tableHTML += '</tr>';
        if (dayCount > daysInMonth && i >= 4) break;
    }
    return tableHTML + '</tbody></table>';
}

function renderCalendar(month, year) {
    const calendarYmEl = document.getElementById('calendar-ym'); 
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); 
    if (!calendarYmEl || !calendarTableContainerEl) return;

    calendarYmEl.textContent = `${year}年 ${month + 1}月`;
    calendarTableContainerEl.innerHTML = createCalendar(month, year);
}

/**
 * 5. 初期化
 */
function initializeCalendar() {
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');

    // --- A. 「現在の年」を取得 (プルダウンの選択肢の基準) ---
    const todayObj = new Date();
    const currentActualYear = todayObj.getFullYear();

    // --- B. URLやサーバーからの情報を取得 (表示中のカレンダーの基準) ---
    const urlParams = new URLSearchParams(window.location.search);
    currentViewMode = urlParams.get('mode') === 'attendance' ? 'attendance' : 'schedule';
    if (viewToggleCheckbox) viewToggleCheckbox.checked = (currentViewMode === 'attendance');

    let serverMonth, serverYear;
    if (typeof serverTargetMonthString === 'undefined' || !serverTargetMonthString) {
        serverMonth = todayObj.getMonth();
        serverYear = todayObj.getFullYear();
    } else {
        const d = new Date(serverTargetMonthString + "T00:00:00"); 
        serverMonth = d.getMonth(); 
        serverYear = d.getFullYear();
    }

    // --- C. 年プルダウン生成 (常に「現在の年」から±2年) ---
    if (yearSelector) {
        yearSelector.innerHTML = '';
        // 現在の年から-2年 〜 +2年 の固定範囲をループ
        for (let y = currentActualYear - 2; y <= currentActualYear + 2; y++) {
            yearSelector.add(new Option(y + '年', y));
        }
        // プルダウンの選択状態は「表示中の年」に合わせる
        yearSelector.value = serverYear;
    }

    // --- D. 月プルダウン生成 ---
    if (monthSelector) {
        monthSelector.innerHTML = '';
        for (let m = 0; m < 12; m++) {
            monthSelector.add(new Option((m + 1) + '月', m));
        }
        monthSelector.value = serverMonth;
    }

    renderCalendar(serverMonth, serverYear);
}

// === 6. イベントリスナー登録 (DOMContentLoaded) ===
document.addEventListener('DOMContentLoaded', () => {
    initializeCalendar();

    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const calendarTableContainerEl = document.getElementById('calendar-table-container');

    // 更新ハンドラ（URL遷移）
    const handleUpdate = () => {
        const y = yearSelector.value;
        const m = String(parseInt(monthSelector.value) + 1).padStart(2, '0');
        const mode = viewToggleCheckbox && viewToggleCheckbox.checked ? 'attendance' : 'schedule';
        location.href = `/student/main_calendar?month=${y}-${m}&mode=${mode}`;
    };

    yearSelector?.addEventListener('change', handleUpdate);
    monthSelector?.addEventListener('change', handleUpdate);
    viewToggleCheckbox?.addEventListener('change', handleUpdate);

    // カレンダークリック
    calendarTableContainerEl?.addEventListener('click', (event) => {
        const targetCell = event.target.closest('td[data-date]');
        if (targetCell) {
            const dateStr = targetCell.dataset.date;
            if (viewToggleCheckbox && viewToggleCheckbox.checked) {
                window.location.href = `/student/attendance/date?date=${dateStr}`;
            } else {
                openModal(dateStr);
            }
        }
    });

    // モーダル内クリック（削除ボタンなど）
    document.getElementById('scheduleModal')?.addEventListener('click', (event) => {
        const deleteBtn = event.target.closest('.delete-schedule-btn');
        if (deleteBtn) {
            handleDeleteSchedule(deleteBtn);
        } else if (event.target.id === 'scheduleModal') {
            closeModal();
        }
    });

    document.getElementById('closeButton')?.addEventListener('click', closeModal);

    // 予定追加フォーム
    document.getElementById('scheduleForm')?.addEventListener('submit', (event) => {
        event.preventDefault();
        const title = document.getElementById('scheduleTitle').value;
        const date = document.getElementById('modalDate').dataset.rawDate;

        const formData = new URLSearchParams({ title, date });
        fetch('/student/calendar/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        }).then(res => res.ok ? window.location.reload() : alert("保存失敗"));
    });
});

// BFcache対策
window.addEventListener('pageshow', (event) => {
    if (event.persisted) initializeCalendar();
});