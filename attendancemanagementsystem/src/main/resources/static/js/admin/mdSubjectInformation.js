/**
 * 編集モード切り替え
 */
function toggleEditMode() {
  const body = document.body;
  const btn = document.querySelector(".edit-btn");
  const errorTextDup = document.getElementById("errorMsgDuplicate");
  const errorTextSec = document.getElementById("errorMsgSecurity");
  const form = document.getElementById("masterForm");

  if (body.classList.contains("editing-mode")) {
    // --- 適用（保存）ボタン押下時 ---

    // 1. エラー表示リセット
    document
      .querySelectorAll(".input-error")
      .forEach((el) => el.classList.remove("input-error"));
    if (errorTextDup) errorTextDup.style.display = "none";
    if (errorTextSec) errorTextSec.style.display = "none";

    // 2. 必須入力チェック
    if (!validateRequired()) {
      alert("未入力の項目があります。\nすべての項目を選択・入力してください。");
      return;
    }

    // 3. ★追加: セキュリティチェック（インジェクション対策）
    if (!validateSecurity()) {
      if (errorTextSec) errorTextSec.style.display = "inline-block";
      alert("入力内容に使用できない文字が含まれています。\n（< > & \" ' / \\ .. など）");
      return;
    }

    // 4. 重複チェック
    if (!validateDuplicates()) {
      if (errorTextDup) errorTextDup.style.display = "inline-block";
      alert("同じ教科と教師の組み合わせが既に存在します。");
      return;
    }

    // 5. 送信確認
    if (!confirm("変更を保存しますか？")) {
      return;
    }
    form.submit();

  } else {
    // --- 編集ボタン押下時 ---
    body.classList.add("editing-mode");
    btn.textContent = "適用";
    if (errorTextDup) errorTextDup.style.display = "none";
    if (errorTextSec) errorTextSec.style.display = "none";
  }
}

/**
 * ★追加: セキュリティチェック
 * インジェクションやディレクトリトラバーサルに使われる記号を禁止する
 */
function validateSecurity() {
  const rows = document.querySelectorAll("#infoTable tbody tr");
  let isValid = true;
  
  // 禁止文字の正規表現
  // < > & " ' / \ ..
  const forbiddenPattern = /[<>&"'\/]|\\|\.\./;

  rows.forEach((row) => {
    const subjectName = row.querySelector('input[name="subjectName"]');
    if (!subjectName) return;

    const val = subjectName.value;
    if (forbiddenPattern.test(val)) {
      subjectName.classList.add("input-error");
      isValid = false;
    }
  });

  return isValid;
}

/**
 * 必須入力チェック
 */
function validateRequired() {
  const rows = document.querySelectorAll("#infoTable tbody tr");
  let isValid = true;

  rows.forEach((row) => {
    const major = row.querySelector('select[name="majorId"]');
    const dept = row.querySelector('select[name="departmentId"]');
    const grade = row.querySelector('select[name="grade"]');
    const subjectName = row.querySelector('input[name="subjectName"]');
    const teacher = row.querySelector('select[name="teacherId"]');
    const count = row.querySelector('input[name="courseCount"]');

    if (!major || !dept || !grade || !subjectName || !teacher || !count) return;

    const checkEmpty = (el) => {
      if (el.value === "" || el.value.trim() === "") {
        el.classList.add("input-error");
        isValid = false;
      }
    };

    checkEmpty(major);
    checkEmpty(dept);
    checkEmpty(grade);
    checkEmpty(subjectName);
    checkEmpty(teacher);
    
    if (count.value === "" || parseInt(count.value) <= 0) {
      count.classList.add("input-error");
      isValid = false;
    }
  });

  return isValid;
}

/**
 * 重複チェック
 */
function validateDuplicates() {
  const rows = document.querySelectorAll("#infoTable tbody tr");
  const pairs = new Map();
  let hasError = false;

  rows.forEach((row, index) => {
    const nameInput = row.querySelector('input[name="subjectName"]');
    const teacherSelect = row.querySelector('select[name="teacherId"]');

    if (!nameInput || !teacherSelect) return;

    const nameVal = nameInput.value.trim();
    const teacherVal = teacherSelect.value;

    if (nameVal && teacherVal) {
      const key = nameVal + "::" + teacherVal;

      if (pairs.has(key)) {
        hasError = true;
        nameInput.classList.add("input-error");
        teacherSelect.classList.add("input-error");
        
        const prevIndex = pairs.get(key);
        const prevRow = rows[prevIndex];
        if (prevRow) {
            prevRow.querySelector('input[name="subjectName"]').classList.add("input-error");
            prevRow.querySelector('select[name="teacherId"]').classList.add("input-error");
        }
      } else {
        pairs.set(key, index);
      }
    }
  });
  return !hasError;
}

/**
 * 行追加
 */
function addRow() {
  const tableBody = document.querySelector("#infoTable tbody");
  const newRow = document.createElement("tr");

  const majorOpts = document.getElementById("tpl-major").innerHTML;
  const deptOpts = document.getElementById("tpl-dept").innerHTML;
  const gradeOpts = document.getElementById("tpl-grade").innerHTML;
  const teacherOpts = document.getElementById("tpl-teacher").innerHTML;

  newRow.innerHTML = `
    <input type="hidden" name="subjectId" value="">
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
        <div class="edit-mode-wrapper">
            <input type="text" class="edit-mode" name="subjectName" placeholder="教科名">
            <button type="button" class="delete-btn edit-mode" onclick="deleteRow(this)">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </div>
    </td>
    <td>
        <select class="edit-mode" name="teacherId">${teacherOpts}</select>
    </td>
    <td>
        <input type="number" class="edit-mode" name="courseCount" value="1" min="1" max="10">
    </td>
  `;

  tableBody.appendChild(newRow);

  if (!document.body.classList.contains("editing-mode")) {
     document.body.classList.add("editing-mode");
     const btn = document.querySelector(".edit-btn");
     if(btn) btn.textContent = "適用";
  }
}

/**
 * 行削除
 */
function deleteRow(btn) {
  if (confirm("この行を削除しますか？")) {
    const row = btn.closest("tr");
    row.remove();
  }
}