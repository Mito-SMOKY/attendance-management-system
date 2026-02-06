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
//以下要確認

      function toggleEditMode() {
        const container = document.querySelector(".subject-info-container");
        container.classList.toggle("is-editing");

        const btn = document.querySelector(".edit-btn");
        if (container.classList.contains("is-editing")) {
          btn.textContent = "編集終了(データは保存されません)";
          btn.classList.add("active");

          const successMsg = document.querySelector(".alert-success");
          const errorMsg = document.querySelector(".alert-error");
          if (successMsg) successMsg.style.display = "none";
          if (errorMsg) errorMsg.style.display = "none";
        } else {
          btn.textContent = "編集";
          btn.classList.remove("active");
        }
      }

      function addTeacherField(btn) {
        const cell = btn.closest(".teacher-edit-area");
        const row = btn.closest("tr");

        const wrapper = document.createElement("div");
        wrapper.className = "teacher-select-wrapper";

        const select = document.createElement("select");
        select.name = "teacherId";
        const defaultOpt = document.createElement("option");
        defaultOpt.value = "";
        defaultOpt.text = "-- 未定 --";
        select.appendChild(defaultOpt);

        teacherOptions.forEach((t) => {
          const opt = document.createElement("option");
          opt.value = t.teacherId;
          opt.text = t.teacherName;
          select.appendChild(opt);
        });
        wrapper.appendChild(select);

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "remove-teacher-btn";
        removeBtn.innerText = "×";
        removeBtn.onclick = function () {
          removeTeacher(this);
        };
        wrapper.appendChild(removeBtn);

        let majorId,
          deptId,
          grade,
          subjectName,
          subjectId = "";

        if (row.classList.contains("new-row")) {
          majorId = row.querySelector(".row-major-select").value;
          deptId = row.querySelector(".row-dept-select").value;
          grade = row.querySelector(".row-grade-select").value;
          subjectName = row.querySelector(".row-subject-name").value;
        } else {
          majorId = row.querySelector(".row-major-id").value;
          deptId = row.querySelector(".row-dept-id").value;
          grade = row.querySelector(".row-grade").value;
          subjectName = row.querySelector(".row-subject-name").value;

          const baseIdInput = cell.querySelector(
            'input[name="row-base-subject-id"]',
          );
          if (baseIdInput) subjectId = baseIdInput.value;
        }

        wrapper.appendChild(createHidden("majorId", majorId));
        wrapper.appendChild(createHidden("departmentId", deptId));
        wrapper.appendChild(createHidden("grade", grade));
        wrapper.appendChild(createHidden("subjectName", subjectName));
        wrapper.appendChild(createHidden("subjectId", subjectId));

        cell.insertBefore(wrapper, btn);
      }

      function removeTeacher(btn) {
        const wrapper = btn.closest(".teacher-select-wrapper");
        const area = wrapper.parentElement;
        if (area.querySelectorAll(".teacher-select-wrapper").length <= 1) {
          wrapper.querySelector("select").value = "";
        } else {
          wrapper.remove();
        }
      }

      function deleteRow(btn) {
        if (confirm("この教科情報を削除しますか？")) {
          btn.closest("tr").remove();
        }
      }

      function addRow() {
        const tbody = document.querySelector("#infoTable tbody");
        const tr = document.createElement("tr");
        tr.className = "new-row";

        // 1. コース
        const tdMajor = document.createElement("td");
        const selMajor = createSelect(majorOptions, "majorId", "majorName");
        selMajor.className = "row-major-select";
        selMajor.name = "dummy_major";
        selMajor.onchange = function () {
          syncHiddenValues(tr, "majorId", this.value);
        };
        tdMajor.appendChild(selMajor);
        tr.appendChild(tdMajor);

        // 2. クラス
        const tdDept = document.createElement("td");
        const selDept = createSelect(
          departmentOptions,
          "departmentId",
          "className",
        );
        selDept.className = "row-dept-select";
        selDept.name = "dummy_dept";
        selDept.onchange = function () {
          syncHiddenValues(tr, "departmentId", this.value);
        };
        tdDept.appendChild(selDept);
        tr.appendChild(tdDept);

        // 3. 学年 (修正箇所)
        const tdGrade = document.createElement("td");
        const selGrade = document.createElement("select");
        selGrade.className = "row-grade-select";
        selGrade.name = "dummy_grade";
        const defOpt = document.createElement("option");

        // ★修正: value="" を明示的にセット
        defOpt.value = "";
        defOpt.text = "選択";

        selGrade.appendChild(defOpt);
        gradeOptions.forEach((g) => {
          const opt = document.createElement("option");
          opt.value = g;
          opt.text = g + "年";
          selGrade.appendChild(opt);
        });
        selGrade.onchange = function () {
          syncHiddenValues(tr, "grade", this.value);
        };
        tdGrade.appendChild(selGrade);
        tr.appendChild(tdGrade);

        // 4. 教科名
        const tdName = document.createElement("td");
        const inpName = document.createElement("input");
        inpName.type = "text";
        inpName.className = "row-subject-name";
        inpName.style.width = "100%";
        inpName.oninput = function () {
          syncHiddenValues(tr, "subjectName", this.value);
        };
        tdName.appendChild(inpName);
        tr.appendChild(tdName);

        // 5. 担当教師
        const tdTeacher = document.createElement("td");
        const editArea = document.createElement("div");
        editArea.className = "teacher-edit-area edit-mode";

        const addBtn = document.createElement("button");
        addBtn.type = "button";
        addBtn.className = "add-teacher-inner-btn";
        addBtn.innerText = "＋ 担当追加";
        addBtn.onclick = function () {
          addTeacherField(this);
        };

        editArea.appendChild(addBtn);
        tdTeacher.appendChild(editArea);
        tr.appendChild(tdTeacher);

        addTeacherField(addBtn);

        // 6. 削除ボタン
        const tdDel = document.createElement("td");
        const delBtn = document.createElement("button");
        delBtn.type = "button";
        delBtn.className = "delete-btn";
        delBtn.innerHTML = '<i class="fa-solid fa-trash-can"></i>';
        delBtn.onclick = function () {
          deleteRow(this);
        };
        tdDel.appendChild(delBtn);
        tr.appendChild(tdDel);

        tbody.appendChild(tr);
      }

      function createHidden(name, val) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = val === undefined || val === null ? "" : val;
        return input;
      }

      function createSelect(options, valKey, textKey) {
        const sel = document.createElement("select");
        const def = document.createElement("option");
        def.value = "";
        def.text = "選択";
        sel.appendChild(def);
        options.forEach((opt) => {
          const o = document.createElement("option");
          o.value = opt[valKey];
          o.text = opt[textKey];
          sel.appendChild(o);
        });
        return sel;
      }

      function syncHiddenValues(row, name, value) {
        const hiddens = row.querySelectorAll(`input[name="${name}"]`);
        hiddens.forEach((h) => (h.value = value));
      }

      document.addEventListener("input", function (e) {
        if (
          e.target.classList.contains("row-subject-name") &&
          !e.target.closest("tr").classList.contains("new-row")
        ) {
          const val = e.target.value;
          const row = e.target.closest("tr");
          row
            .querySelectorAll('input[name="subjectName"]')
            .forEach((h) => (h.value = val));
        }
      });