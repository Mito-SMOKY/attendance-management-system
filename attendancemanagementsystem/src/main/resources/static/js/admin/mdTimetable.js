/**
 * mdTimetable.js
 * 時間割管理画面の共通スクリプト
 * ・学科・学年の選択に応じて、教科リストを適切に絞り込む機能を搭載
 */

$(document).ready(function() {

    // --- 1. 学科・学年変更時の連動処理 (イベントデリゲート) ---
    
    // ① 学科が変わった時 -> 学年リストを更新し、(学年未定になるので)教科リストはクリア
    $(document).on('change', '#departmentId', function() {
        updateGradeOptions();   
        // 学科を変えた直後は学年が不整合になるため、教科リストは一旦空にするか、
        // updateGradeOptions完了後の自動選択に任せるのが安全です。
        // ここでは明示的に教科リストをリセット（クリア）します。
        clearSubjectOptions(); 
    });

    // ② 学年が変わった時 -> 教科リストを更新 (★ここが追加ポイント)
    $(document).on('change', '#targetGrade', function() {
        updateSubjectOptions();
    });

    // 初期表示時の実行
    if ($('#departmentId').length && $('#departmentId').val()) {
        updateGradeOptions();
        // 初期値ですでに学年も入っている場合は教科も読み込む
        if ($('#targetGrade').val()) {
            updateSubjectOptions();
        }
    }

    /**
     * 学年プルダウンの更新
     */
    function updateGradeOptions() {
        const $deptSelect = $('#departmentId');
        const $gradeSelect = $('#targetGrade');

        if (!$deptSelect.length || !$gradeSelect.length) return;

        const deptId = $deptSelect.val();
        const currentGrade = $gradeSelect.data('selected') || $gradeSelect.val();

        if (!deptId) {
            $gradeSelect.empty().append('<option value="">-</option>');
            $gradeSelect.trigger('change.select2');
            return;
        }

        $.ajax({
            url: '/admin/mdTimetable/api/getRegisteredGrades',
            type: 'GET',
            data: { departmentId: deptId },
            dataType: 'json',
            success: function(grades) {
                $gradeSelect.empty();
                if (!grades || grades.length === 0) {
                    $gradeSelect.append('<option value="">データなし</option>');
                } else {
                    $gradeSelect.append('<option value="">(学年を選択)</option>');

                    $.each(grades, function(index, grade) {
                        const option = $('<option>', {
                            value: grade,
                            text: grade + '年'
                        });
                        if (String(grade) === String(currentGrade)) {
                            option.prop('selected', true);
                        }
                        $gradeSelect.append(option);
                    });

                    // 未選択なら先頭を自動選択
                    if (!$gradeSelect.val() && grades.length > 0 && !currentGrade) {
                        $gradeSelect.val(grades[0]).trigger('change');
                    }
                }
                $gradeSelect.trigger('change');
                $gradeSelect.trigger('change.select2');
            },
            error: function(xhr) {
                console.error("学年データの取得に失敗:", xhr);
                $gradeSelect.empty().append('<option value="">取得エラー</option>');
            }
        });
    }

    /**
     * ★修正: 教科プルダウンの更新
     * 「学科」と「学年」の両方が決まっている時だけ検索する
     */
    function updateSubjectOptions() {
        const deptId = $('#departmentId').val();
        const targetGrade = $('#targetGrade').val(); // ★学年も取得

        const $subjectSelects = $('select[name$=".subjectId"]');
        if ($subjectSelects.length === 0) return;

        // 学科か学年のどちらかが欠けていたら、教科は選べないようにクリアして終了
        if (!deptId || !targetGrade) {
            clearSubjectOptions();
            return;
        }

        // サーバーから「その学科の、その学年の」教科リストを取得
        $.ajax({
            url: '/admin/mdTimetable/api/getSubjects',
            type: 'GET',
            data: { 
                departmentId: deptId,
                targetGrade: targetGrade // ★パラメータに追加
            },
            dataType: 'json',
            success: function(subjects) {
                $subjectSelects.each(function() {
                    const $select = $(this);
                    const currentVal = $select.val(); 

                    $select.empty();
                    $select.append('<option value="">(科目未定)</option>');

                    if (subjects && subjects.length > 0) {
                        $.each(subjects, function(index, subject) {
                            const option = $('<option>', {
                                value: subject.subjectId,
                                text: subject.subjectName
                            });
                            if (String(subject.subjectId) === String(currentVal)) {
                                option.prop('selected', true);
                            }
                            $select.append(option);
                        });
                    }
                    $select.trigger('change.select2');
                });
            },
            error: function(xhr) {
                console.error("教科リストの取得に失敗しました", xhr);
            }
        });
    }

    /**
     * 教科プルダウンをリセットするヘルパー関数
     */
    function clearSubjectOptions() {
        const $subjectSelects = $('select[name$=".subjectId"]');
        $subjectSelects.each(function() {
            const $select = $(this);
            $select.empty();
            $select.append('<option value="">(まずは学年を選択)</option>');
            $select.trigger('change.select2');
        });
    }


    // --- 2. Flatpickr (一括登録用・期間選択) ---
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    if (startDateInput && endDateInput) {
        const fpEnd = flatpickr("#endDate", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true 
        });

        flatpickr("#startDate", {
            locale: "ja",
            dateFormat: "Y-m-d",
            allowInput: true,
            onChange: function(selectedDates, dateStr, instance) {
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


// --- 以下、HTML内から呼ばれる関数群 ---
// (内容は変更なしのため、そのまま利用します)

function setAutoDates() {
    const year = document.getElementById('yearSelect').value;
    const term = document.getElementById('termSelect').value;
    const startElem = document.getElementById("startDate");
    const endElem = document.getElementById("endDate");

    if (!year || !term) return;

    let startStr = (term == '1') ? year + '-04-01' : year + '-10-01';
    let endStr = (term == '1') ? year + '-09-30' : (parseInt(year) + 1) + '-03-31';

    if (startElem && startElem._flatpickr) startElem._flatpickr.setDate(startStr);
    else if (startElem) startElem.value = startStr;

    if (endElem && endElem._flatpickr) endElem._flatpickr.setDate(endStr);
    else if (endElem) endElem.value = endStr;
}

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

async function uploadSchedulePdf() {
    const fileInput = document.getElementById('schedulePdf');
    const loadingMsg = document.getElementById('pdfLoadingMsg');
    const analyzeBtn = document.getElementById('analyzePdfBtn');
    
    const yearSelect = document.getElementById('yearSelect');
    const termSelect = document.getElementById('termSelect'); 
    const outputArea = document.getElementById('excludedDates');
    
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');

    const targetGradeSelect = document.getElementById('targetGrade');
    const targetGrade = targetGradeSelect ? targetGradeSelect.value : "";

    if (fileInput.files.length === 0) {
        alert("PDFファイルを選択してください");
        return;
    }

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
    formData.append("targetGrade", targetGrade);

    try {
        const response = await fetch('/admin/mdTimetable/analyzePdf', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error("Server Error: " + response.status);

        const result = await response.json(); 
        
        if (result.startDate && result.endDate) {
            if (startDateInput && startDateInput._flatpickr) startDateInput._flatpickr.setDate(result.startDate);
            else if (startDateInput) startDateInput.value = result.startDate;

            if (endDateInput && endDateInput._flatpickr) endDateInput._flatpickr.setDate(result.endDate);
            else if (endDateInput) endDateInput.value = result.endDate;
        }

        const dateList = result.holidays || [];
        if (dateList.length > 0) {
            outputArea.value = dateList.join(', ');
        }
        
        let msg = `【${targetGrade}年生】のスケジュール解析完了\n`;
        if (result.startDate && result.endDate) {
            msg += `■ 期間: ${result.startDate} ～ ${result.endDate}\n`;
        }
        msg += `■ 休日: ${dateList.length}日分を抽出しました`;
        alert(msg);

    } catch (e) {
        console.error(e);
        alert("解析に失敗しました。");
    } finally {
        loadingMsg.style.display = 'none';
        analyzeBtn.disabled = false;
    }
}

function applyTimetableData(data) {
    let count = 0;
    data.forEach(item => {
        const dayKey = item.day ? item.day.toUpperCase() : "";
        const subjectEl = $(`select[name="scheduleMap[${item.slot}]['${dayKey}'].subjectId"]`);
        const roomEl    = $(`select[name="scheduleMap[${item.slot}]['${dayKey}'].classroomId"]`);
        const teacherEl = $(`select[name="scheduleMap[${item.slot}]['${dayKey}'].userId"]`);

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

async function checkAndSubmit() {
    const deptId = document.getElementById('departmentId').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const targetGrade = document.getElementById('targetGrade').value;
    
    const form = document.getElementById('registerForm');

    if (!deptId || !targetGrade || !startDate || !endDate) {
        alert("クラス・学年・期間を正しく入力してください。");
        return;
    }

    try {
        const params = new URLSearchParams({
            departmentId: deptId,
            targetGrade: targetGrade,
            startDate: startDate,
            endDate: endDate
        });

        const response = await fetch(`/admin/mdTimetable/check-overlap?${params.toString()}`);
        
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