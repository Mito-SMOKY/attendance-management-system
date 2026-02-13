// 月ごとの日数を取得
function getDaysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

// 日のプルダウンを更新
function updateDayOptions(yearId, monthId, dayId) {
    const yearSelect = document.getElementById(yearId);
    const monthSelect = document.getElementById(monthId);
    const daySelect = document.getElementById(dayId);

    if (!yearSelect || !monthSelect || !daySelect) return;

    const year = parseInt(yearSelect.value, 10);
    const month = parseInt(monthSelect.value, 10);
    
    // 現在選択されている日を保持（日数が減った場合に備えて調整）
    const currentDay = parseInt(daySelect.value, 10) || 1;

    // 日数を計算
    const days = getDaysInMonth(year, month);

    // 一旦クリア
    daySelect.innerHTML = '';

    for (let d = 1; d <= days; d++) {
        const option = document.createElement('option');
        option.value = d;
        option.text = d; // "日" はHTML側に記述しているので数字のみ
        if (d === currentDay) {
            option.selected = true;
        }
        daySelect.appendChild(option);
    }
}

// プルダウンの初期化（年・月の生成）
function initDateSelects() {
    const currentYear = new Date().getFullYear();
    const currentMonth = new Date().getMonth() + 1;
    const currentDay = new Date().getDate();

    const ids = [
        { y: 'startYear', m: 'startMonth', d: 'startDay' },
        { y: 'endYear', m: 'endMonth', d: 'endDay' }
    ];

    ids.forEach(obj => {
        const ySel = document.getElementById(obj.y);
        const mSel = document.getElementById(obj.m);
        const dSel = document.getElementById(obj.d);

        // 年 (今年〜翌年)
        for (let y = currentYear; y <= currentYear + 1; y++) {
            const opt = document.createElement('option');
            opt.value = y;
            opt.text = y;
            if (y === currentYear) opt.selected = true;
            ySel.appendChild(opt);
        }

        // 月 (1〜12)
        for (let m = 1; m <= 12; m++) {
            const opt = document.createElement('option');
            opt.value = m;
            opt.text = m;
            if (m === currentMonth) opt.selected = true;
            mSel.appendChild(opt);
        }

        // 初期化実行
        updateDayOptions(obj.y, obj.m, obj.d);
        // 今日を選択
        dSel.value = currentDay;

        // イベントリスナー登録
        ySel.addEventListener('change', () => updateDayOptions(obj.y, obj.m, obj.d));
        mSel.addEventListener('change', () => updateDayOptions(obj.y, obj.m, obj.d));
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // 日付プルダウン初期化
    initDateSelects();

    const confirmBtn = document.getElementById('confirmBtn');
    const modal = document.getElementById('confirmationModal');
    const modalCancelBtn = document.getElementById('modalCancelBtn');
    const modalSubmitBtn = document.getElementById('modalSubmitBtn');
    const form = document.getElementById('requestForm');

    // 確認ボタンクリック時
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            
            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }

            // 承認者
            const approverSelect = document.getElementById('approverSelect');
            let approverText = "";
            if (approverSelect.selectedIndex >= 0) {
                approverText = approverSelect.options[approverSelect.selectedIndex].text;
            }

            // 日付の取得と整形
            const sy = document.getElementById('startYear').value;
            const sm = document.getElementById('startMonth').value.padStart(2, '0');
            const sd = document.getElementById('startDay').value.padStart(2, '0');
            
            const ey = document.getElementById('endYear').value;
            const em = document.getElementById('endMonth').value.padStart(2, '0');
            const ed = document.getElementById('endDay').value.padStart(2, '0');

            const startDateStr = `${sy}-${sm}-${sd}`;
            const endDateStr = `${ey}-${em}-${ed}`;

            if (startDateStr > endDateStr) {
                alert("正しい日付を入力してください。");
                return; // ここで処理を中断し、モーダルを開かせない
            }


            // 隠しフィールドにセット (YYYY-MM-DD形式)
            document.getElementById('hiddenStartDate').value = `${sy}-${sm}-${sd}`;
            document.getElementById('hiddenEndDate').value = `${ey}-${em}-${ed}`;

            // 理由
            const reason = document.getElementById('reasonText').value;

            // 時限
            const checkboxes = document.querySelectorAll('input[name="periods"]:checked');
            let periodsText = "";
            if (checkboxes.length > 0) {
                const values = Array.from(checkboxes).map(cb => {
                    const span = cb.nextElementSibling; 
                    return span ? span.textContent : cb.value + "限";
                });
                periodsText = values.join("、");
            } else {
                alert("対象時限を少なくとも1つ選択してください。");
                return;
            }

            // モーダルへセット
            document.getElementById('modalApprover').textContent = approverText;
            document.getElementById('modalPeriod').textContent = `${sy}/${sm}/${sd} ～ ${ey}/${em}/${ed}`;
            document.getElementById('modalTime').textContent = periodsText;
            document.getElementById('modalReason').textContent = reason;

            // モーダル表示
            modal.style.display = 'flex';
        });
    }

    // キャンセルボタン
    if (modalCancelBtn) {
        modalCancelBtn.addEventListener('click', function() {
            modal.style.display = 'none';
        });
    }

    // 申請実行ボタン
    if (modalSubmitBtn) {
        modalSubmitBtn.addEventListener('click', function() {
            // hiddenフィールドはセット済みなのでそのまま送信
            form.submit();
        });
    }

    // 背景クリック
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    }
});