/**
 * 編集モードを開始する
 * (上部の「編集」ボタンから呼ばれる)
 */
function enableEditMode() {
    const form = document.getElementById("masterForm");
    const card = document.querySelector(".classroom-card"); // is-editingをつける対象
    
    // 編集モードクラスを付与 (CSSで表示切り替え)
    card.classList.add("is-editing");
    form.classList.add("is-editing"); // 念のためformにも

    // エラーメッセージは一旦消す
    resetErrors();
}

/**
 * 保存処理を実行する
 * (下部の「保存実行」ボタンから呼ばれる)
 */
function saveData() {
    const form = document.getElementById("masterForm");

    // バリデーション実行
    if (!validateData()) return;

    if (confirm("変更を保存しますか？")) {
        form.submit();
    }
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
 * 行追加 (先ほどの画面と同じロジック)
 */
function addClassroomRow() {
    const tbody = document.querySelector("#classroomTable tbody");
    const newRow = document.createElement("tr");
    
    // HTML構造は existing の tr と合わせる
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
    
    // 追加した行の最初の入力欄にフォーカス
    const input = newRow.querySelector('input[name="classroomName"]');
    if(input) input.focus();
    
    // スクロール (手動登録画面と同じ動き)
    newRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * 行削除
 */
function deleteRow(btn) {
    // 必須チェックなどは要件に合わせて調整してください。ここでは単純に削除。
    if (confirm("この行を削除しますか？")) {
        const row = btn.closest("tr");
        row.remove();
    }
}

/**
 * エラーリセット (変更なし)
 */
function resetErrors() {
    document.getElementById("errorMsgRequired").style.display = "none";
    document.getElementById("errorMsgDuplicate").style.display = "none";
    document.querySelectorAll(".input-error").forEach(el => el.classList.remove("input-error"));
}

/**
 * バリデーション (変更なし)
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