/**
 * 編集モード切り替え
 */
function toggleEditMode() {
    const form = document.getElementById("masterForm");
    const btn = document.getElementById("toggleButton");
    
    // 現在のクラスでモード判定
    if (form.classList.contains("is-editing")) {
        // --- 保存処理（編集モード -> 保存） ---
        
        // バリデーション実行
        if (!validateData()) return;

        if (confirm("変更を保存しますか？")) {
            form.submit();
        }
    } else {
        // --- 編集開始（表示モード -> 編集モード） ---
        form.classList.add("is-editing");
        
        // ボタンを「編集」から「保存」へ変更
        btn.textContent = "保存";
        btn.classList.remove("edit-btn");
        btn.classList.add("save-btn");
        
        // エラーメッセージは一旦消す
        resetErrors();
    }
}

/**
 * ★追加: 編集キャンセル
 */
function cancelEditMode() {
    // 変更を破棄して確実に戻すため、リロードが最も安全でシンプル
    if(confirm("編集を破棄して元に戻りますか？")) {
        window.location.reload();
    }
}

/**
 * 行追加
 */
function addClassroomRow() {
    const tbody = document.querySelector("#classroomTable tbody");
    const newRow = document.createElement("tr");
    
    newRow.innerHTML = `
        <input type="hidden" name="classroomId" value="">
        <td>
            <span class="view-mode" style="display:none"></span>
            <input type="text" class="form-input edit-mode" name="classroomName" placeholder="教室名 (例: 201)" style="display:block">
        </td>
        <td>
            <span class="view-mode" style="display:none"></span>
            <input type="text" class="form-input edit-mode" name="macAddress" placeholder="AA:BB:CC:11:22:33" style="display:block">
        </td>
        <td class="action-col">
            <button type="button" class="delete-btn edit-mode" onclick="deleteRow(this)" style="display:inline-block">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </td>
    `;
    
    tbody.appendChild(newRow);
    const input = newRow.querySelector('input[name="classroomName"]');
    if(input) input.focus();
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

/**
 * エラーリセット
 */
function resetErrors() {
    document.getElementById("errorMsgRequired").style.display = "none";
    document.getElementById("errorMsgDuplicate").style.display = "none";
    document.querySelectorAll(".input-error").forEach(el => el.classList.remove("input-error"));
}

/**
 * バリデーション
 */
function validateData() {
    const rows = document.querySelectorAll("#classroomTable tbody tr");
    const names = new Map();
    let hasEmptyError = false;
    let hasDuplicateError = false;
    
    resetErrors();

    rows.forEach((row, index) => {
        const nameInput = row.querySelector('input[name="classroomName"]');
        if (!nameInput) return;

        const valName = nameInput.value.trim();

        // 空チェック
        if (valName === "") {
            hasEmptyError = true;
            nameInput.classList.add("input-error");
        } else {
            // 重複チェック
            if (names.has(valName)) {
                hasDuplicateError = true;
                nameInput.classList.add("input-error");
                
                const prevIndex = names.get(valName);
                const prevInput = rows[prevIndex]?.querySelector('input[name="classroomName"]');
                if (prevInput) prevInput.classList.add("input-error");
            } else {
                names.set(valName, index);
            }
        }
    });

    if (hasEmptyError) {
        document.getElementById("errorMsgRequired").style.display = "block";
        return false;
    }
    if (hasDuplicateError) {
        document.getElementById("errorMsgDuplicate").style.display = "block";
        return false;
    }
    return true;
}