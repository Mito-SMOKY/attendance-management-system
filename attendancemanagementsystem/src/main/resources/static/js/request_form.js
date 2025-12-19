
//指定された年と月の最大日数を計算して返す。
function getDaysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

//日のドロップダウンリストを現在の年と月に基づいて再構築する。
function updateDayOptions(yearId, monthId, dayId) {
    const yearSelect = document.getElementById(yearId);
    const monthSelect = document.getElementById(monthId);
    const daySelect = document.getElementById(dayId);

    if (!yearSelect || !monthSelect || !daySelect) return;

    const year = parseInt(yearSelect.value, 10);
    const month = parseInt(monthSelect.value, 10);

    if (isNaN(year) || isNaN(month)) return;

    const days = getDaysInMonth(year, month);
    const currentDay = parseInt(daySelect.value, 10);

    // 一旦クリア
    daySelect.innerHTML = '';

    for (let d = 1; d <= days; d++) {
        const option = document.createElement('option');
        option.value = d;
        option.text = d + '日';
        if (d === currentDay) {
            option.selected = true;
        }
        daySelect.appendChild(option);
    }
}

// 画面読み込み時に初期化
document.addEventListener('DOMContentLoaded', () => {
    
    // --- 日付連動ロジック ---
    updateDayOptions('startYear', 'startMonth', 'startDay');
    
    const sYear = document.getElementById('startYear');
    const sMonth = document.getElementById('startMonth');
    if(sYear) sYear.addEventListener('change', () => updateDayOptions('startYear', 'startMonth', 'startDay'));
    if(sMonth) sMonth.addEventListener('change', () => updateDayOptions('startYear', 'startMonth', 'startDay'));

    updateDayOptions('endYear', 'endMonth', 'endDay');
    
    const eYear = document.getElementById('endYear');
    const eMonth = document.getElementById('endMonth');
    if(eYear) eYear.addEventListener('change', () => updateDayOptions('endYear', 'endMonth', 'endDay'));
    if(eMonth) eMonth.addEventListener('change', () => updateDayOptions('endYear', 'endMonth', 'endDay'));


    // --- 全選択チェックボックスのロジック ---
    const selectAllCheckbox = document.getElementById('selectAllPeriods');
    const periodCheckboxes = document.querySelectorAll('input[name="periods"]');

    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const isChecked = this.checked;
            periodCheckboxes.forEach(cb => {
                cb.checked = isChecked;
            });
        });

        periodCheckboxes.forEach(cb => {
            cb.addEventListener('change', () => {
                if (!cb.checked) {
                    selectAllCheckbox.checked = false;
                } else {
                    const allChecked = Array.from(periodCheckboxes).every(p => p.checked);
                    if (allChecked) {
                        selectAllCheckbox.checked = true;
                    }
                }
            });
        });
    }


    // --- モーダル制御ロジック ---
    const confirmBtn = document.getElementById('confirmBtn');
    const modal = document.getElementById('confirmationModal');
    const modalCancelBtn = document.getElementById('modalCancelBtn');
    const modalSubmitBtn = document.getElementById('modalSubmitBtn');
    const form = document.getElementById('requestForm');

    const modalPeriod = document.getElementById('modalPeriod');
    const modalTime = document.getElementById('modalTime');
    const modalReason = document.getElementById('modalReason');
    const modalApprover = document.getElementById('modalApprover');

    if (confirmBtn) {
        confirmBtn.addEventListener('click', () => {
            // 入力値の取得
            const sY = document.getElementById('startYear').value;
            const sM = document.getElementById('startMonth').value;
            const sD = document.getElementById('startDay').value;
            
            const eY = document.getElementById('endYear').value;
            const eM = document.getElementById('endMonth').value;
            const eD = document.getElementById('endDay').value;

            const reasonElement = document.querySelector('textarea[name="reason"]');
            const reason = reasonElement ? reasonElement.value : "";

            // 承認者取得 (新規追加)
            const approverSelect = document.getElementById('approverId');
            const approverOption = approverSelect.options[approverSelect.selectedIndex];
            // optionのtext(名前)を取得。未選択(value="")ならエラーにする
            if (!approverSelect.value) {
                alert("承認者を選択してください。");
                return;
            }
            const approverName = approverOption.text;

            // 時限取得
            const checkboxes = document.querySelectorAll('input[name="periods"]:checked');
            let selectedPeriods = [];
            checkboxes.forEach((cb) => {
                selectedPeriods.push(cb.value + "限");
            });

            // バリデーション
            if (selectedPeriods.length === 0) {
                alert("時限を選択してください。");
                return;
            }
            if (!reason || reason.trim() === "") {
                alert("理由を入力してください。");
                return;
            }

            // モーダルにセット
            modalApprover.textContent = approverName;
            modalPeriod.textContent = `${sY}年${sM}月${sD}日 〜 ${eY}年${eM}月${eD}日`;
            modalTime.textContent = selectedPeriods.join('・');
            modalReason.textContent = reason;

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