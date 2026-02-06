function toggleEditMode() {
  const container = document.querySelector(".subject-info-container");
  const btn = document.querySelector(".edit-btn");

  if (container.classList.contains("is-editing")) {
    if (confirm("編集中の内容は破棄されます。よろしいですか？")) {
      // ページをリロードして、変更前の状態（DBの値）に戻す
      window.location.reload();
    }
    // キャンセルの場合は何もしない（編集モードのまま）
    return;
  }

  container.classList.add("is-editing");
  
  btn.textContent = "保存せずに終了";
  btn.classList.add("active");

  const successMsg = document.querySelector(".alert-success");
  const errorMsg = document.querySelector(".alert-error");
  if (successMsg) successMsg.style.display = "none";
  if (errorMsg) errorMsg.style.display = "none";
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