const date = new Date();
const today = date.getDate();
const currentMonth = date.getMonth();
const currentYear = date.getFullYear();

function createCalendar(month, year = date.getFullYear()) {

    const monthDays = ["日", "月", "火", "水", "木", "金", "土"];
    // 年月表示

    let calendarHTML =`
        <div class="calendar-header">
    
            <div class="calendar-ym">${year}年 ${month + 1}月</div>
                <p>予定表</p>
                <label class="toggle-button">
                <input type="checkbox"/>
                </label>
                <p>出欠</p>
    
        </div>
    `;

    calendarHTML += '<table class="calendar"><thead><tr>';

    for (let i = 0; i < 7; i++) {
        if (i === 0 || i === 6) {
            calendarHTML += `<th class="sun">${monthDays[i]}</th>`;
        } else {
            calendarHTML += `<th>${monthDays[i]}</th>`;
        }
    }

    calendarHTML += '</tr></thead><tbody>';

    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    let dayCount = 1;
    let prevDayCount = daysInPrevMonth - firstDay + 1;

    for (let i = 0; i < 6; i++) {
        calendarHTML += '<tr>';

        for (let j = 0; j < 7; j++) {
            if (i === 0 && j < firstDay) {
                calendarHTML += `<td class="mute">${prevDayCount}</td>`;
                prevDayCount++;
            } else if (dayCount > daysInMonth) {
                let nextMonthDayCount = dayCount - daysInMonth;
                calendarHTML += `<td class="mute">${nextMonthDayCount}</td>`;
                dayCount++;
            } else {
                // 今日の日付に class を付ける（currentYear を参照）
                if (dayCount === today && month === currentMonth && year === currentYear) {
                    calendarHTML += `<td class="today">${dayCount}</td>`;
                } else if (j === 0) {
                    calendarHTML += `<td class="sun">${dayCount}</td>`;
                } else if (j === 6) {
                    calendarHTML += `<td class="sat">${dayCount}</td>`;
                } else {
                    calendarHTML += `<td>${dayCount}</td>`;
                }
                dayCount++;
            }
        }

        calendarHTML += '</tr>';
        // 月の全日が表示されたらループを終了
        if (dayCount > daysInMonth && i >= 4) {
            break;
        }
    }

    calendarHTML += '</tbody></table>';

    return calendarHTML;
}

const calEl = document.getElementById('calendar');
if (calEl) {
    calEl.innerHTML = createCalendar(currentMonth, currentYear);
} else {
    console.error('Element with id="calendar" not found.');
}

document.getElementById('calendar').innerHTML = createCalendar(currentMonth);