// currentViewMode 変数を追加
let currentViewMode = 'schedule'; // 'schedule' または 'attendance'

/**
 * 指定された年月のカレンダーHTMLを生成する
 */
function createCalendar(month, year) {
    const monthDays = ["日", "月", "火", "水", "木", "金", "土"];
    let tableHTML = '<table class="calendar"><thead><tr>';
    // 曜日ヘッダーの生成
    for (let i = 0; i < 7; i++) {
        if (i === 0 || i === 6) {
            tableHTML += `<th class="sun">${monthDays[i]}</th>`;
        } else {
            tableHTML += `<th>${monthDays[i]}</th>`;
        }
    }
    tableHTML += '</tr></thead><tbody>';
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInPrevMonth = new Date(year, month, 0).getDate();
    let dayCount = 1;
    let prevDayCount = daysInPrevMonth - firstDay + 1;
    const dateObj = new Date();
    const today = dateObj.getDate();
    const currentMonth = dateObj.getMonth();
    const currentYear = dateObj.getFullYear();

    // 6週間分の行を生成
    for (let i = 0; i < 6; i++) {
        tableHTML += '<tr>';

        // 7日分のセルを生成
        for (let j = 0; j < 7; j++) {
            if (i === 0 && j < firstDay) {
                tableHTML += `<td class="mute">${prevDayCount}</td>`;
                prevDayCount++;
            } else if (dayCount > daysInMonth) {
                let nextMonthDayCount = dayCount - daysInMonth;
                tableHTML += `<td class="mute">${nextMonthDayCount}</td>`;
                dayCount++;
            } else {
                // データ日付文字列を生成 (YYYY-MM-DD)
                const dataDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayCount).padStart(2, '0')}`;

                // currentViewMode によって描画内容を分岐
                let eventsHtml = '';
                if (currentViewMode === 'schedule') {
                    // === 予定表モード ===
                    if (Array.isArray(calendarEventsData)) {
                        const dailyEvents = calendarEventsData.filter(event => event.date === dataDate);
                        if (dailyEvents.length > 0) {
                            eventsHtml = '<div class="schedule-list">';
                            dailyEvents.forEach(event => {
                                eventsHtml += `<div class="schedule-item">${event.title}</div>`; 
                            });
                            eventsHtml += '</div>';
                        }
                    }
                } else {
                    // === 出欠モード ===
                    if (typeof attendanceRecordsData !== 'undefined' && Array.isArray(attendanceRecordsData)) {
                        
                        const dailyRecords = attendanceRecordsData.filter(record => record.date === dataDate);
                        
                        if (dailyRecords.length > 0) {
                            let displayMark = '';
                            let displayClass = '';

                            const isAllPresent = dailyRecords.every(r => r.status === '出席');
                            const isAllAbsent = dailyRecords.every(r => r.status === '欠席');
                            const isAllPublic = dailyRecords.every(r => r.status === '公欠');

                            if (isAllPresent) {
                                displayMark = '〇';
                                displayClass = 'attendance-present';
                            } else if (isAllAbsent) {
                                displayMark = '✕';
                                displayClass = 'attendance-absent';
                            } else if (isAllPublic) {
                                displayMark = '〇';
                                displayClass = 'attendance-public';
                            } else {
                                displayMark = '△';
                                displayClass = 'attendance-late';
                            }

                            if (displayMark) {
                                eventsHtml = `<div class="attendance-list"><span class="${displayClass}">${displayMark}</span></div>`;
                            }
                        }
                    }
                }

                let cellClass = '';
                if (dayCount === today && month === currentMonth && year === currentYear) {
                    cellClass = 'today';
                } else if (j === 0) {
                    cellClass = 'sun';
                } else if (j === 6) {
                    cellClass = 'sat';
                }

                tableHTML += `<td class="${cellClass}" data-date="${dataDate}">
                                <div class="day-number">${dayCount}</div>
                                ${eventsHtml}
                                </td>`;
                
                dayCount++;
            }
        }
        tableHTML += '</tr>';
        if (dayCount > daysInMonth && i >= 4) {
            break;
        }
    }
    tableHTML += '</tbody></table>';
    return tableHTML;
}

function renderCalendar(month, year) {
    const calendarYmEl = document.getElementById('calendar-ym'); 
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); 
    const yearInput = document.getElementById('yearInput');
    const monthInput = document.getElementById('monthInput');

    if (!calendarYmEl || !calendarTableContainerEl) {
        return;
    }
    calendarYmEl.textContent = `${year}年 ${month + 1}月`;
    const tableHtml = createCalendar(month, year);
    calendarTableContainerEl.innerHTML = tableHtml;

    if (yearInput) yearInput.value = year;
    if (monthInput) monthInput.value = month; 
}


// ===================================================================
// ページ初期化処理 (修正箇所)
// ===================================================================

function initializeCalendar() {
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    let serverMonth, serverYear;
    
    // 1. URLからパラメータを取得 (?date=...&mode=...)
    const urlParams = new URLSearchParams(window.location.search);
    const modeParam = urlParams.get('mode');

    // 2. モードの復元処理
    if (modeParam === 'attendance') {
        currentViewMode = 'attendance';
        if (viewToggleCheckbox) {
            viewToggleCheckbox.checked = true; // トグルボタンをONにする
        }
    } else {
        currentViewMode = 'schedule';
        if (viewToggleCheckbox) {
            viewToggleCheckbox.checked = false;
        }
    }

    // 3. 日付の復元処理
    if (typeof serverTargetMonthString === 'undefined' || !serverTargetMonthString) {
        const fallbackDate = new Date();
        serverMonth = fallbackDate.getMonth();
        serverYear = fallbackDate.getFullYear();
    } else {
        const serverDate = new Date(serverTargetMonthString + "T00:00:00"); 
        serverMonth = serverDate.getMonth(); 
        serverYear = serverDate.getFullYear();
    }

    renderCalendar(serverMonth, serverYear);
}


document.addEventListener('DOMContentLoaded', () => {

    const yearInput = document.getElementById('yearInput');
    const monthInput = document.getElementById('monthInput');
    const jumpButton = document.getElementById('jumpButton');
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); 

    initializeCalendar();

    //表示ボタンのクリックイベント
    if (jumpButton) {
        jumpButton.addEventListener('click', () => {
            const selectedYear = parseInt(yearInput.value, 10);
            const selectedMonth = parseInt(monthInput.value, 10);
            
            // 現在のモードを取得
            const currentMode = viewToggleCheckbox && viewToggleCheckbox.checked ? 'attendance' : 'schedule';

            if (!isNaN(selectedYear) && !isNaN(selectedMonth)) {
                // URLに &mode=... を追加して遷移
                location.href = `/student/main_calendar?month=${selectedYear}-${String(selectedMonth + 1).padStart(2, '0')}&mode=${currentMode}`;
            } else {
                alert("有効な年月を入力してください。");
            }
        });
    }

    // トグルボタンのイベントリスナー
    if (viewToggleCheckbox) {
        viewToggleCheckbox.addEventListener('change', () => {
            if (viewToggleCheckbox.checked) {
                currentViewMode = 'attendance'; 
            } else {
                currentViewMode = 'schedule'; 
            }
            const currentSelectedYear = parseInt(yearInput.value, 10);
            const currentSelectedMonth = parseInt(monthInput.value, 10);
            if (!isNaN(currentSelectedYear) && !isNaN(currentSelectedMonth)) {
                renderCalendar(currentSelectedMonth, currentSelectedYear);
            }
        });
    }
    
    // モーダル関連 & 画面遷移ロジック

    const modal = document.getElementById('scheduleModal');
    const closeButton = document.getElementById('closeButton');
    const scheduleForm = document.getElementById('scheduleForm');
    const modalDateEl = document.getElementById('modalDate');

    function openModal(dateStr) {
        const dateObj = new Date(dateStr + 'T00:00:00'); 
        const year = dateObj.getFullYear();
        const month = dateObj.getMonth() + 1;
        const day = dateObj.getDate();
        
        modalDateEl.textContent = `${year}年 ${month}月 ${day}日`;
        modalDateEl.dataset.rawDate = dateStr; 

        const existingListEl = document.getElementById('existing-events-list');
        if (existingListEl) {
            existingListEl.innerHTML = ''; 
            const dailyEvents = calendarEventsData.filter(event => event.date === dateStr);
            if (dailyEvents.length > 0) {
                dailyEvents.forEach(event => {
                    const itemHtml = `
                        <div class="existing-item">
                            <button class="delete-schedule-btn" type="button" data-id="${event.calendarId}">×</button>
                            <span>${event.title}</span>
                        </div>
                    `;
                    existingListEl.innerHTML += itemHtml;
                });
            } else {
                existingListEl.innerHTML = '<p>登録済みの予定はありません。</p>';
            }
        }
        scheduleForm.reset(); 
        modal.style.display = 'flex'; 
    }

    function closeModal() {
        modal.style.display = 'none';
    }

    if (calendarTableContainerEl) {
        calendarTableContainerEl.addEventListener('click', (event) => {
            const targetCell = event.target.closest('td[data-date]');
            if (targetCell) {
                const dateStr = targetCell.dataset.date;
                
                if (currentViewMode === 'attendance') {
                    window.location.href = `/student/attendance/date?date=${dateStr}`;
                } else {
                    openModal(dateStr);
                }
            }
        });
    }

    function handleDeleteSchedule(button) {
        const scheduleId = button.dataset.id;
        
        if (!confirm('この予定を削除してもよろしいですか？')) {
            return; 
        }

        fetch(`/student/calendar/delete/${scheduleId}`, {
            method: 'DELETE',
        })
        .then(response => {
            if (response.ok) {
                alert('予定を削除しました。');
                button.closest('.existing-item').remove();

                const index = calendarEventsData.findIndex(event => event.calendarId == scheduleId); 
                if (index > -1) {
                    calendarEventsData.splice(index, 1);
                }
                const parts = modalDateEl.textContent.match(/(\d+)年 (\d+)月/);
                if (parts) {
                    const year = parseInt(parts[1], 10);
                    const month = parseInt(parts[2], 10) - 1;
                    renderCalendar(month, year);
                }
            } else {
                alert('削除に失敗しました。サーバーエラーが発生しました。');
            }
        })
        .catch(error => {
            console.error('削除処理で通信エラー:', error);
            alert('削除中に通信エラーが発生しました。');
        });
    }

    if (closeButton) {
        closeButton.addEventListener('click', closeModal);
    }
    if (modal) {
        modal.addEventListener('click', (event) => {
            const deleteButton = event.target.closest('.delete-schedule-btn');
            if (deleteButton) {
                event.stopPropagation(); 
                handleDeleteSchedule(deleteButton); 
                return;
            }
            if (event.target === modal) {
                closeModal();
            }
        });
    }

    if (scheduleForm) {
        scheduleForm.addEventListener('submit', (event) => {
            event.preventDefault(); 
            const title = document.getElementById('scheduleTitle').value;
            
            const date = modalDateEl.dataset.rawDate; 
            
            if (!date) {
                console.error("日付データが見つかりません。");
                alert("日付エラーが発生しました。");
                return;
            }

            const formData = new URLSearchParams();
            formData.append('title', title);
            formData.append('date', date);

            fetch('/student/calendar/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    alert("予定を保存しました。");
                    window.location.reload(); 
                } else {
                    alert("保存に失敗しました。サーバーエラーが発生しました。");
                }
            })
            .catch(error => {
                console.error('保存処理で通信エラー:', error);
                alert("保存中に通信エラーが発生しました。");
            });
        });
    }
});

// BFcache対策
window.addEventListener('pageshow', function(event) {
    if (event.persisted) {
        initializeCalendar();
    }
});