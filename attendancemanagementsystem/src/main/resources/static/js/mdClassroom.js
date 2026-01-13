/**
 * 編集モードの切り替えおよび保存処理
 */
function toggleEditMode() {
    const body = document.body;
    const btn = document.querySelector(".edit-btn");
    const form = document.getElementById("masterForm");
    const errorMsgReq = document.getElementById("errorMsgRequired");
    const errorMsgDup = document.getElementById("errorMsgDuplicate");

    // 編集モード関連の要素
    const viewModes = document.querySelectorAll(".view-mode");
    const editModes = document.querySelectorAll(".edit-mode");
    const deleteBtns = document.querySelectorAll(".delete-btn");
    const addBtnWrapper = document.querySelector(".add-btn-wrapper");

    // --- 適用（保存）ボタン押下時 ---
    if (body.classList.contains("editing-mode")) {
        // バリデーション
        if (!validateData()) {
            return;
        }

        if (!confirm("変更を保存しますか？")) {
            return;
        }

        // フォーム送信
        form.submit();
        return;
    }

    // --- 編集ボタン押下時 ---
    body.classList.add("editing-mode");
    btn.textContent = "適用";

    // 表示切り替え
    viewModes.forEach((el) => (el.style.display = "none"));
    editModes.forEach((el) => (el.style.display = "block"));
    deleteBtns.forEach((el) => (el.style.display = "inline-block"));
    addBtnWrapper.style.display = "block";

    // エラーメッセージ初期化
    errorMsgReq.style.display = "none";
    errorMsgDup.style.display = "none";
}

/**
 * 行追加処理
 */
function addClassroomRow() {
    const tableBody = document.querySelector("#classroomTable tbody");
    const newRow = document.createElement("tr");

    newRow.innerHTML = `
        <td>
            <input type="text" class="form-input" name="classroomName" placeholder="教室名 (例: 201)">
        </td>
        <td>
            <input type="text" class="form-input" name="macAddress" placeholder="AA:BB:CC:11:22:33">
        </td>
        <td>
            <button type="button" class="delete-btn" onclick="deleteRow(this)">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </td>
    `;

    tableBody.appendChild(newRow);
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
 * バリデーション処理
 * - 必須チェック
 * - 重複チェック
 */
function validateData() {
    const rows = document.querySelectorAll("#classroomTable tbody tr");
    const names = new Map();
    
    let hasEmptyError = false;
    let hasDuplicateError = false;

    // エラー表示リセット
    document.getElementById("errorMsgRequired").style.display = "none";
    document.getElementById("errorMsgDuplicate").style.display = "none";
    document.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));

    rows.forEach((row, index) => {
        const nameInput = row.querySelector('input[name="classroomName"]');
        if (!nameInput) return; // 削除済みなどで要素がない場合はスキップ

        const valName = nameInput.value.trim();

        // 必須チェック
        if (valName === "") {
            hasEmptyError = true;
            nameInput.classList.add("input-error");
            return; // 重複チェックは不要なので次の行へ
        }

        // 重複チェック
        if (names.has(valName)) {
            hasDuplicateError = true;
            nameInput.classList.add("input-error");

            // 先に見つかった重複元の行も赤くする
            const prevRowIndex = names.get(valName);
            const prevRow = rows[prevRowIndex];
            const prevInput = prevRow.querySelector('input[name="classroomName"]');
            
            if (prevInput) {
                prevInput.classList.add("input-error");
            }
        } else {
            // 重複チェック用に名前とインデックスを記録
            names.set(valName, index);
        }
    });

    if (hasEmptyError) {
        document.getElementById("errorMsgRequired").style.display = "inline-block";
        return false;
    }
    if (hasDuplicateError) {
        document.getElementById("errorMsgDuplicate").style.display = "inline-block";
        return false;
    }

    return true;
}