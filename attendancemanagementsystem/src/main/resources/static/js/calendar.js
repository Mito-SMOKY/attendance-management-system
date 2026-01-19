// === 1. グローバル変数 ===
let currentViewMode = 'schedule'; 
let currentWeekStart;

// === 2. モーダル制御関数 ===
function openModal(dateStr) {
    const modal = document.getElementById('scheduleModal');
    const modalDateEl = document.getElementById('modalDate');
    const listContainer = document.getElementById('existing-events-list');

    if (!modal || !modalDateEl) return;

    modalDateEl.textContent = dateStr.replace(/-/g, '/') + ' の予定';
    modalDateEl.dataset.rawDate = dateStr; 
    
    // 既存予定リストの表示更新
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

// === 3. 予定削除処理関数 ===
function handleDeleteSchedule(button) {
    const scheduleId = button.dataset.id;
    if (!confirm('この予定を削除してもよろしいですか？')) return;

    const pathSegments = window.location.pathname.split('/');
    const rolePath = pathSegments[1];

    fetch(`/${rolePath}/calendar/delete/${scheduleId}`, { method: 'DELETE' })
    .then(response => {
        if (response.ok) {
            alert('予定を削除しました。');
            window.location.reload(); 
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
                    // 出席管理モード
                    const dailyRecords = (typeof attendanceRecordsData !== 'undefined' ? attendanceRecordsData : []).filter(r => r.date === dataDate);
                    
                    if (dailyRecords.length > 0) {
                        const status = dailyRecords[0].status; // "◎", "〇", "△", "欠席" など
                        
                        let mark = status;
                        let cls = '';

                        if (status === '◎') {
                            mark = '◎';
                            cls = 'attendance-present'; 
                        } else if (status === '〇') {
                            mark = '〇';
                            cls = 'attendance-present';
                        } else if (status === '△') {
                            mark = '△';
                            cls = 'attendance-late';    
                        } else if (status === '欠席') {
                            mark = '✕';
                            cls = 'attendance-absent';  
                        } else {
                            mark = status;
                            cls = 'attendance-late';
                        }
                        
                        eventsHtml = `<div class="attendance-list"><span class="${cls}">${mark}</span></div>`;
                    }
                }

                // 今日の日付強調表示
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

// === 5. 初期化 ===
function initializeCalendar() {
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');

    // URLパラメータから表示モードを取得
    const urlParams = new URLSearchParams(window.location.search);
    currentViewMode = urlParams.get('mode') === 'attendance' ? 'attendance' : 'schedule';
    if (viewToggleCheckbox) viewToggleCheckbox.checked = (currentViewMode === 'attendance');

    // サーバーから渡されたターゲット年月を解析
    // PCの現在時刻ではなく、表示しようとしている年月を基準にする
    const todayObj = new Date();
    let serverMonth, serverYear;
    if (typeof serverTargetMonthString === 'undefined' || !serverTargetMonthString) {
        serverMonth = todayObj.getMonth();
        serverYear = todayObj.getFullYear();
    } else {
        const d = new Date(serverTargetMonthString + "T00:00:00"); 
        serverMonth = d.getMonth(); 
        serverYear = d.getFullYear();
    }

    // --- C. 年プルダウン生成 ---
    if (yearSelector) {
        yearSelector.innerHTML = '';

        // 1. 入学年度(studentAcademicYear)が定義されている場合はそれを使用
        if (typeof studentAcademicYear !== 'undefined' && studentAcademicYear) {
            // 入学年度から3年間を表示 (例: 2026, 2027, 2028)
            const startYear = parseInt(studentAcademicYear, 10);
            for (let i = 0; i < 3; i++) {
                let y = startYear + i;
                yearSelector.add(new Option(y + '年', y));
            }
        } else {
            // 2. 安全策(フォールバック): 
            // 「PCの現在年」ではなく「今表示しているカレンダーの年(serverYear)」を基準にする
            // これにより、2026年を表示中なら確実に2026年が選択肢に出る
            const baseYear = serverYear; 
            for (let y = baseYear - 2; y <= baseYear + 2; y++) {
                yearSelector.add(new Option(y + '年', y));
            }
        }

        // プルダウンの選択状態を表示中の年に合わせる
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

    // カレンダー描画
    renderCalendar(serverMonth, serverYear);
}

// === 6. イベントリスナー登録 ===
document.addEventListener('DOMContentLoaded', () => {
    initializeCalendar();

    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const calendarTableContainerEl = document.getElementById('calendar-table-container');

    const handleUpdate = () => {
        const y = yearSelector.value;
        const m = String(parseInt(monthSelector.value) + 1).padStart(2, '0');
        const mode = viewToggleCheckbox && viewToggleCheckbox.checked ? 'attendance' : 'schedule';
        const pathSegments = window.location.pathname.split('/'); 
        const rolePath = pathSegments[1];
        location.href =  `/${rolePath}/main_calendar?month=${y}-${m}&mode=${mode}`;
    };

    yearSelector?.addEventListener('change', handleUpdate);
    monthSelector?.addEventListener('change', handleUpdate);
    viewToggleCheckbox?.addEventListener('change', handleUpdate);

    calendarTableContainerEl?.addEventListener('click', (event) => {
        const targetCell = event.target.closest('td[data-date]');
        if (targetCell) {
            const dateStr = targetCell.dataset.date;
            if (viewToggleCheckbox && viewToggleCheckbox.checked) {
                const pathSegments = window.location.pathname.split('/'); 
                const rolePath = pathSegments[1]; 
                window.location.href = `/${rolePath}/attendance/date?date=${dateStr}`;
            } else {
                openModal(dateStr);
            }
        }
    });

    document.getElementById('scheduleModal')?.addEventListener('click', (event) => {
        const deleteBtn = event.target.closest('.delete-schedule-btn');
        if (deleteBtn) {
            handleDeleteSchedule(deleteBtn);
        } else if (event.target.id === 'scheduleModal') {
            closeModal();
        }
    });

    document.getElementById('closeButton')?.addEventListener('click', closeModal);

    document.getElementById('scheduleForm')?.addEventListener('submit', (event) => {
        event.preventDefault();
        const title = document.getElementById('scheduleTitle').value;
        const date = document.getElementById('modalDate').dataset.rawDate;

        const pathSegments = window.location.pathname.split('/');
        const rolePath = pathSegments[1]; 

        const formData = new URLSearchParams({ title, date });
        
        fetch(`/${rolePath}/calendar/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        }).then(res => {
            if(res.ok) {
                window.location.reload();
            } else {
                alert("保存失敗: " + res.status);
            }
        })
        .catch(err => console.error("Error:", err));
    });
});

window.addEventListener('pageshow', (event) => {
    if (event.persisted) initializeCalendar();
});