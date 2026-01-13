/* sadminEmergencyMain.js */

document.addEventListener('DOMContentLoaded', () => {
    console.log("JS読み込み完了: sadminEmergencyMain.js");

    // 1. 年・月・日のセレクトボックスに数字を入れる関数
    function generateDateOptions() {
        const currentYear = new Date().getFullYear();
        const currentMonth = new Date().getMonth() + 1;
        const currentDay = new Date().getDate();

        const targetIds = [
            { y: 'startYear', m: 'startMonth', d: 'startDay' },
            { y: 'endYear', m: 'endMonth', d: 'endDay' }
        ];

        targetIds.forEach(obj => {
            const yearSelect = document.getElementById(obj.y);
            const monthSelect = document.getElementById(obj.m);
            const daySelect = document.getElementById(obj.d);

            if (!yearSelect || !monthSelect) return;

            // 年 (前後5年)
            yearSelect.innerHTML = ''; 
            for (let y = currentYear - 1; y <= currentYear + 1; y++) {
                const opt = document.createElement('option');
                opt.value = y;
                opt.text = y;
                if (y === currentYear) opt.selected = true;
                yearSelect.appendChild(opt);
            }

            // 月 (1～12)
            monthSelect.innerHTML = '';
            for (let m = 1; m <= 12; m++) {
                const opt = document.createElement('option');
                opt.value = m;
                opt.text = m;
                if (m === currentMonth) opt.selected = true;
                monthSelect.appendChild(opt);
            }

            // 日の初期生成
            updateDayOptions(obj.y, obj.m, obj.d);
            
            // 今日を選択状態にする
            if(daySelect) daySelect.value = currentDay;
        });
    }

    // 2. 年・月に合わせて、日の日数（28～31）を計算しなおす関数
    function updateDayOptions(yearId, monthId, dayId) {
        const yearSelect = document.getElementById(yearId);
        const monthSelect = document.getElementById(monthId);
        const daySelect = document.getElementById(dayId);

        if (!yearSelect || !monthSelect || !daySelect) return;

        const year = parseInt(yearSelect.value, 10);
        const month = parseInt(monthSelect.value, 10);

        if (isNaN(year) || isNaN(month)) return;

        // 月末日を取得
        const daysInMonth = new Date(year, month, 0).getDate();
        const currentDay = parseInt(daySelect.value, 10);

        // 日の選択肢を再生成
        daySelect.innerHTML = '';

        for (let d = 1; d <= daysInMonth; d++) {
            const option = document.createElement('option');
            option.value = d;
            option.text = d;
            if (d === currentDay) {
                option.selected = true;
            }
            daySelect.appendChild(option);
        }
    }

    // --- 初期実行 ---
    generateDateOptions();

    // --- イベント登録 ---
    ['start', 'end'].forEach(prefix => {
        const yId = prefix + 'Year';
        const mId = prefix + 'Month';
        const dId = prefix + 'Day';
        
        const yEl = document.getElementById(yId);
        const mEl = document.getElementById(mId);

        if(yEl) yEl.addEventListener('change', () => updateDayOptions(yId, mId, dId));
        if(mEl) mEl.addEventListener('change', () => updateDayOptions(yId, mId, dId));
    });

    // --- モーダル表示とエラーチェック ---
    const confirmBtn = document.getElementById('confirmBtn');
    const modal = document.getElementById('confirmationModal');
    const modalCancelBtn = document.getElementById('modalCancelBtn');
    const modalSubmitBtn = document.getElementById('modalSubmitBtn');
    const form = document.getElementById('emergencyForm');

    if (confirmBtn) {
        confirmBtn.addEventListener('click', () => {
            // 値を取得
            const sY = document.getElementById('startYear').value;
            const sM = document.getElementById('startMonth').value;
            const sD = document.getElementById('startDay').value;
            
            const eY = document.getElementById('endYear').value;
            const eM = document.getElementById('endMonth').value;
            const eD = document.getElementById('endDay').value;
            
            const type = document.getElementById('typeSelect').value;
            const course = document.getElementById('courseSelect').value;
            const grade = document.getElementById('gradeSelect').value;
            const sClass = document.getElementById('classSelect').value;
            const period = document.getElementById('periodSelect').value;
            const remarks = document.getElementById('remarksInput').value;
            const notify = document.getElementById('notifyToggle').checked;

            // ▼▼▼ 追加したチェック処理 ▼▼▼
            
            // 1. 必須入力チェック
            if (!type) {
                alert("種類を選択してください");
                return;
            }

            // 2. 日付の前後関係チェック
            // Dateオブジェクトを作成して比較します (月は0始まりなので -1 します)
            const startDate = new Date(sY, sM - 1, sD);
            const endDate = new Date(eY, eM - 1, eD);

            if (startDate > endDate) {
                alert("日付の指定が正しくありません。\n終了日は開始日と同じか、それより後の日付にしてください。");
                return; // ここで処理をストップ（モーダルを出さない）
            }

            // ▲▲▲ チェック処理ここまで ▲▲▲


            // モーダルに内容をセット
            document.getElementById('modalTypeTitle').textContent = type;
            document.getElementById('modalDateStart').textContent = `${sY}/${sM}/${sD}`;
            document.getElementById('modalDateEnd').textContent = `${eY}/${eM}/${eD}`;
            document.getElementById('modalCourse').textContent = course || '指定なし';
            document.getElementById('modalGrade').textContent = grade || '指定なし';
            document.getElementById('modalClass').textContent = sClass || '指定なし';
            document.getElementById('modalPeriod').textContent = period || '指定なし';
            document.getElementById('modalRemarks').textContent = remarks || 'なし';
            document.getElementById('modalNotify').textContent = notify ? 'あり' : 'なし';

            // モーダルを表示
            modal.style.display = 'flex';
        });
    }

    if (modalCancelBtn) {
        modalCancelBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }

    if (modalSubmitBtn) {
        modalSubmitBtn.addEventListener('click', () => {
            form.submit();
        });
    }
});