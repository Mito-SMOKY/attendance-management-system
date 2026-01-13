/**
 * 編集モード切り替え & 保存処理
 */
function toggleEditMode() {
    const body = document.body;
    const btn = document.querySelector(".edit-btn");
    const errorText = document.getElementById("errorMsgDuplicate");
    const form = document.getElementById("masterForm");

    if (body.classList.contains("editing-mode")) {
        // --- 適用（保存）ボタン押下時 ---

        // 1. エラー表示をクリア
        document.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));

        // 2. 必須入力チェック
        if (!validateRequired()) {
            alert("未入力の項目があります。\nすべての項目を選択・入力してください。");
            return; // 処理を中断
        }

        // 3. 重複チェック
        if (!validateDuplicates()) {
            errorText.style.display = "inline-block";
            return;
        }

        if (!confirm("変更を保存しますか？")) {
            return;
        }
        form.submit();

    } else {
        // --- 編集ボタン押下時 ---
        body.classList.add("editing-mode");
        btn.textContent = "適用";
        errorText.style.display = "none";
    }
}

/**
 * 必須入力チェック
 * @returns {boolean} 全て入力されていれば true
 */
function validateRequired() {
    const rows = document.querySelectorAll("#infoTable tbody tr");
    let isValid = true;

    rows.forEach((row) => {
        // 各入力項目の取得
        const major = row.querySelector('select[name="majorId"]');
        const dept = row.querySelector('select[name="departmentId"]');
        const grade = row.querySelector('select[name="grade"]');
        const subjectName = row.querySelector('input[name="subjectName"]');
        const teacher = row.querySelector('select[name="teacherId"]');
        const count = row.querySelector('input[name="courseCount"]');

        // 要素が見つからない場合はスキップ（削除済み行など）
        if (!major || !dept || !grade || !subjectName || !teacher || !count) return;

        // 値のチェック
        if (major.value === "") {
            major.classList.add("input-error");
            isValid = false;
        }
        if (dept.value === "") {
            dept.classList.add("input-error");
            isValid = false;
        }
        if (grade.value === "") {
            grade.classList.add("input-error");
            isValid = false;
        }
        if (subjectName.value.trim() === "") {
            subjectName.classList.add("input-error");
            isValid = false;
        }
        if (teacher.value === "") {
            teacher.classList.add("input-error");
            isValid = false;
        }
        if (count.value === "" || count.value <= 0) {
            count.classList.add("input-error");
            isValid = false;
        }
    });

    return isValid;
}

/**
 * 行追加処理
 * HTML内で定義された xxxOptions 変数を使用します
 */
function addRow() {
    const tableBody = document.querySelector("#infoTable tbody");
    const newRow = document.createElement("tr");

    // 教師プルダウン生成
    let teacherOpts = '<option value="">教師を選択</option>';
    if (typeof teacherOptions !== 'undefined') {
        teacherOptions.forEach((teacher) => {
            teacherOpts += `<option value="${teacher.teacherId}">${teacher.teacherName}</option>`;
        });
    }

    // コースプルダウン生成
    let majorOpts = '<option value="">コース選択</option>';
    if (typeof majorOptions !== 'undefined') {
        majorOptions.forEach((m) => {
            majorOpts += `<option value="${m.majorId}">${m.majorName}</option>`;
        });
    }

    // クラスプルダウン生成
    let deptOpts = '<option value="">クラス選択</option>';
    if (typeof departmentOptions !== 'undefined') {
        departmentOptions.forEach((d) => {
            deptOpts += `<option value="${d.departmentId}">${d.className}</option>`;
        });
    }

    // 学年プルダウン生成
    let gradeOpts = '<option value="">学年選択</option>';
    if (typeof gradeOptions !== 'undefined') {
        gradeOptions.forEach((g) => {
            gradeOpts += `<option value="${g}">${g}年</option>`;
        });
    }

    // 行のHTMLを構築
    newRow.innerHTML = `
        <td>
            <select class="edit-mode" name="majorId">${majorOpts}</select>
        </td>
        <td>
            <select class="edit-mode" name="departmentId">${deptOpts}</select>
        </td>
        <td>
            <select class="edit-mode" name="grade">${gradeOpts}</select>
        </td>
        <td>
            <input type="text" class="edit-mode" name="subjectName" placeholder="教科名 (手動入力)">
            <button type="button" class="delete-btn" onclick="deleteRow(this)">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </td>
        <td>
            <select class="edit-mode" name="teacherId">${teacherOpts}</select>
        </td>
        <td>
            <input type="number" class="edit-mode" name="courseCount" value="1" min="1" max="10">
        </td>
    `;

    tableBody.appendChild(newRow);

    // 追加した行が見えるように、編集モードでなければ切り替える
    if (!document.body.classList.contains("editing-mode")) {
        toggleEditMode();
    }
}

/**
 * 行削除処理
 */
function deleteRow(btn) {
    if (confirm("この行を削除しますか？")) {
        const row = btn.closest("tr");
        row.remove();
    }
}

/**
 * 重複チェック (教科名 + 教師)
 * @returns {boolean} 重複がなければ true
 */
function validateDuplicates() {
    const rows = document.querySelectorAll("#infoTable tbody tr");
    const pairs = new Map();
    let hasError = false;

    // まずエラー表示をリセット（input-errorのみ。validateRequiredでついたエラーも消える点に注意）
    document.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));

    rows.forEach((row, index) => {
        const nameInput = row.querySelector('input[name="subjectName"]');
        const teacherSelect = row.querySelector('select[name="teacherId"]');
        
        // 要素が取得できなければスキップ
        if (!nameInput || !teacherSelect) return;

        const nameVal = nameInput.value.trim();
        const teacherVal = teacherSelect.value;

        // 両方値が入っている場合のみチェック
        if (nameVal && teacherVal) {
            const key = nameVal + "::" + teacherVal;
            
            if (pairs.has(key)) {
                hasError = true;
                // 今回の行をエラーに
                nameInput.classList.add("input-error");
                teacherSelect.classList.add("input-error");
                
                // 既出の行（重複元）もエラーにする
                const originalIndex = pairs.get(key);
                const originalRow = rows[originalIndex];
                if (originalRow) {
                    originalRow.querySelector('input[name="subjectName"]').classList.add("input-error");
                    originalRow.querySelector('select[name="teacherId"]').classList.add("input-error");
                }
            } else {
                pairs.set(key, index);
            }
        }
    });

    return !hasError;
}