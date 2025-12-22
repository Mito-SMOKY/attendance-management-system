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
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); 

    initializeCalendar();

    // --- 自動更新ロジック (年・月・トグルの変更を監視) ---
    const handleUpdate = () => {
        const selectedYear = parseInt(yearInput.value, 10);
        const selectedMonth = parseInt(monthInput.value, 10);
        const currentMode = viewToggleCheckbox && viewToggleCheckbox.checked ? 'attendance' : 'schedule';

        if (!isNaN(selectedYear) && !isNaN(selectedMonth)) {
            // 月を2桁に整形してURLを生成し、画面を自動遷移（更新）
            const formattedMonth = String(selectedMonth + 1).padStart(2, '0');
            location.href = `/student/main_calendar?month=${selectedYear}-${formattedMonth}&mode=${currentMode}`;
        }
    };

    // 年・月が変更されたら即座に実行
    yearInput?.addEventListener('change', handleUpdate);
    monthInput?.addEventListener('change', handleUpdate);

    // トグルボタンのイベントリスナー（ここも画面遷移に合わせる場合はhandleUpdateに変更）
    if (viewToggleCheckbox) {
        viewToggleCheckbox.addEventListener('change', handleUpdate);
    }
    
    // --- モーダル・削除・保存ロジック (以下、変更なし) ---

    const modal = document.getElementById('scheduleModal');
    const closeButton = document.getElementById('closeButton');
    const scheduleForm = document.getElementById('scheduleForm');
    const modalDateEl = document.getElementById('modalDate');

    // ... (中略: openModal, closeModal などの既存コード) ...

    if (calendarTableContainerEl) {
        calendarTableContainerEl.addEventListener('click', (event) => {
            const targetCell = event.target.closest('td[data-date]');
            if (targetCell) {
                const dateStr = targetCell.dataset.date;
                // currentViewModeの代わりにトグルの状態で判定
                const mode = viewToggleCheckbox && viewToggleCheckbox.checked ? 'attendance' : 'schedule';
                if (mode === 'attendance') {
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