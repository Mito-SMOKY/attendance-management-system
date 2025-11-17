const date = new Date();
const today = date.getDate();
const currentMonth = date.getMonth();
const currentYear = date.getFullYear();

function createCalendar(month, year = date.getFullYear()) {

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
                // 今日の日付に class を付ける
                if (dayCount === today && month === currentMonth && year === currentYear) {
                    tableHTML += `<td class="today">${dayCount}</td>`;
                } else if (j === 0) {
                    tableHTML += `<td class="sun">${dayCount}</td>`;
                } else if (j === 6) {
                    tableHTML += `<td class="sat">${dayCount}</td>`;
                } else {
                    tableHTML += `<td>${dayCount}</td>`;
                }
                dayCount++;
            }
        }

        tableHTML += '</tr>';
        if (dayCount > daysInMonth && i >= 4) {
            break;
        }
    }

    tableHTML += '</tbody></table>';

    // 
    return tableHTML; 
}

const yearInput = document.getElementById('yearInput');
const monthInput = document.getElementById('monthInput');
const jumpButton = document.getElementById('jumpButton');
const calendarYmEl = document.getElementById('calendar-ym'); // 年月表示エリア
const calendarTableContainerEl = document.getElementById('calendar-table-container'); // 表のコンテナ

/*
 * (指定された年月で表とタイトルを更新する)
 * @param {number} month (0-11)
 * @param {number} year 
 */

function renderCalendar(month, year) {
    if (!calendarYmEl || !calendarTableContainerEl) {
        console.error('カレンダーの描画に必要なHTML要素が見つかりません。');
        return;
    }

    calendarYmEl.textContent = `${year}年 ${month + 1}月`;

    const tableHtml = createCalendar(month, year);
    calendarTableContainerEl.innerHTML = tableHtml;
}

if (yearInput && monthInput && jumpButton) {
    
    yearInput.value = currentYear;
    monthInput.value = currentMonth;

    jumpButton.addEventListener('click', () => {
        const selectedYear = parseInt(yearInput.value, 10);
        const selectedMonth = parseInt(monthInput.value, 10);

        if (!isNaN(selectedYear) && !isNaN(selectedMonth)) {
            renderCalendar(selectedMonth, selectedYear); // 指定された年月で再描画
        } else {
            alert("有効な年月を入力してください。");
        }
    });

    // C. 初回のカレンダーを描画
    renderCalendar(currentMonth, currentYear);

} else {
    console.error('カレンダーのコントロール要素が見つかりません。');
}