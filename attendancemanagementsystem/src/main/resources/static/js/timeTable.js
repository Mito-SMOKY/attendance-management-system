document.addEventListener('DOMContentLoaded', function() {
    
    // テンプレートで window にセットされている想定（timeTable.html の <script th:inline="javascript">）
    const initialWeekStartStr = (typeof window.initialWeekStartStr !== 'undefined') ? window.initialWeekStartStr : '';
    const initialDateStr = (typeof window.initialDateStr !== 'undefined') ? window.initialDateStr : '';
    const initialData = (typeof window.initialData !== 'undefined') ? window.initialData : null;
    
    console.log('window.initialWeekStartStr', initialWeekStartStr, 'initialDateStr', initialDateStr, 'initialData?', !!initialData);

    let currentUserId; 
    
    const userSelect = document.getElementById('adminUserSelect');
    if (userSelect) {
        // プルダウンがあれば初期値を設定し、イベントリスナーを追加
        currentUserId = userSelect.value;
        window.currentUserId = currentUserId; // 他の関数からも参照できるように設定
        
        userSelect.addEventListener('change', (event) => {
            // ユーザーが選択した新しいIDを取得
            window.currentUserId = event.target.value;
            
            // 選択されたユーザーの時間割を再取得・再描画する
            fetchTimetableData(currentWeekStart); 
        });
    } else {
        // プルダウンがなければデフォルト値を設定
        currentUserId = document.querySelector('body').dataset.initialUserId || 'admin001';
        window.currentUserId = currentUserId;
    }


    function parseISODateLocal(s) {
        if (!s) return new Date(NaN);
        if (s instanceof Date) return s;
        // 期待フォーマット: "YYYY-MM-DD"（サーバー側でこの形式を渡す想定）
        if (typeof s === 'string') {
            const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
            if (m) {
                const y = parseInt(m[1], 10);
                const mo = parseInt(m[2], 10) - 1;
                const d = parseInt(m[3], 10);
                // Date(year, monthIndex, day) の形式でローカル日付を強制的に作成
                return new Date(y, mo, d);
            }
        }
        // fallback
        return new Date(s);
    }

    function formatDateYMD(date) {
        const d = (date instanceof Date) ? date : new Date(date);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    // 与えた日付を「その週の月曜日」に揃えるヘルパー
    function toWeekStartMonday(d) {
        const date = (typeof d === 'string') ? parseISODateLocal(d) : parseISODateLocal(d);
        if (isNaN(date.getTime())) return new Date(NaN);
        const day = date.getDay(); // 0=Sun ... 6=Sat
        const diff = (day + 6) % 7; // Mon->0, Sun->6
        date.setDate(date.getDate() - diff);
        date.setHours(0,0,0,0);
        return date;
    }

    let currentWeekStart;
    
    // 1. HTMLから渡された日付文字列をパース
    const parsedStart = parseISODateLocal(initialWeekStartStr || initialDateStr);

    if (isNaN(parsedStart.getTime())) {
        // 2. パース失敗 (NaN) の場合、現在日にフォールバック
        console.error("初期日付文字列が無効です。現在日を基準に設定します。");
        currentWeekStart = toWeekStartMonday(new Date());
    } else {
        // 3. パース成功の場合、その週の月曜日に正規化
        currentWeekStart = toWeekStartMonday(parsedStart);
    }
    // --------------------------------------------------------------------------


    // DOM 要素
    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');
    const prevWeekBtn = document.getElementById('prevWeekBtn');
    const nextWeekBtn = document.getElementById('nextWeekBtn');

    function safeAddListener(el, ev, fn) { if (el) el.addEventListener(ev, fn); }

    safeAddListener(prevWeekBtn, 'click', () => changeWeek(-7));
    safeAddListener(nextWeekBtn, 'click', () => changeWeek(7));
    safeAddListener(yearSelector, 'change', handleMonthYearChange);
    safeAddListener(monthSelector, 'change', handleMonthYearChange);

    // 年月プルダウン初期化
    function initializeSelectors() {
        // 基準日は currentWeekStart（既に月曜）
        let baseDate = !isNaN(currentWeekStart.getTime()) ? currentWeekStart : new Date();

        // initialDateStr が有効なら参照（ただし currentWeekStart を壊さない）
        const initDate = parseISODateLocal(initialDateStr);
        if (!isNaN(initDate.getTime())) baseDate = initDate;

        const yearToSelect = !isNaN(baseDate.getFullYear()) ? baseDate.getFullYear() : new Date().getFullYear();
        const monthToSelect = !isNaN(baseDate.getMonth()) ? baseDate.getMonth() : new Date().getMonth();

        if (yearSelector) {
            yearSelector.innerHTML = '';
            for (let y = yearToSelect - 1; y <= yearToSelect + 2; y++) {
                const option = new Option(y + '年', y);
                yearSelector.add(option);
            }
            yearSelector.value = yearToSelect;
        }

        if (monthSelector) {
            monthSelector.innerHTML = '';
            for (let m = 0; m < 12; m++) {
                const monthName = (m + 1) + '月';
                const option = new Option(monthName, m);
                monthSelector.add(option);
            }
            monthSelector.value = monthToSelect;
        }

        updateNavigationButtons();
        
    }

    function changeWeek(days) {
        if (isNaN(currentWeekStart.getTime())) return;
        const nextWeekStart = new Date(currentWeekStart);
        nextWeekStart.setDate(nextWeekStart.getDate() + days);
        // 整合性確保：必ず月曜に揃える
        const normalized = toWeekStartMonday(nextWeekStart);
        currentWeekStart = normalized;
        fetchTimetableData(currentWeekStart);
    }

    function handleMonthYearChange() {
        if (!yearSelector || !monthSelector) return;
        const targetYear = parseInt(yearSelector.value);
        const targetMonth = parseInt(monthSelector.value);
        const tempDate = new Date(targetYear, targetMonth, 1);
        currentWeekStart = toWeekStartMonday(tempDate);
        console.log('handleMonthYearChange -> currentWeekStart:', currentWeekStart.toISOString().split('T')[0]);
        fetchTimetableData(currentWeekStart);
    }

    // API 呼び出し：必ず startDate を月曜に正規化して送る。レスポンスの dates は使わず自前生成する。
    function fetchTimetableData(startDate) {
        startDate = toWeekStartMonday(startDate);
        if (isNaN(startDate.getTime())) {
            console.error("日付データが無効です。API呼び出しをスキップします。");
            return;
        }

        const dateStr = formatDateYMD(startDate);
        const userId = window.currentUserId || 
                        document.getElementById('adminUserSelect')?.value || 
                        'admin001';
        const apiEndpoint = `/api/timetabledata?date=${dateStr}&userId=${userId}`;

        console.log('fetching timetable for weekStart (mon):', dateStr);
        
        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(raw => {
                console.log("API raw response:", raw);
                // 強制的に月曜基準の weekStart にする（サーバーが違っていてもこちらで統一）
                const normalized = toWeekStartMonday(raw && raw.weekStart ? raw.weekStart : startDate);
                const weekStartStr = isNaN(normalized.getTime()) ? formatDateYMD(startDate) : formatDateYMD(normalized);
    
                // 常に月曜〜金曜の日付配列を自前生成（表示の整合性を保つ）
                const dates = [];
                for (let i = 0; i < 5; i++) {
                    const d = new Date(normalized);
                    d.setDate(d.getDate() + i);
                    dates.push(formatDateYMD(d));
                }

                const data = raw || {};
                data.weekStart = weekStartStr;
                data.dates = dates;

                // timeSlots / schedule が無ければ空で埋める（安全）
                data.timeSlots = Array.isArray(data.timeSlots) ? data.timeSlots : (data.timeSlots ? data.timeSlots : ["9:30-11:00","11:10-12:30","13:30-14:50","15:00-16:20"]);
                data.schedule = data.schedule || {};

                // currentWeekStart を更新
                currentWeekStart = parseISODateLocal(weekStartStr);

                console.log("normalized/enforced weekStart:", data.weekStart, "dates:", data.dates);
                renderTimetable(data);
            })
            .catch(err => {
                console.error("時間割データ取得エラー: ", err);
            });
    }

    function isWeekInSelectedMonth(weekStartDate) {
        if (isNaN(weekStartDate.getTime())) return false;
        const currentSelectedYear = yearSelector ? parseInt(yearSelector.value) : (new Date()).getFullYear();
        const currentSelectedMonth = monthSelector ? parseInt(monthSelector.value) : (new Date()).getMonth();
        const middleDay = new Date(weekStartDate);
        middleDay.setDate(middleDay.getDate() + 3);
        return middleDay.getFullYear() === currentSelectedYear && middleDay.getMonth() === currentSelectedMonth;
    }

    function updateNavigationButtons() {
        if (!prevWeekBtn || !nextWeekBtn) return;
        
        // 日付が無効な場合のみボタンを無効化
        if (isNaN(currentWeekStart.getTime())) {
            prevWeekBtn.disabled = true; 
            nextWeekBtn.disabled = true; 
            return;
        }
        prevWeekBtn.disabled = false;
        nextWeekBtn.disabled = false;
        prevWeekBtn.classList.remove('disabled-arrow');
        nextWeekBtn.classList.remove('disabled-arrow');
    }

    function renderTimetable(data) {
        const timetable = document.getElementById('weeklyTimetable');
        const tbody = document.getElementById('timetableBody');
        if (!timetable || !tbody) return;

        const theadRow = timetable.querySelector('thead tr');
        tbody.innerHTML = '';

        // header: 月曜〜金曜を ISO 日付で表示（必要ならフォーマット変更）
        if (data.dates && data.dates.length === 5) {
            theadRow.innerHTML = '<th class="time-slot-header">時間</th>';
            data.dates.forEach(dateStr => {
                const d = parseISODateLocal(dateStr);
                const label = (d.getMonth() + 1) + '/' + d.getDate() + ' (' + ['日','月','火','水','木','金','土'][d.getDay()] + ')';
                theadRow.innerHTML += `<th>${label}</th>`;
            });
        }

        const displayEl = document.getElementById('currentWeekDisplay');
        
        if (displayEl && data.dates && data.dates.length >= 5) {
            // data.dates[0] が月曜、data.dates[4] が金曜です
            const startDate = parseISODateLocal(data.dates[0]);
            const endDate = parseISODateLocal(data.dates[4]);

            // 「M月D日」の形式を作成
            const startStr = `${startDate.getMonth() + 1}月${startDate.getDate()}日`;
            const endStr = `${endDate.getMonth() + 1}月${endDate.getDate()}日`;

            // 表示を更新 (例: 2月9日 ～ 2月13日)
            displayEl.textContent = `${startStr} ～ ${endStr}`;
        } else if (displayEl) {
            displayEl.textContent = '日付範囲不明';
        }
        
        // body: 各時間帯ごとに行を追加
        const slots = Array.isArray(data.timeSlots) ? data.timeSlots : [];
        slots.forEach((slotTime, slotIndex) => {
            const row = tbody.insertRow();
            row.insertCell().textContent = slotTime;
            for (let i = 0; i < 5; i++) {
                const weekStartDate = parseISODateLocal(data.weekStart);
                const date = new Date(weekStartDate);
                date.setDate(date.getDate() + i);
                const dateKey = formatDateYMD(date);
                const daySchedule = data.schedule ? data.schedule[dateKey] : null;
                const cell = row.insertCell();
                if (daySchedule && daySchedule[slotIndex]) {
                    const entry = daySchedule[slotIndex];
                    if (entry && entry.subject) {
                        cell.innerHTML = `<div class="subject">${entry.subject}</div><div class="classroom">${entry.classroom || ''}</div>`;
                    }
                }
            }
        });

        if (data.dates && data.dates.length >= 1) {
            // 週の中日（水曜日あたり）を基準に「何月か」を判定
            const weekMiddleDate = parseISODateLocal(data.dates[2] || data.dates[0]);
            
            if (yearSelector) {
                yearSelector.value = weekMiddleDate.getFullYear();
            }
            if (monthSelector) {
                monthSelector.value = weekMiddleDate.getMonth();
            }
        }

        updateNavigationButtons();
    }

    
    console.log('初期 currentWeekStart:', isNaN(currentWeekStart.getTime()) ? 'invalid' : currentWeekStart.toISOString().split('T')[0]);

    if (initialData && initialData.schedule) {
        console.log("初期データをレンダリング（正規化）");
        // normalize initialData
        const normStart = toWeekStartMonday(initialData.weekStart || currentWeekStart);
        initialData.weekStart = isNaN(normStart.getTime()) ? formatDateYMD(currentWeekStart) : formatDateYMD(normStart);
        // generate dates mon-fri
        initialData.dates = [];
        for (let i = 0; i < 5; i++) {
            const d = new Date(normStart);
            d.setDate(d.getDate() + i);
            initialData.dates.push(formatDateYMD(d));
        }
        // ensure timeslots/schedule
        initialData.timeSlots = Array.isArray(initialData.timeSlots) ? initialData.timeSlots : ["9:30-11:00","11:10-12:30","13:30-14:50","15:00-16:20"];
        initialData.schedule = initialData.schedule || {};
        currentWeekStart = parseISODateLocal(initialData.weekStart);
        renderTimetable(initialData);
    } else {
        // API呼び出しで初期データを取得
        const dateToFetch = currentWeekStart;
        console.log('fetching for weekStart:', dateToFetch.toISOString().split('T')[0]);
        fetchTimetableData(dateToFetch);
    }

    // --- 初期化 ---
    initializeSelectors();
});