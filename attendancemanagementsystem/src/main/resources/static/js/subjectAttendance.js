// attendanceDetail.js

document.addEventListener('DOMContentLoaded', () => {
    
    // 必要なDOM要素を取得
    const yearDropdown = document.getElementById('year-dropdown'); // 💡 年のプルダウンを追加
    const monthDropdown = document.getElementById('month-dropdown');
    
    // --- イベントリスナー ---
    
    /**
     * 年または月のプルダウンが変更された際に実行されるハンドラ
     */
    const handleDropdownChange = () => {
        const selectedYear = yearDropdown.value;
        const selectedMonth = monthDropdown.value;
        // データの取得と更新を実行
        fetchAttendanceData(selectedYear, selectedMonth);
    };

    // 年と月の両方の変更を監視
    if (yearDropdown && monthDropdown) {
        yearDropdown.addEventListener('change', handleDropdownChange);
        monthDropdown.addEventListener('change', handleDropdownChange);
    }


    // --- データ取得ロジック ---
    
    /**
     * サーバーから新しい月のデータを取得し、画面を更新する関数
     * @param {string} year - 選択された年 (例: "2024")
     * @param {string} month - 選択された月 (例: "11")
     */
    function fetchAttendanceData(year, month) {
        // 💡 APIエンドポイントを /student/api/data に修正
        const apiEndpoint = `/student/api/data?year=${year}&month=${month}`;

        fetch(apiEndpoint)
            .then(response => {
                if (!response.ok) {
                    throw new Error('サーバーからのデータ取得に失敗しました。HTTP Status: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                // 画面要素を新しいデータで更新
                updateSummary(data);
                updateCalendar(data.dailyAttendanceList);
            })
            .catch(error => {
                console.error('エラーが発生しました:', error);
                alert('データの読み込み中にエラーが発生しました。');
            });
    }

    // --- DOM更新ロジック ---
    
    /**
     * 左側と上部のサマリー情報を更新する
     * @param {object} data - AttendanceData オブジェクト
     */
    function updateSummary(data) {
        // 左側コンテナの更新
        document.querySelector('.subject-name').textContent = data.subjectName;
        document.getElementById('classroom-value').textContent = data.classroom;
        document.getElementById('teacher-value').textContent = data.teacherName;
        document.getElementById('required-classes-value').textContent = data.requiredClasses;
        
        const rateDisplay = (data.currentAttendanceRate * 100).toFixed(1) + '%';
        document.getElementById('current-rate-value').textContent = rateDisplay;
        
        document.getElementById('max-absence-value').textContent = data.maxAbsenceClasses;
        
        // リスクマークの更新
        const riskIcon = document.getElementById('risk-warning-icon');
        if (riskIcon) {
            riskIcon.classList.remove('visible', 'hidden');
            riskIcon.classList.add(data.maxAbsenceClasses <= 1 ? 'visible' : 'hidden');
        }

        // 上部サマリーの更新
        document.getElementById('present-classes-value').textContent = `${data.presentClasses}コマ`;
        document.getElementById('absent-classes-value').textContent = `${data.absentClasses}コマ`;
        document.getElementById('late-classes-value').textContent = `${data.lateClasses}コマ`;
        document.getElementById('official-absent-value').textContent = `${data.officialAbsentClasses}コマ`;
        document.getElementById('official-pending-value').textContent = `${data.officialPendingClasses}コマ`;
    }

    /**
     * カレンダーテーブルの tbody を更新する
     * @param {Array<object>} dailyList - DailyAttendance の配列
     */
    function updateCalendar(dailyList) {
        const tbody = document.getElementById('calendar-body');
        tbody.innerHTML = ''; // 既存の行をクリア

        dailyList.forEach(day => {
            const row = document.createElement('tr');
            
            // 1. 日付
            row.insertAdjacentHTML('beforeend', `<td class="date-day">${day.dateDay}</td>`);

            // 2. 各時限のステータス
            day.classStatuses.forEach(status => {
                let statusClass = 'no-class';
                if (status === '○') statusClass = 'present';
                if (status === '✕') statusClass = 'absent'; 
                if (status === '△') statusClass = 'late'; 
                if (status === '○') statusClass = 'attendance-public'; 
                
                row.insertAdjacentHTML('beforeend', 
                    `<td class="status-cell"><span class="${statusClass}">${status}</span></td>`);
            });

            // 3. 教室
            row.insertAdjacentHTML('beforeend', `<td class="classroom-cell">${day.classroom}</td>`);

            tbody.appendChild(row);
        });
    }
});