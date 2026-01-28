document.addEventListener('DOMContentLoaded', function() {
    
    // ▼▼▼ 設定：年の範囲 ▼▼▼
    const MIN_YEAR = 2025;
    const MAX_YEAR = 2028;

    // テンプレート変数の取得
    const initialWeekStartStr = (typeof window.initialWeekStartStr !== 'undefined') ? window.initialWeekStartStr : '';
    const initialDateStr = (typeof window.initialDateStr !== 'undefined') ? window.initialDateStr : '';
    const initialData = (typeof window.initialData !== 'undefined') ? window.initialData : null;
    
    let currentUserId; 
    
    const userSelect = document.getElementById('adminUserSelect');
    if (userSelect) {
        currentUserId = userSelect.value;
        window.currentUserId = currentUserId; 
        
        userSelect.addEventListener('change', (event) => {
            window.currentUserId = event.target.value;
            fetchTimetableData(currentWeekStart); 
        });
    } else {
        currentUserId = document.querySelector('body').dataset.initialUserId || 'admin001';
        window.currentUserId = currentUserId;
    }

    // 日付パース用
    function parseISODateLocal(s) {
        if (!s) return new Date(NaN);
        if (s instanceof Date) return s;
        if (typeof s === 'string') {
            const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
            if (m) {
                const y = parseInt(m[1], 10);
                const mo = parseInt(m[2], 10) - 1;
                const d = parseInt(m[3], 10);
                return new Date(y, mo, d);
            }
        }
        return new Date(s);
    }

    // YYYY-MM-DD 文字列生成
    function formatDateYMD(date) {
        const d = (date instanceof Date) ? date : new Date(date);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    // 日付を「その週の月曜日」に戻す関数
    function getMondayOfWeek(d) {
        const date = new Date(d);
        const day = date.getDay(); 
        const diff = (day + 6) % 7;
        date.setDate(date.getDate() - diff);
        date.setHours(0,0,0,0);
        return date;
    }

    let currentWeekStart;
    
    // 初期表示の日付設定
    const parsedStart = parseISODateLocal(initialWeekStartStr || initialDateStr);
    if (isNaN(parsedStart.getTime())) {
        currentWeekStart = getMondayOfWeek(new Date()); 
    } else {
        currentWeekStart = getMondayOfWeek(parsedStart);
    }
    
    // 初期日付が範囲外なら補正
    if (currentWeekStart.getFullYear() < MIN_YEAR) {
        currentWeekStart = new Date(MIN_YEAR, 0, 1);
        currentWeekStart = getMondayOfWeek(currentWeekStart);
    } else if (currentWeekStart.getFullYear() > MAX_YEAR) {
        currentWeekStart = new Date(MAX_YEAR, 11, 25); 
        currentWeekStart = getMondayOfWeek(currentWeekStart);
    }

    // DOM 要素
    const yearSelector = document.getElementById('yearSelector');
    const monthSelector = document.getElementById('monthSelector');
    const prevWeekBtn = document.getElementById('prevWeekBtn');
    const nextWeekBtn = document.getElementById('nextWeekBtn');
    const displayEl = document.getElementById('currentWeekDisplay');

    // ▼▼▼ 修正：ボタン位置固定のための強力なスタイル適用 ▼▼▼
    // 幅を min/max ともに固定し、inline-blockとして確実に領域を確保する
    if (displayEl) {
        displayEl.style.display = 'inline-block';
        
        // 日付文字列が長くても短くても、常にこのピクセル幅を確保します
        // 「12月31日 〜 12月31日」でも余裕があるサイズ(260px)に設定
        const FIXED_WIDTH = '260px'; 
        
        displayEl.style.width = FIXED_WIDTH;
        displayEl.style.minWidth = FIXED_WIDTH;
        displayEl.style.maxWidth = FIXED_WIDTH;
        
        displayEl.style.textAlign = 'center';
        displayEl.style.whiteSpace = 'nowrap';
        displayEl.style.verticalAlign = 'middle';
    }

    function safeAddListener(el, ev, fn) { if (el) el.addEventListener(ev, fn); }

    safeAddListener(prevWeekBtn, 'click', () => changeWeek(-7)); 
    safeAddListener(nextWeekBtn, 'click', () => changeWeek(7));
    safeAddListener(yearSelector, 'change', handleMonthYearChange);
    safeAddListener(monthSelector, 'change', handleMonthYearChange);

    function initializeSelectors() {
        let baseDate = new Date(currentWeekStart);
        baseDate.setDate(baseDate.getDate() + 4); 
        
        const yearToSelect = baseDate.getFullYear();
        const monthToSelect = baseDate.getMonth();

        if (yearSelector) {
            yearSelector.innerHTML = '';
            for (let y = MIN_YEAR; y <= MAX_YEAR; y++) {
                const option = new Option(y + '年', y);
                yearSelector.add(option);
            }
            if (yearToSelect >= MIN_YEAR && yearToSelect <= MAX_YEAR) {
                yearSelector.value = yearToSelect;
            } else {
                yearSelector.value = (yearToSelect < MIN_YEAR) ? MIN_YEAR : MAX_YEAR;
            }
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
        
        const nextDate = new Date(currentWeekStart);
        nextDate.setDate(nextDate.getDate() + days);
        
        const checkFriday = new Date(nextDate);
        checkFriday.setDate(checkFriday.getDate() + 4); 
        const checkYear = checkFriday.getFullYear();

        if (checkYear < MIN_YEAR || checkYear > MAX_YEAR) {
            console.log("移動先が年範囲外のためキャンセルしました");
            return;
        }

        currentWeekStart = nextDate;
        fetchTimetableData(currentWeekStart);
    }

    function handleMonthYearChange() {
        if (!yearSelector || !monthSelector) return;
        const targetYear = parseInt(yearSelector.value);
        const targetMonth = parseInt(monthSelector.value);
        
        let targetDate = new Date(targetYear, targetMonth, 1);
        const dayOfWeek = targetDate.getDay();

        if (dayOfWeek === 6) { 
            targetDate.setDate(targetDate.getDate() + 2);
        } else if (dayOfWeek === 0) {
            targetDate.setDate(targetDate.getDate() + 1);
        } else {
            targetDate = getMondayOfWeek(targetDate);
        }
        
        if (targetDate.getFullYear() < MIN_YEAR) {
            targetDate = new Date(MIN_YEAR, 0, 1);
            targetDate = getMondayOfWeek(targetDate);
        }

        currentWeekStart = targetDate;
        fetchTimetableData(currentWeekStart);
    }

    function fetchTimetableData(startDate) {
        if (isNaN(startDate.getTime())) return;

        const dateStr = formatDateYMD(startDate);
        const userId = window.currentUserId || 
                        document.getElementById('adminUserSelect')?.value || 
                        'admin001';
        const apiEndpoint = `/api/timetabledata?date=${dateStr}&userId=${userId}`;

        console.log('fetching timetable starting from:', dateStr);
        
        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(raw => {
                const startString = dateStr; 
                const startObj = parseISODateLocal(startString);
                
                const dates = [];
                for (let i = 0; i < 5; i++) {
                    const d = new Date(startObj);
                    d.setDate(d.getDate() + i);
                    dates.push(formatDateYMD(d));
                }

                const data = raw || {};
                data.weekStart = startString;
                data.dates = dates;

                data.timeSlots = Array.isArray(data.timeSlots) ? data.timeSlots : ["9:30-11:00","11:10-12:30","13:30-14:50","15:00-16:20"];
                data.schedule = data.schedule || {};

                currentWeekStart = startObj;

                renderTimetable(data);
            })
            .catch(err => {
                console.error("時間割データ取得エラー: ", err);
            });
    }

    function updateNavigationButtons() {
        if (!prevWeekBtn || !nextWeekBtn) return;
        
        const prevCheck = new Date(currentWeekStart);
        prevCheck.setDate(prevCheck.getDate() - 7);
        const prevCheckFri = new Date(prevCheck); 
        prevCheckFri.setDate(prevCheckFri.getDate() + 4);

        const nextCheck = new Date(currentWeekStart);
        nextCheck.setDate(nextCheck.getDate() + 7);
        const nextCheckFri = new Date(nextCheck);
        nextCheckFri.setDate(nextCheckFri.getDate() + 4);

        const canGoPrev = (prevCheckFri.getFullYear() >= MIN_YEAR);
        const canGoNext = (nextCheckFri.getFullYear() <= MAX_YEAR);

        prevWeekBtn.disabled = !canGoPrev;
        nextWeekBtn.disabled = !canGoNext;

        prevWeekBtn.classList.toggle('disabled-arrow', !canGoPrev);
        nextWeekBtn.classList.toggle('disabled-arrow', !canGoNext);
    }

    function renderTimetable(data) {
        const timetable = document.getElementById('weeklyTimetable');
        const tbody = document.getElementById('timetableBody');
        if (!timetable || !tbody) return;

        const theadRow = timetable.querySelector('thead tr');
        tbody.innerHTML = '';

        theadRow.innerHTML = '<th class="time-slot-header">時間</th>';
        const validDates = data.dates || [];
        
        validDates.forEach(dateStr => {
            const d = parseISODateLocal(dateStr);
            const label = (d.getMonth() + 1) + '/' + d.getDate() + ' (' + ['日','月','火','水','木','金','土'][d.getDay()] + ')';
            theadRow.innerHTML += `<th>${label}</th>`;
        });
        
        if (displayEl && validDates.length > 0) {
            const wStart = parseISODateLocal(validDates[0]); 
            const wEnd = parseISODateLocal(validDates[validDates.length - 1]);
            
            const fmt = (d) => `${d.getMonth() + 1}月${d.getDate()}日`;
            displayEl.textContent = `${fmt(wStart)} 〜 ${fmt(wEnd)}`;

            const checkDateForDropdown = new Date(wEnd); 

            if (yearSelector) {
                if (checkDateForDropdown.getFullYear() >= MIN_YEAR && checkDateForDropdown.getFullYear() <= MAX_YEAR) {
                    yearSelector.value = checkDateForDropdown.getFullYear();
                }
            }
            if (monthSelector) {
                monthSelector.value = checkDateForDropdown.getMonth();
            }
        }

        const slots = Array.isArray(data.timeSlots) ? data.timeSlots : [];
        slots.forEach((slotTime, slotIndex) => {
            const row = tbody.insertRow();
            row.insertCell().textContent = slotTime;
            
            for (let i = 0; i < 5; i++) {
                if (i < validDates.length) {
                    const dateKey = validDates[i];
                    const daySchedule = data.schedule ? data.schedule[dateKey] : null;
                    const cell = row.insertCell();
                    
                    if (daySchedule && daySchedule[slotIndex]) {
                        const entry = daySchedule[slotIndex];
                        if (entry && entry.subject) {
                            cell.innerHTML = `<div class="subject">${entry.subject}</div><div class="classroom">${entry.classroom || ''}</div>`;
                        }
                    }
                } else {
                    row.insertCell();
                }
            }
        });

        updateNavigationButtons();
    }

    if (initialData && initialData.schedule) {
        const startRaw = formatDateYMD(currentWeekStart);
        currentWeekStart = getMondayOfWeek(new Date(startRaw));
        const correctedStartStr = formatDateYMD(currentWeekStart);

        initialData.weekStart = correctedStartStr;
        
        const dates = [];
        for (let i = 0; i < 5; i++) {
            const d = new Date(currentWeekStart);
            d.setDate(d.getDate() + i);
            dates.push(formatDateYMD(d));
        }
        initialData.dates = dates;
        
        renderTimetable(initialData);
    } else {
        fetchTimetableData(currentWeekStart);
    }

    initializeSelectors();
});