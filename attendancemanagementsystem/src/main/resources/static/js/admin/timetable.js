document.addEventListener('DOMContentLoaded', function() {
    
    // =========================================================
    // 1. 初期化パラメータの取得
    // =========================================================
    const container = document.getElementById('timetableContainer');

    const initialWeekStartStr = container ? container.dataset.weekStart : '';
    const initialDateStr      = container ? container.dataset.initialDate : '';
    const defaultUserId       = (container && container.dataset.initialUserId) ? container.dataset.initialUserId : 'admin001';

    const userSelect = document.getElementById('adminUserSelect');
    let currentUserId = userSelect ? userSelect.value : defaultUserId;

    if (userSelect) {
        userSelect.addEventListener('change', (event) => {
            currentUserId = event.target.value;
            fetchTimetableData(currentWeekStart); 
        });
    }

    // =========================================================
    // 2. 日付操作ヘルパー関数
    // =========================================================
    function parseISODateLocal(s) {
        if (!s) return new Date(NaN);
        if (s instanceof Date) return s;
        if (typeof s === 'string') {
            const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
            if (m) {
                return new Date(parseInt(m[1]), parseInt(m[2]) - 1, parseInt(m[3]));
            }
        }
        return new Date(s);
    }

    function formatDateYMD(date) {
        const d = (date instanceof Date) ? date : new Date(date);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    function toWeekStartMonday(d) {
        const date = parseISODateLocal(d);
        if (isNaN(date.getTime())) return new Date(NaN);
        const day = date.getDay(); 
        const diff = (day + 6) % 7; 
        date.setDate(date.getDate() - diff);
        date.setHours(0,0,0,0);
        return date;
    }

    // ★追加: 週番号をクライアント側で計算する関数
    function calculateWeekNumber(date) {
        // その月の1日を取得
        const firstDayOfMonth = new Date(date.getFullYear(), date.getMonth(), 1);
        const dayOfWeekFirst = firstDayOfMonth.getDay() || 7; // 1日が何曜日か(1=月...7=日)
        
        // 日付 + (1日の曜日オフセット) を7で割って切り上げ
        // ※日本のカレンダー表記(月曜始まりなど)に合わせた簡易計算
        //  (日付 + 1日の曜日 - 1) / 7
        const offsetDate = date.getDate() + dayOfWeekFirst - 1;
        return Math.floor((offsetDate - 1) / 7) + 1;
    }

    // =========================================================
    // 3. 状態変数の初期化
    // =========================================================
    let currentWeekStart;
    const parsedStart = parseISODateLocal(initialWeekStartStr || initialDateStr);

    if (isNaN(parsedStart.getTime())) {
        currentWeekStart = toWeekStartMonday(new Date());
    } else {
        currentWeekStart = toWeekStartMonday(parsedStart);
    }

    // =========================================================
    // 4. UIイベント設定
    // =========================================================
    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');
    const prevWeekBtn = document.getElementById('prevWeekBtn');
    const nextWeekBtn = document.getElementById('nextWeekBtn');

    function safeAddListener(el, ev, fn) { if (el) el.addEventListener(ev, fn); }

    safeAddListener(prevWeekBtn, 'click', () => changeWeek(-7));
    safeAddListener(nextWeekBtn, 'click', () => changeWeek(7));
    safeAddListener(yearSelector, 'change', handleMonthYearChange);
    safeAddListener(monthSelector, 'change', handleMonthYearChange);

    function initializeSelectors() {
        let baseDate = !isNaN(currentWeekStart.getTime()) ? currentWeekStart : new Date();
        const initDateObj = parseISODateLocal(initialDateStr);
        if (!isNaN(initDateObj.getTime())) baseDate = initDateObj;

        const yearToSelect = baseDate.getFullYear();
        const monthToSelect = baseDate.getMonth();

        if (yearSelector) {
            yearSelector.innerHTML = '';
            for (let y = yearToSelect - 1; y <= yearToSelect + 2; y++) {
                yearSelector.add(new Option(y + '年', y));
            }
            yearSelector.value = yearToSelect;
        }

        if (monthSelector) {
            monthSelector.innerHTML = '';
            for (let m = 0; m < 12; m++) {
                monthSelector.add(new Option((m + 1) + '月', m));
            }
            monthSelector.value = monthToSelect;
        }
        updateNavigationButtons();
    }

    function changeWeek(days) {
        if (isNaN(currentWeekStart.getTime())) return;
        const nextDate = new Date(currentWeekStart);
        nextDate.setDate(nextDate.getDate() + days);
        currentWeekStart = toWeekStartMonday(nextDate);
        fetchTimetableData(currentWeekStart);
    }

    function handleMonthYearChange() {
        if (!yearSelector || !monthSelector) return;
        const targetYear = parseInt(yearSelector.value);
        const targetMonth = parseInt(monthSelector.value);
        const tempDate = new Date(targetYear, targetMonth, 1);
        currentWeekStart = toWeekStartMonday(tempDate);
        fetchTimetableData(currentWeekStart);
    }

    // =========================================================
    // 5. データ取得 (API Fetch)
    // =========================================================
    function fetchTimetableData(startDate) {
        const mondayDate = toWeekStartMonday(startDate);
        if (isNaN(mondayDate.getTime())) return;

        const dateStr = formatDateYMD(mondayDate);
        const apiEndpoint = `/admin/api/timetabledata?date=${dateStr}&userId=${currentUserId}`;
        
        console.log(`Fetching timetable: ${apiEndpoint}`);

        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP Error ${response.status}`);
                return response.json();
            })
            .then(data => {
                const normalizedData = normalizeApiData(data, mondayDate);
                renderTimetable(normalizedData);
            })
            .catch(err => {
                console.error("時間割データ取得エラー:", err);
                renderTimetable(normalizeApiData({}, mondayDate)); 
            });
    }

    function normalizeApiData(rawData, requestedMonday) {
        const data = rawData || {};
        const serverWeekStart = data.weekStart ? parseISODateLocal(data.weekStart) : requestedMonday;
        const finalMonday = isNaN(serverWeekStart.getTime()) ? requestedMonday : serverWeekStart;
        
        data.weekStart = formatDateYMD(finalMonday);
        data.dates = [];
        for (let i = 0; i < 5; i++) {
            const d = new Date(finalMonday);
            d.setDate(d.getDate() + i);
            data.dates.push(formatDateYMD(d));
        }

        if (!data.timeSlots || !Array.isArray(data.timeSlots)) {
            data.timeSlots = ["9:30-11:00", "11:10-12:30", "13:30-14:50", "15:00-16:20"];
        }
        
        data.schedule = data.schedule || {};
        currentWeekStart = finalMonday;
        return data;
    }

    // =========================================================
    // 6. 描画処理 (Render)
    // =========================================================
    function renderTimetable(data) {
        const timetable = document.getElementById('weeklyTimetable');
        const tbody = document.getElementById('timetableBody');
        if (!timetable || !tbody) return;

        const theadRow = timetable.querySelector('thead tr');
        tbody.innerHTML = ''; 

        if (data.dates) {
            theadRow.innerHTML = '<th class="time-slot-header">時間</th>';
            const weekDays = ['日','月','火','水','木','金','土'];
            data.dates.forEach(dateStr => {
                const d = parseISODateLocal(dateStr);
                const label = `${d.getMonth() + 1}/${d.getDate()} (${weekDays[d.getDay()]})`;
                theadRow.innerHTML += `<th>${label}</th>`;
            });
        }

        updateWeekDisplay(data);

        data.timeSlots.forEach((slotTime, slotIndex) => {
            const row = tbody.insertRow();
            const timeCell = row.insertCell();
            timeCell.textContent = slotTime;
            timeCell.classList.add('time-slot-cell'); 

            for (let i = 0; i < 5; i++) {
                const dateKey = data.dates[i]; 
                const daySchedule = data.schedule[dateKey];
                const cell = row.insertCell();

                if (daySchedule && daySchedule[slotIndex]) {
                    const entry = daySchedule[slotIndex];
                    if (entry && entry.subject) {
                        cell.innerHTML = `
                            <div class="subject">${entry.subject}</div>
                            <div class="classroom">${entry.classroom || ''}</div>
                        `;
                    }
                }
            }
        });
        updateNavigationButtons();
    }

    function updateWeekDisplay(data) {
        const displayEl = document.getElementById('currentWeekDisplay');
        if (!displayEl) return;
        
        let displayMonth;
        if (monthSelector && !isNaN(parseInt(monthSelector.value))) {
            displayMonth = parseInt(monthSelector.value) + 1;
        } else {
            const middleDay = new Date(currentWeekStart);
            middleDay.setDate(middleDay.getDate() + 3);
            displayMonth = middleDay.getMonth() + 1;
        }

        // ★修正: サーバーからの weekNumber が無ければ、JSで計算した値を使う
        // calculateWeekNumber には週の代表日(水曜日あたり)を渡すと精度が良い
        const middleDay = new Date(currentWeekStart);
        middleDay.setDate(middleDay.getDate() + 3);
        
        const weekNum = data.weekNumber || calculateWeekNumber(middleDay);
        
        displayEl.textContent = `${displayMonth}月 第${weekNum}週`;
    }

    function isWeekInSelectedMonth(weekStartDate) {
        if (isNaN(weekStartDate.getTime())) return false;
        const selectedYear = yearSelector ? parseInt(yearSelector.value) : new Date().getFullYear();
        const selectedMonth = monthSelector ? parseInt(monthSelector.value) : new Date().getMonth();
        const middleDay = new Date(weekStartDate);
        middleDay.setDate(middleDay.getDate() + 3);
        return middleDay.getFullYear() === selectedYear && middleDay.getMonth() === selectedMonth;
    }

    function updateNavigationButtons() {
        if (!prevWeekBtn || !nextWeekBtn) return;
        if (isNaN(currentWeekStart.getTime())) {
            prevWeekBtn.disabled = true;
            nextWeekBtn.disabled = true;
            return;
        }
        const prevDate = new Date(currentWeekStart); prevDate.setDate(prevDate.getDate() - 7);
        const nextDate = new Date(currentWeekStart); nextDate.setDate(nextDate.getDate() + 7);

        prevWeekBtn.disabled = !isWeekInSelectedMonth(prevDate);
        nextWeekBtn.disabled = !isWeekInSelectedMonth(nextDate);
        
        prevWeekBtn.classList.toggle('disabled-arrow', prevWeekBtn.disabled);
        nextWeekBtn.classList.toggle('disabled-arrow', nextWeekBtn.disabled);
    }
    
    initializeSelectors();
    fetchTimetableData(currentWeekStart);
});