// currentViewMode 変数を追加
let currentViewMode = 'schedule'; // 'schedule' または 'attendance'

/**
 * 指定された年月のカレンダーHTMLを生成する
 */
function createCalendar(month, year) {
    // ( ... 既存の tableHTML, 日付計算ロジック ... )
    const monthDays = ["日", "月", "火", "水", "木", "金", "土"];
    let tableHTML = '<table class="calendar"><thead><tr>';
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
    // ( ... ロジックここまで ... )

    for (let i = 0; i < 6; i++) {
        tableHTML += '<tr>';

        for (let j = 0; j < 7; j++) {
            if (i === 0 && j < firstDay) {
                tableHTML += `<td class="mute">${prevDayCount}</td>`;
                prevDayCount++;
            } else if (dayCount > daysInMonth) {
                let nextMonthDayCount = dayCount - daysInMonth;
                tableHTML += `<td class="mute">${nextMonthDayCount}</td>`;
                dayCount++;
            } else {
                
                const dataDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayCount).padStart(2, '0')}`;

                //currentViewMode によって描画内容を分岐　
                let eventsHtml = '';
                if (currentViewMode === 'schedule') {
                    // 予定表モード
                    const dailyEvents = calendarEventsData.filter(event => event.date === dataDate);
                    if (dailyEvents.length > 0) {
                        eventsHtml = '<div class="schedule-list">';
                        dailyEvents.forEach(event => {
                            eventsHtml += `<div class="schedule-item">${event.title}</div>`; 
                        });
                        eventsHtml += '</div>';
                    }
                } else {

                    // attendanceRecordsData が null や undefined でないか確認 
                    if (Array.isArray(attendanceRecordsData)) {
                        
                        const attendanceRecord = attendanceRecordsData.find(record => record.date === dataDate);
                        
                        if (attendanceRecord) {
                            let attendanceMark = '';
                            if (attendanceRecord.status === '出席') {
                                attendanceMark = '<span class="attendance-present">〇</span>'; 
                            } else if (attendanceRecord.status === '欠席') {
                                attendanceMark = '<span class="attendance-absent">✕</span>'; 
                            }
                            if(attendanceMark) {
                               eventsHtml = `<div class="attendance-list">${attendanceMark}</div>`;
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

// ===================================================================
// メインの実行処理 (DOM読み込み後に実行)
// ===================================================================
document.addEventListener('DOMContentLoaded', () => {

    // --- HTMLから要素を取得 ---
    const yearInput = document.getElementById('yearInput');
    const monthInput = document.getElementById('monthInput');
    const jumpButton = document.getElementById('jumpButton');
    const calendarYmEl = document.getElementById('calendar-ym'); 
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); 

    //トグルボタンの要素を取得
    const viewToggleCheckbox = document.getElementById('viewToggleCheckbox');

    // ( ... renderCalendar, サーバー日付での初回描画, jumpButton のリスナー ... )

    function renderCalendar(month, year) {
        if (!calendarYmEl || !calendarTableContainerEl) {
            console.error('カレンダーの描画に必要なHTML要素が見つかりません。');
            return;
        }
        calendarYmEl.textContent = `${year}年 ${month + 1}月`;
        const tableHtml = createCalendar(month, year);
        calendarTableContainerEl.innerHTML = tableHtml;
    }

    let serverMonth, serverYear;
    if (typeof serverTargetMonthString === 'undefined') {
        console.error("HTML側に 'serverTargetMonthString' が定義されていません！");
        const fallbackDate = new Date();
        serverMonth = fallbackDate.getMonth();
        serverYear = fallbackDate.getFullYear();
        renderCalendar(serverMonth, serverYear);
    } else {
        const serverDate = new Date(serverTargetMonthString + "T00:00:00"); 
        serverMonth = serverDate.getMonth(); 
        serverYear = serverDate.getFullYear();
        if (yearInput) yearInput.value = serverYear;
        if (monthInput) monthInput.value = serverMonth; 
        renderCalendar(serverMonth, serverYear);
    }
    
    if (jumpButton) {
        jumpButton.addEventListener('click', () => {
            const selectedYear = parseInt(yearInput.value, 10);
            const selectedMonth = parseInt(monthInput.value, 10);
            if (!isNaN(selectedYear) && !isNaN(selectedMonth)) {
                location.href = `/student/main_calendar?month=${selectedYear}-${String(selectedMonth + 1).padStart(2, '0')}`;
            } else {
                alert("有効な年月を入力してください。");
            }
        });
    }

    //トグルボタンのイベントリスナーを追加　
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

    const modal = document.getElementById('scheduleModal');
    const closeButton = document.getElementById('closeButton');
    const scheduleForm = document.getElementById('scheduleForm');
    const modalTitle = document.getElementById('modalTitle');
    const modalDateEl = document.getElementById('modalDate');

    function openModal(dateStr) {
        const dateObj = new Date(dateStr + 'T00:00:00'); 
        const year = dateObj.getFullYear();
        const month = dateObj.getMonth() + 1;
        const day = dateObj.getDate();
        modalDateEl.textContent = `${year}年 ${month}月 ${day}日`;
        const existingListEl = document.getElementById('existing-events-list');
        if (existingListEl) {
            existingListEl.innerHTML = ''; 
            const dailyEvents = calendarEventsData.filter(event => event.date === dateStr);
            if (dailyEvents.length > 0) {
                dailyEvents.forEach(event => {
                    const itemHtml = `
                        <div class="existing-item">
                            <button class="delete-schedule-btn" data-id="${event.id}">×</button>
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
            //出欠モード時はモーダルを開かない
            if (currentViewMode === 'attendance') {
                return; 
            }
            // ▲▲▲

            const targetCell = event.target.closest('td[data-date]');
            if (targetCell) {
                const dateStr = targetCell.dataset.date; 
                openModal(dateStr);
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

                const index = calendarEventsData.findIndex(event => event.id == scheduleId); 
                if (index > -1) {
                    calendarEventsData.splice(index, 1);
                }
                const dateText = modalDateEl.textContent; 
                const parts = dateText.match(/(\d+)年 (\d+)月/);
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
            const dateText = modalDateEl.textContent; 
            const parts = dateText.match(/(\d+)年 (\d+)月 (\d+)日/);
            
            if (!parts) {
                console.error("モーダルの日付が不正です:", dateText);
                alert("日付が読み取れませんでした。");
                return;
            }
            
            const year = parts[1];
            const month = parts[2].padStart(2, '0');
            const day = parts[3].padStart(2, '0');
            const date = `${year}-${month}-${day}`; 

            const formData = new URLSearchParams();
            formData.append('title', title);
            formData.append('date', date);

            // 4. Spring Boot (Controller) に POST リクエストを送信
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