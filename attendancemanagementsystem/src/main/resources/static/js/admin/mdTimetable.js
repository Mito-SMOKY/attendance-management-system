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

/**
 * ★修正: 行事予定表PDF解析
 * (期間の自動セット ＆ 休日の抽出)
 */
async function uploadSchedulePdf() {
    const fileInput = document.getElementById('schedulePdf');
    const loadingMsg = document.getElementById('pdfLoadingMsg');
    const analyzeBtn = document.getElementById('analyzePdfBtn');
    
    const yearSelect = document.getElementById('yearSelect');
    const termSelect = document.getElementById('termSelect'); 
    const outputArea = document.getElementById('excludedDates');
    
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');

    // ★修正: 学年は「クラス」から推測せず、画面の「学年プルダウン」から直接取得する
    const targetGradeSelect = document.getElementById('targetGrade');
    const targetGrade = targetGradeSelect ? targetGradeSelect.value : null;

    if (fileInput.files.length === 0) {
        alert("PDFファイルを選択してください");
        return;
    }

    // 学年未選択のチェック
    if (!targetGrade) {
        alert("学年を選択してください。");
        return;
    }

    loadingMsg.style.display = 'block';
    analyzeBtn.disabled = true;

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);
    formData.append("year", yearSelect.value); 
    formData.append("term", termSelect.value); 
    
    // ★ここで選択した学年をパラメータとして渡す
    formData.append("targetGrade", targetGrade);

    try {
        const response = await fetch('/admin/mdTimetable/analyzePdf', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error("Server Error: " + response.status);

        const result = await response.json(); 
        console.log("解析結果:", result);

        if (result.startDate && result.endDate) {
            if (startDateInput && startDateInput._flatpickr) startDateInput._flatpickr.setDate(result.startDate);
            else if (startDateInput) startDateInput.value = result.startDate;

            if (endDateInput && endDateInput._flatpickr) endDateInput._flatpickr.setDate(result.endDate);
            else if (endDateInput) endDateInput.value = result.endDate;
        }

        const dateList = result.holidays || [];

        if (dateList.length === 0 && (!result.startDate || !result.endDate)) {
            alert("PDFから有効な情報を読み取れませんでした。");
        } else {
            if (dateList.length > 0) {
                outputArea.value = dateList.join(', ');
            }
            
            // メッセージも選択した学年を表示
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

/**
 * ★追加: 学科選択時に、その学科で「登録済みの学年」だけをプルダウンにセットする
 * (参照画面・削除画面用)
 */
function updateRegisteredGradesForButtons() {
    const deptSelect = document.getElementById('departmentId') || document.querySelector('select[name="departmentId"]');
    // 学年の隠しフィールド
    const gradeInput = document.getElementById('targetGrade') || document.querySelector('input[name="targetGrade"]');
    
    if (!deptSelect || !gradeInput) return;
    
    // 学年ボタンのグループを取得
    const group = gradeInput.closest('.grade-toggle-group');
    if (!group) return; // ボタン形式じゃない場合は何もしない

    const deptId = deptSelect.value;
    const buttons = group.querySelectorAll('.grade-btn');

    // 学科未選択なら全ボタン無効化して終了
    if (!deptId) {
        buttons.forEach(btn => {
            btn.disabled = true;
            btn.classList.remove('active');
        });
        gradeInput.value = "";
        return;
    }

    // APIコール
    fetch(`/admin/mdTimetable/api/getRegisteredGrades?departmentId=${deptId}`)
        .then(response => response.json())
        .then(grades => {
            let isCurrentValueValid = false;
            const currentVal = parseInt(gradeInput.value);

            buttons.forEach(btn => {
                const btnVal = parseInt(btn.getAttribute('data-value'));
                
                // 取得したリストに含まれているかチェック
                if (grades.includes(btnVal)) {
                    // データあり -> 有効化
                    btn.disabled = false;
                    btn.title = ""; // ツールチップ解除
                    
                    if (btnVal === currentVal) isCurrentValueValid = true;
                } else {
                    // データなし -> 無効化
                    btn.disabled = true;
                    btn.classList.remove('active');
                    btn.title = "データがありません";
                }
            });

            // もし選択中の学年が無効になった（または未選択の）場合、
            // 有効な学年のうち一番小さいものを自動選択する
            if (!isCurrentValueValid) {
                gradeInput.value = ""; // 一旦クリア
                
                // 有効なボタンの先頭をクリック状態にする
                const firstValidBtn = group.querySelector('.grade-btn:not(:disabled)');
                if (firstValidBtn) {
                    firstValidBtn.click(); // クリックイベントを発火させて値をセット
                }
            }
        })
        .catch(error => {
            console.error('学年データの取得に失敗:', error);
        });
}
// 画面読み込み時にイベントリスナーを設定
document.addEventListener('DOMContentLoaded', function() {
    const deptSelect = document.getElementById('departmentId') || document.querySelector('select[name="departmentId"]');
    
    if (deptSelect) {
        // 学科プルダウンがある場合のみ動作
        
        // 今の画面が「参照」か「削除」かを判定する簡易ロジック
        // (URLに 'view' か 'delete' が含まれている、または特定のクラスがある等)
        const isViewOrDeletePage = location.pathname.includes('/view') || location.pathname.includes('/delete');

        if (isViewOrDeletePage) {
            // 変更時に発火
            deptSelect.addEventListener('change', updateRegisteredGrades);
            
            // 初期表示時にも実行 (再読み込み時など値を復元するため)
            // ただし、HTML側でth:selectedされている値を優先するため、少し遅延させるか、
            // 現在の値を保持してから実行する
            const gradeSelect = document.getElementById('targetGrade') || document.querySelector('select[name="targetGrade"]');
            if (gradeSelect && deptSelect.value) {
                // 現在の値を属性に保存しておく
                gradeSelect.setAttribute('data-selected-grade', gradeSelect.value);
                updateRegisteredGrades();
            }
        }
    }
});

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