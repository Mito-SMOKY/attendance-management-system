// calendar.js の中

/**
 * 指定された年と月の最大日数を計算して返す。
 * @param {number} year - 年
 * @param {number} month - 月 (1〜12)
 * @returns {number} その月の最大日数
 */
function getDaysInMonth(year, month) {
    // Dateオブジェクトで、次の月の0日目を指定すると、現在の月の最終日が得られる
    // monthは1ベースだが、Dateのmonth引数は0ベース（0=1月, 11=12月）なので -1 する
    // 戻り値は1から始まる日の値
    return new Date(year, month, 0).getDate();
}

/**
 * 日のドロップダウンリストを現在の年と月に基づいて再構築する。
 * @param {string} yearId - 年のselect要素のID
 * @param {string} monthId - 月のselect要素のID
 * @param {string} dayId - 日のselect要素のID
 */
function updateDayOptions(yearId, monthId, dayId) {
    
    const yearSelect = document.getElementById(yearId);
    const monthSelect = document.getElementById(monthId);
    const daySelect = document.getElementById(dayId);

    if (!yearSelect || !monthSelect || !daySelect) {
        // 要素が見つからなければ処理を中断
        return;
    }

    // 現在選択されている値を取得し、数値に変換
    const selectedYear = parseInt(yearSelect.value, 10);
    const selectedMonth = parseInt(monthSelect.value, 10);
    const selectedDay = parseInt(daySelect.value, 10);

    // 最大日数を計算
    const daysInMonth = getDaysInMonth(selectedYear, selectedMonth);

    // 日のドロップダウンをクリア
    daySelect.innerHTML = '';

    // 新しい日付オプションを作成して追加
    for (let day = 1; day <= daysInMonth; day++) {
        const option = document.createElement('option');
        option.value = day;
        option.textContent = day;
        daySelect.appendChild(option);
    }
    
    // 以前選択されていた日があれば、その日（または最終日）を選択状態にする
    if (selectedDay > 0) {
        daySelect.value = Math.min(selectedDay, daysInMonth);
    } else {
        // デフォルトで1日を選択
        daySelect.value = 1;
    }
}



