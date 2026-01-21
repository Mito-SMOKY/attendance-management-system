/**
 * mdTimetable.js
 * 時間割管理画面の共通スクリプト
 */

$(document).ready(function() {
    // --- 1. Select2の初期化 ---
    $('.searchable-select').select2({
        language: "ja",
        width: '100%',
        placeholder: "選択してください",
        allowClear: true
    });

    // --- 2. Flatpickr (一括登録用・期間選択) ---
    // startとendを別々に初期化して連動させる方式に変更（バグ回避のため）
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    if (startDateInput && endDateInput) {
        
        // 終了日の設定
        const fpEnd = flatpickr("#endDate", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true // 手入力も許可
        });

        // 開始日の設定
        const fpStart = flatpickr("#startDate", {
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
 * Flatpickrを使っている場合は .setDate() で値を更新する
 */
function setAutoDates() {
    const year = document.getElementById('yearSelect').value;
    const term = document.getElementById('termSelect').value;
    
    // Flatpickrのインスタンスを取得
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

    // Flatpickrが適用されている場合は専用メソッドでセット
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
 * AI画像解析のアップロード処理
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
    analyzeBtn.textContent = "解析中...";

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
        analyzeBtn.textContent = "画像を読み込んで反映";
    }
}

function applyTimetableData(data) {
    let count = 0;
    data.forEach(item => {
        // 例: scheduleMap[1]['MONDAY']
        const nameBase = `scheduleMap[${item.slot}]['${item.day}']`;
        
        // select要素を探す
        // 注意: HTML上のname属性は th:field によって生成されるため、
        // 実際のDOMでは name="scheduleMap[1]['MONDAY'].subjectId" のようになっている
        const subjectEl = $(`select[name="${nameBase}.subjectId"]`);
        const roomEl    = $(`select[name="${nameBase}.classroomId"]`);
        const teacherEl = $(`select[name="${nameBase}.userId"]`);

        if (subjectEl.length === 0) return; 

        if (item.subjectId) {
            subjectEl.val(item.subjectId).trigger('change'); // Select2更新用
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
 * 一括登録前の重複チェック
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
            // データがある場合の警告メッセージ
            const msg = "【⚠️ データ重複警告】\n\n" + 
                        "指定された期間内に、既に以下のデータが見つかりました。\n" +
                        "--------------------------------------------------\n" +
                        "■ 重複期間: " + data.minDate + " ～ " + data.maxDate + "\n" +
                        "■ データ数: " + data.count + " コマ\n" +
                        "--------------------------------------------------\n\n" +
                        "登録を実行すると、これらのデータは今回の内容で\n" +
                        "上書き（更新）されます。\n\n" +
                        "本当によろしいですか？";
            
            if (confirm(msg)) {
                form.submit();
            }
        } else {
            // データがない場合の確認
            if (confirm("時間割を一括登録します。よろしいですか？")) {
                form.submit();
            }
        }

    } catch (error) {
        console.error("Check Error:", error);
        // エラー時は安全策として普通の確認を出して進める
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

    // Flatpickrに日付をセット
    if (dateInput._flatpickr) {
        dateInput._flatpickr.setDate(dateStr);
    } else {
        dateInput.value = dateStr;
    }
}