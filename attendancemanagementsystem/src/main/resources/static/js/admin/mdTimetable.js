/**
 * mdTimetable.js
 * 時間割管理画面の共通スクリプト
 */

$(document).ready(function() {

    // --- 2. Flatpickr (一括登録用・期間選択) ---
    // startとendを別々に初期化して連動させる方式
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    if (startDateInput && endDateInput) {
        
        // 終了日の設定
        const fpEnd = flatpickr("#endDate", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true 
        });

        // 開始日の設定
        flatpickr("#startDate", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true,
            onChange: function(selectedDates, dateStr, instance) {
                // 開始日が変更されたら、終了日の最小日(minDate)をその日に設定する
                fpEnd.set('minDate', dateStr);
            }
        });
    }

    // --- 3. Flatpickr (日別編集・参照用) ---
    if (document.getElementById("dateInput")) {
        flatpickr("#dateInput", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true
        });
    }
});

/* =========================================
 * 一括登録画面 (mdTimetable.html) 用
 * ========================================= */

/**
 * 年度と学期から、開始日・終了日の目安をセットする
 * (手動ボタン用)
 */
function setAutoDates() {
    const year = document.getElementById('yearSelect').value;
    const term = document.getElementById('termSelect').value;
    
    const startElem = document.getElementById("startDate");
    const endElem = document.getElementById("endDate");

    let startStr = "";
    let endStr = "";

    if (term == '1') {
        startStr = year + '-04-01';
        endStr = year + '-09-30';
    } else {
        startStr = year + '-10-01';
        endStr = (parseInt(year) + 1) + '-03-31';
    }

    // Flatpickrにセット
    if (startElem && startElem._flatpickr) {
        startElem._flatpickr.setDate(startStr);
    } else if (startElem) {
        startElem.value = startStr;
    }

    if (endElem && endElem._flatpickr) {
        endElem._flatpickr.setDate(endStr);
    } else if (endElem) {
        endElem.value = endStr;
    }
}

/**
 * AI画像解析のアップロード処理 (時間割画像用)
 */
async function uploadImage() {
    const fileInput = document.getElementById('timetableImage');
    const loadingMsg = document.getElementById('loadingMsg');
    const analyzeBtn = document.getElementById('analyzeBtn');

    if (fileInput.files.length === 0) {
        alert("ファイルを選択してください");
        return;
    }

    loadingMsg.style.display = 'block';
    analyzeBtn.disabled = true;

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    try {
        const response = await fetch('/admin/mdTimetable/analyze-image', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) throw new Error("Server Error");

        const data = await response.json();
        if (data.length === 0) {
            alert("データを読み取れませんでした。");
        } else {
            const count = applyTimetableData(data);
            alert(count + "件のデータを自動入力しました！");
        }
        
    } catch (e) {
        console.error(e);
        alert("解析中にエラーが発生しました。");
    } finally {
        loadingMsg.style.display = 'none';
        analyzeBtn.disabled = false;
    }
}

