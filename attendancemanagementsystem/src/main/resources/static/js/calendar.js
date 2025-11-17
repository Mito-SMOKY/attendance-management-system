
//CSRF対策用ヘルパーをインポート 
import { fetchWithCSRF } from './csrf_helper.js';

// HTMLのグローバル変数(window.)から、このファイルで使うための変数を定義する
const calendarEventsData = window.calendarEventsData || [];
const serverTargetMonthString = window.serverTargetMonthString;
// (出欠データも同様に読み込む)
const attendanceRecordsData = window.attendanceRecordsData || []; 
// ▲▲▲

/**
 * 指定された年月のカレンダーHTMLを生成する
 * (この関数は、HTMLで定義された 'calendarEventsData' グローバル変数を参照します)
 */
function createCalendar(month, year) {

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

    // --- 今日の日付情報を取得 (todayクラス用) ---
    const dateObj = new Date();
    const today = dateObj.getDate();
    const currentMonth = dateObj.getMonth();
    const currentYear = dateObj.getFullYear();

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
                // --- ▼▼▼【修正点 1: DBデータ(予定)の表示ロジック】▼▼▼ ---
                
                // (1) このマスの日付文字列 (YYYY-MM-DD) を作成
                const dataDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayCount).padStart(2, '0')}`;

                // (2) グローバル変数 'calendarEventsData' から、この日付の予定を探す
                let eventsHtml = '';
                if (typeof calendarEventsData !== 'undefined' && Array.isArray(calendarEventsData)) {
                    // event.date が "YYYY-MM-DD" 形式である前提
                    const dailyEvents = calendarEventsData.filter(event => event.date === dataDate);
                    
                    if (dailyEvents.length > 0) {
                        eventsHtml = '<div class="schedule-list">'; // (CSSは別途必要)
                        dailyEvents.forEach(event => {
                            // (CSSは別途必要)
                            eventsHtml += `<div class="schedule-item">${event.title}</div>`; 
                        });
                        eventsHtml += '</div>';
                    }
                }

                // (3) セルのCSSクラスを決定
                let cellClass = '';
                if (dayCount === today && month === currentMonth && year === currentYear) {
                    cellClass = 'today';
                } else if (j === 0) {
                    cellClass = 'sun';
                } else if (j === 6) {
                    cellClass = 'sat';
                }

                // (4) <td> を生成 (日付(dayCount) と 予定(eventsHtml) を両方入れる)
                tableHTML += `<td class="${cellClass}" data-date="${dataDate}">
                                <div class="day-number">${dayCount}</div>
                                ${eventsHtml}
                             </td>`;
                // --- ▲▲▲【修正完了】▲▲▲ ---
                
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
    const calendarYmEl = document.getElementById('calendar-ym'); // 年月表示エリア
    const calendarTableContainerEl = document.getElementById('calendar-table-container'); // 表のコンテナ

    /**
     * (指定された年月で表とタイトルを更新する)
     */
    function renderCalendar(month, year) {
        if (!calendarYmEl || !calendarTableContainerEl) {
            console.error('カレンダーの描画に必要なHTML要素が見つかりません。');
            return;
        }
        // 年月タイトルを更新
        calendarYmEl.textContent = `${year}年 ${month + 1}月`;
        // カレンダーの表HTMLを生成して挿入
        const tableHtml = createCalendar(month, year);
        calendarTableContainerEl.innerHTML = tableHtml;
    }

    // --- ▼▼▼【修正点 2: サーバー(Java)から渡された月で初回描画する】▼▼▼ ---
    if (typeof serverTargetMonthString === 'undefined') {
        // (HTMLの <script> タグが失敗した場合のフォールバック)
        console.error("HTML側に 'serverTargetMonthString' が定義されていません！");
        const fallbackDate = new Date();
        renderCalendar(fallbackDate.getMonth(), fallbackDate.getFullYear());
    } else {
        // サーバーから指定された月 (例: "2025-11-01") でカレンダーを描画
        const serverDate = new Date(serverTargetMonthString + "T00:00:00"); // タイムゾーン対策
        const serverMonth = serverDate.getMonth(); // 0-11
        const serverYear = serverDate.getFullYear();

        // 年/月ジャンプの <input> にもサーバーの月を初期値として設定
        if (yearInput) yearInput.value = serverYear;
        if (monthInput) monthInput.value = serverMonth; // (value="0"～"11")

        // ★初回のカレンダーを描画
        renderCalendar(serverMonth, serverYear);
    }
    
    // --- 「表示」ボタンのロジック ---
    // (注意: このボタンはJSだけでカレンダーを再描画するため、DBの予定は反映されません)
    // (本当にDBの予定を再取得したい場合は、ページをリロードさせる必要があります)
    if (jumpButton) {
        jumpButton.addEventListener('click', () => {
            const selectedYear = parseInt(yearInput.value, 10);
            const selectedMonth = parseInt(monthInput.value, 10);

            if (!isNaN(selectedYear) && !isNaN(selectedMonth)) {
                // (★改善案★: 本当はここでページをリロードさせるのが望ましい)
                // location.href = `/student/home?month=${selectedYear}-${String(selectedMonth + 1).padStart(2, '0')}`;
                
                // 現在の実装: JSだけでカレンダーを再描画 (DBデータは反映されない)
                renderCalendar(selectedMonth, selectedYear);
            } else {
                alert("有効な年月を入力してください。");
            }
        });
    }
    // --- ▲▲▲【修正完了】▲▲▲ ---


    // --- モーダル関連の要素取得 ---
    const modal = document.getElementById('scheduleModal');
    const closeButton = document.getElementById('closeButton');
    const scheduleForm = document.getElementById('scheduleForm');
    const modalTitle = document.getElementById('modalTitle');
    const modalDateEl = document.getElementById('modalDate');

    // モーダルを開く関数
    function openModal(dateStr) {
        const dateObj = new Date(dateStr + 'T00:00:00'); // タイムゾーンずれ対策
        const year = dateObj.getFullYear();
        const month = dateObj.getMonth() + 1;
        const day = dateObj.getDate();

        // モーダルの日付タイトルを設定 (例: 2025年 11月 20日)
        modalDateEl.textContent = `${year}年 ${month}月 ${day}日`;
        scheduleForm.reset(); // フォームの中身をリセット
        modal.style.display = 'flex'; // モーダルを表示
    }

    // モーダルを閉じる関数
    function closeModal() {
        modal.style.display = 'none';
    }

    // カレンダーのセル（<td>）がクリックされたらモーダルを開く
    if (calendarTableContainerEl) {
        calendarTableContainerEl.addEventListener('click', (event) => {
            // クリックされた要素、またはその親要素が 'td[data-date]' かどうか
            const targetCell = event.target.closest('td[data-date]');
            
            if (targetCell) {
                const dateStr = targetCell.dataset.date; // "YYYY-MM-DD"
                openModal(dateStr);
            }
        });
    }

    // 閉じるボタン
    if (closeButton) {
        closeButton.addEventListener('click', closeModal);
    }

    // 背景クリックで閉じる
    if (modal) {
        modal.addEventListener('click', (event) => {
            if (event.target === modal) {
                closeModal();
            }
        });
    }

    // --- ▼▼▼【修正点 3: フォーム送信(submit)で、/student/calendar/add に POST する】▼▼▼ ---
    if (scheduleForm) {
        scheduleForm.addEventListener('submit', (event) => {
            event.preventDefault(); // 本来のフォーム送信（ページリロード）をキャンセル

            // 1. フォームからデータを取得
            const title = document.getElementById('scheduleTitle').value;
            // const time = document.getElementById('scheduleTime').value; // (DBにtimeカラムないので一旦保留)
            
            // 2. モーダルの日付 (例: "2025年 11月 20日") を "YYYY-MM-DD" 形式に変換
            const dateText = modalDateEl.textContent; 
            const parts = dateText.match(/(\d+)年 (\d+)月 (\d+)日/);
            
            if (!parts) {
                console.error("モーダルの日付が不正です:", dateText);
                alert("日付が読み取れませんでした。");
                return;
            }
            
            const year = parts[1];
            const month = parts[2].padStart(2, '0'); // "11"
            const day = parts[3].padStart(2, '0'); // "20"
            const date = `${year}-${month}-${day}`; // "2025-11-20"

            // 3. Controller の @RequestParam ("title", "date") に合わせたデータを作成
            const formData = new URLSearchParams();
            formData.append('title', title);
            formData.append('date', date);

            // 4. Spring Boot (Controller) に POST リクエストを送信
            fetchWithCSRF('/student/calendar/add', {
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