/**
 * 編集モードを開始する
 * (上部の「編集」ボタンから呼ばれる)
 */
function enableEditMode() {
  document.body.classList.add("editing-mode");
  
  // エラー表示リセット
  const errorTextDup = document.getElementById("errorMsgDuplicate");
  const errorTextSec = document.getElementById("errorMsgSecurity");
  
  document.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));
  if (errorTextDup) errorTextDup.style.display = "none";
  if (errorTextSec) errorTextSec.style.display = "none";
}

/**
 * 編集キャンセル
 * (下部の「キャンセル」ボタンから呼ばれる)
 */
function cancelEditMode() {
  if(confirm("編集を破棄して元に戻りますか？")) {
    window.location.reload();
  }
}

/**
 * 保存処理を実行する
 * (下部の「保存実行」ボタンから呼ばれる)
 */
function saveData() {
  const form = document.getElementById("masterForm");
  const errorTextDup = document.getElementById("errorMsgDuplicate");
  const errorTextSec = document.getElementById("errorMsgSecurity");

  // リセット
  document.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));
  if (errorTextDup) errorTextDup.style.display = "none";
  if (errorTextSec) errorTextSec.style.display = "none";

  // 1. 必須入力チェック
  if (!validateRequired()) {
    alert("未入力の項目があります。\nすべての項目を選択・入力してください。");
    return;
  }

  // 2. セキュリティチェック
  if (!validateSecurity()) {
    if (errorTextSec) errorTextSec.style.display = "inline-block";
    alert("入力内容に使用できない文字が含まれています。\n（< > & \" ' / \\ .. など）");
    return;
  }

  // 3. 重複チェック
  if (!validateDuplicates()) {
    if (errorTextDup) errorTextDup.style.display = "inline-block";
    alert("同じ教科と教師の組み合わせが既に存在します。");
    return;
  }

  // 4. 送信
  if (confirm("変更を保存しますか？")) {
    form.submit();
  }
}

/**
 * セキュリティチェック
 */
function validateSecurity() {
  const rows = document.querySelectorAll("#infoTable tbody tr");
  let isValid = true;
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
        <span class="view-mode" style="display:none"></span>
        <select class="edit-mode" name="majorId" style="display:block">${majorOpts}</select>
    </td>
    <td>
        <span class="view-mode" style="display:none"></span>
        <select class="edit-mode" name="departmentId" style="display:block">${deptOpts}</select>
    </td>
    <td>
        <span class="view-mode" style="display:none"></span>
        <select class="edit-mode" name="grade" style="display:block">${gradeOpts}</select>
    </td>
    <td>
        <span class="view-mode" style="display:none"></span>
        <div class="edit-mode-wrapper edit-mode" style="display:flex">
            <input type="text" name="subjectName" placeholder="教科名">
            <button type="button" class="delete-btn" onclick="deleteRow(this)">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </div>
    </td>
    <td>
        <span class="view-mode" style="display:none"></span>
        <select class="edit-mode" name="teacherId" style="display:block">${teacherOpts}</select>
    </td>
    <td>
        <span class="view-mode" style="display:none"></span>
        <input type="number" class="edit-mode" name="courseCount" value="1" min="1" max="10" style="display:block">
    </td>
  `;

  tableBody.appendChild(newRow);
  
  // 追加した行までスクロール
  newRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
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