// 週表示するカレンダー用

// 現在表示している週の開始日を保持 (Dateオブジェクト)
let currentWeekStartDate = new Date(); 
// 時間割データやイベントデータを格納するグローバル変数 (サーバーから渡される想定)
let weeklyScheduleData = []; 
let weeklyAttendanceData = [];

/**
 * 渡された日付が含まれる週の月曜日（週の開始日）を取得する。
 * @param {Date} date - 基準となる日付
 * @returns {Date} その週の月曜日
 */
function getWeekStart(date) {
    const d = new Date(date);
    const day = d.getDay(); // 0:日, 1:月, ..., 6:土
    // ISO標準に従い、日曜日の場合(0)は6日戻すことで前週の月曜日にする
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    d.setHours(0, 0, 0, 0); // 時刻をリセット
    return d;
}

/**
 * Dateオブジェクトを 'YYYY-MM-DD' 形式の文字列に変換する。
 * @param {Date} date - 変換する日付
 * @returns {string} 'YYYY-MM-DD' 形式の文字列
 */
function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}