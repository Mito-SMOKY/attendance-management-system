/**
 * mdTimetable.js
 * 時間割管理画面の共通スクリプト
 */

$(document).ready(function() {

    // --- 1. Select2と連動した学年リストの動的読み込み (参照・削除画面用) ---
    // ★修正: URL判定(isViewOrDeletePage)を削除し、要素の有無だけで判断するように変更
    const $deptSelect = $('#departmentId'); // 学科セレクトボックス
    const $gradeSelect = $('#targetGrade'); // 学年セレクトボックス

    // 画面上に「学科」と「学年」のセレクトボックスが両方ある場合のみ実行
    if ($deptSelect.length && $gradeSelect.length) {
        
        // ① 学科が変更された時の処理 (Select2対応のためjQueryを使用)
        $deptSelect.on('change', function() {
            updateGradeOptions();
        });

        // ② 画面読み込み時にも実行 (戻るボタンや再表示時、初期値がある場合用)
        if ($deptSelect.val()) {
            updateGradeOptions();
        }
    }

    function updateGradeOptions() {
        const deptId = $deptSelect.val();
        // 現在選択されている学年（あればHTMLのdata属性やvalueから取得）
        const currentGrade = $gradeSelect.data('selected') || $gradeSelect.val();

        // 学科が空ならリセットして終了
        if (!deptId) {
            $gradeSelect.empty().append('<option value="">-</option>');
            return;
        }

        // サーバーからデータ取得
        $.ajax({
            url: '/admin/mdTimetable/api/getRegisteredGrades',
            type: 'GET',
            data: { departmentId: deptId },
            dataType: 'json',
            success: function(grades) {
                // セレクトボックスをクリア
                $gradeSelect.empty();

                if (grades.length === 0) {
                    $gradeSelect.append('<option value="">データなし</option>');
                } else {
                    // データあり：optionを追加
                    
                    // 必要であれば「-」や「選択」を追加
                    // $gradeSelect.append('<option value="">-</option>');

                    $.each(grades, function(index, grade) {
                        const option = $('<option>', {
                            value: grade,
                            text: grade + '年'
                        });

                        // 保持していた値と同じなら選択状態にする
                        if (String(grade) === String(currentGrade)) {
                            option.prop('selected', true);
                        }
                        $gradeSelect.append(option);
                    });

                    // もし何も選択されておらず、かつデータがある場合は先頭を自動選択する
                    // (データロード後に即座に表示可能にするため)
                    if (!$gradeSelect.val() && grades.length > 0 && !currentGrade) {
                        $gradeSelect.val(grades[0]).trigger('change');
                    }
                }
            },
            error: function(xhr, status, error) {
                console.error("学年データの取得に失敗:", error);
                $gradeSelect.empty().append('<option value="">取得エラー</option>');
            }
        });
    }


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
 * 行事予定表PDF解析
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

    // 学年は「クラス」から推測せず、画面の「学年プルダウン」から直接取得する
    const targetGradeSelect = document.getElementById('targetGrade');
    const targetGrade = targetGradeSelect ? targetGradeSelect.value : "";

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
    
    // ここで選択した学年をパラメータとして渡す
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