/* 期間の自動セット ＆ 休日の抽出*/
async function uploadSchedulePdf() {
    const fileInput = document.getElementById('schedulePdf');
    const loadingMsg = document.getElementById('pdfLoadingMsg');
    const analyzeBtn = document.getElementById('analyzePdfBtn');
    
    const yearSelect = document.getElementById('yearSelect');
    const termSelect = document.getElementById('termSelect'); // 学期も送信
    const outputArea = document.getElementById('excludedDates');
    
    // 日付入力欄の取得
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');

    // 1. クラスプルダウンから表示テキスト（例: "情報システム科 1年"）を取得
    const selectedText = $("#departmentId option:selected").text(); 
    
    // 2. "1年" や "2年" という数字を抜き出す
    const gradeMatch = selectedText.match(/([0-9]+)年/);
    let targetGrade = 1; // デフォルト
    
    if (gradeMatch) {
        targetGrade = gradeMatch[1];
    } else {
        if ($("#departmentId").val() !== "" && !confirm("クラス名から学年が読み取れませんでした。\n1年生として処理して良いですか？")) {
            return;
        }
    }

    if (fileInput.files.length === 0) {
        alert("PDFファイルを選択してください");
        return;
    }

    // クラス未選択チェック
    if ($("#departmentId").val() === "") {
        alert("先に対象クラス（学科）を選択してください。\n※学年を特定するために必要です。");
        return;
    }

    loadingMsg.style.display = 'block';
    analyzeBtn.disabled = true;

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);
    formData.append("year", yearSelect.value); 
    formData.append("term", termSelect.value); // ★学期を追加
    formData.append("targetGrade", targetGrade);

    try {
        const response = await fetch('/admin/mdTimetable/analyzePdf', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error("Server Error: " + response.status);

        const result = await response.json(); 
        console.log("解析結果:", result);

        // --- 1. 期間の自動セット ---
        if (result.startDate && result.endDate) {
            // Flatpickr経由で値をセット
            if (startDateInput && startDateInput._flatpickr) startDateInput._flatpickr.setDate(result.startDate);
            else if (startDateInput) startDateInput.value = result.startDate;

            if (endDateInput && endDateInput._flatpickr) endDateInput._flatpickr.setDate(result.endDate);
            else if (endDateInput) endDateInput.value = result.endDate;
        }

        // --- 2. 休日リストのセット ---
        const dateList = result.holidays || [];

        if (dateList.length === 0 && (!result.startDate || !result.endDate)) {
            // 期間も休日も取れなかった場合
            alert("PDFから有効な情報を読み取れませんでした。");
        } else {
            // 休日があればセット
            if (dateList.length > 0) {
                outputArea.value = dateList.join(', ');
            }

            // メッセージ作成
            let msg = `【${targetGrade}年生】のスケジュール解析完了\n`;
            if (result.startDate && result.endDate) {
                msg += `■ 期間: ${result.startDate} ～ ${result.endDate}\n`;
            }
            msg += `■ 休日: ${dateList.length}日分を抽出しました`;
            alert(msg);
        }

    } catch (e) {
        console.error(e);
        alert("解析に失敗しました。サーバーエラーが発生した可能性があります。");
    } finally {
        loadingMsg.style.display = 'none';
        analyzeBtn.disabled = false;
    }
}

/**
 * 解析データを画面のプルダウンに反映
 */
function applyTimetableData(data) {
    let count = 0;
    data.forEach(item => {
        const nameBase = `scheduleMap[${item.slot}]['${item.day}']`;
        const subjectEl = $(`select[name="${nameBase}.subjectId"]`);
        const roomEl    = $(`select[name="${nameBase}.classroomId"]`);
        const teacherEl = $(`select[name="${nameBase}.userId"]`);

        if (subjectEl.length === 0) return; 

        if (item.subjectId) {
            subjectEl.val(item.subjectId).trigger('change');
            count++;
        }
        if (item.classroomId && roomEl.length > 0) {
            roomEl.val(item.classroomId).trigger('change');
        }
        if (item.userId && teacherEl.length > 0) {
            teacherEl.val(item.userId).trigger('change');
        }
    });
    return count;
}

/**
 * 重複チェック後に送信
 */
async function checkAndSubmit() {
    const deptId = document.getElementById('departmentId').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const form = document.getElementById('registerForm');

    if (!deptId || !startDate || !endDate) {
        alert("クラスと期間を正しく入力してください。");
        return;
    }

    try {
        const response = await fetch(`/admin/mdTimetable/check-overlap?departmentId=${deptId}&startDate=${startDate}&endDate=${endDate}`);
        
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }

        const data = await response.json();

        if (data.exists) {
            const msg = "【⚠️ データ重複警告】\n\n" + 
                        "指定期間内に既にデータがあります。\n" +
                        "上書き（更新）してもよろしいですか？";
            
            if (confirm(msg)) {
                form.submit();
            }
        } else {
            if (confirm("時間割を一括登録します。よろしいですか？")) {
                form.submit();
            }
        }

    } catch (error) {
        console.error("Check Error:", error);
        if (confirm("通信エラーが発生しましたが、登録を続行しますか？")) {
            form.submit();
        }
    }
}

/* =========================================
 * 参照画面 (mdTimetableView.html) 用
 * ========================================= */
function setReferenceDate() {
    const year = document.getElementById('yearSelect').value;
    const term = document.getElementById('termSelect').value;
    const dateInput = document.getElementById('dateInput');

    if (!dateInput) return;

    let dateStr = "";
    if (term == '1') {
        dateStr = year + '-04-01';
    } else {
        dateStr = year + '-10-01';
    }

    if (dateInput._flatpickr) {
        dateInput._flatpickr.setDate(dateStr);
    } else {
        dateInput.value = dateStr;
    }
}