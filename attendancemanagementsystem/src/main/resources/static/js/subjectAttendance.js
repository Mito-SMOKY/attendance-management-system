document.addEventListener('DOMContentLoaded', () => {
    
    const yearDropdown = document.getElementById('year-dropdown');
    const monthDropdown = document.getElementById('month-dropdown');

    // ★修正: 入学年度(studentAcademicYear)がある場合、プルダウンをJSで上書き生成
    if (typeof studentAcademicYear !== 'undefined' && studentAcademicYear && yearDropdown) {
        yearDropdown.innerHTML = '';
        
        // 入学年度から3年間を生成 (例: 2026, 2027, 2028)
        for (let i = 0; i < 3; i++) {
            const y = studentAcademicYear + i;
            const option = document.createElement('option');
            option.value = y;
            option.textContent = y; 
            
            if (typeof currentSelectedYear !== 'undefined' && y === currentSelectedYear) {
                option.selected = true;
            }
            yearDropdown.appendChild(option);
        }
    }
    
    const getSubjectId = () => {
        const input = document.getElementById('currentSubjectId');
        return input ? input.value : null;
    };

    const handleDropdownChange = () => {
        const selectedYear = yearDropdown.value;
        const selectedMonth = monthDropdown.value;
        fetchAttendanceData(selectedYear, selectedMonth);
    };

    if (yearDropdown && monthDropdown) {
        yearDropdown.addEventListener('change', handleDropdownChange);
        monthDropdown.addEventListener('change', handleDropdownChange);
    }

    function fetchAttendanceData(year, month) {
        const subjectId = getSubjectId();
        if (!subjectId) return;

        const apiEndpoint = `/student/api/data?subjectId=${subjectId}&year=${year}&month=${month}`;

        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) throw new Error('Network error');
                return response.json();
            })
            .then(data => {
                updateSummary(data);
                updateCalendar(data.dailyAttendanceList);
            })
            .catch(error => {
                console.error(error);
                alert('データ取得エラー');
            });
    }

    function updateSummary(data) {
        const setTxt = (id, txt) => {
            const el = document.getElementById(id);
            if(el) el.textContent = txt;
        };

        const subjectNameEl = document.querySelector('.subject-name');
        if(subjectNameEl) subjectNameEl.textContent = data.subjectName;

        setTxt('classroom-value', data.classroom);
        setTxt('teacher-value', data.teacherName);
        setTxt('required-classes-value', data.requiredClasses);
        
        const rateEl = document.getElementById('current-rate-value');
        if(rateEl) rateEl.textContent = (data.currentAttendanceRate * 100).toFixed(1) + '%';
        
        setTxt('max-absence-value', data.maxAbsenceClasses);
        
        const riskIcon = document.getElementById('risk-warning-icon');
        if (riskIcon) {
            riskIcon.classList.remove('visible', 'hidden');
            riskIcon.classList.add(data.maxAbsenceClasses <= 1 ? 'visible' : 'hidden');
        }

        setTxt('present-classes-value', `${data.presentClasses}コマ`);
        setTxt('absent-classes-value', `${data.absentClasses}コマ`);
        setTxt('late-classes-value', `${data.lateClasses}コマ`);
        setTxt('early-leave-classes-value', `${data.earlyLeaveClasses}コマ`); // ★早退
        setTxt('official-absent-value', `${data.officialAbsentClasses}コマ`);
        setTxt('official-pending-value', `${data.officialPendingClasses}コマ`);
    }

    function updateCalendar(dailyList) {
        const tbody = document.getElementById('calendar-body');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!dailyList || dailyList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">データなし</td></tr>';
            return;
        }

        dailyList.forEach(day => {
            const row = document.createElement('tr');
            row.insertAdjacentHTML('beforeend', `<td class="date-day">${day.dateDay}</td>`);

            if (day.classStatuses) {
                day.classStatuses.forEach(status => {
                    let statusClass = 'no-class';
                    if (status === '○') statusClass = 'present';
                    else if (status === '✕') statusClass = 'absent'; 
                    else if (status === '△') statusClass = 'late'; // 早退も△で来るのでここで処理される
                    else if (status === '公') statusClass = 'attendance-public'; 
                    
                    row.insertAdjacentHTML('beforeend', 
                        `<td class="status-cell"><span class="${statusClass}">${status}</span></td>`);
                });
            }
            row.insertAdjacentHTML('beforeend', `<td class="classroom-cell">${day.classroom}</td>`);
            tbody.appendChild(row);
        });
    }
});