// =========================================================
// 関数定義 (DOMContentLoadedの外に書くことでHTMLから確実に見えるようにする)
// =========================================================

// --- 1. 編集モード切替 ---
function toggleEditMode() {
  const inputs = document.querySelectorAll(
    '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select',
  );

  const btnEdit = document.getElementById("btnEditMode");
  const btnSave = document.getElementById("btnSave");
  const btnCancel = document.getElementById("btnCancel"); // ★追加
  const badges = document.querySelectorAll(".badge-required");

  if (!inputs.length) return;

  // 現在の状態を確認 (disabledなら編集モードへ移行)
  let isDisabled = inputs[0].disabled;

  if (isDisabled) {
    // === 編集モード ON ===
    inputs.forEach((input) => {
      // ★追加: 変更前の値を data属性に一時保存しておく
      input.dataset.originalValue = input.value;
      input.disabled = false;
    });

    if (btnEdit) btnEdit.style.display = "none";
    if (btnSave) btnSave.style.display = "inline-block";
    if (btnCancel) btnCancel.style.display = "inline-block"; // ★追加
    badges.forEach((badge) => (badge.style.display = "inline-block"));
  } else {
    // === 編集モード OFF (通常は cancelEdit() を使うが、予備として残す) ===
    inputs.forEach((input) => (input.disabled = true));

    if (btnEdit) btnEdit.style.display = "inline-block";
    if (btnSave) btnSave.style.display = "none";
    if (btnCancel) btnCancel.style.display = "none"; // ★追加
    badges.forEach((badge) => (badge.style.display = "none"));
  }
}

// --- ★追加: 編集キャンセル処理 ---
function cancelEdit() {
  const inputs = document.querySelectorAll(
    '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select',
  );

  // 値を元に戻す
  inputs.forEach((input) => {
    if (input.dataset.originalValue !== undefined) {
      input.value = input.dataset.originalValue;
    }
  });

  // 編集モードを強制的にOFFにする動作と同じなので、トグルロジックを再利用せず明示的に閉じる
  const btnEdit = document.getElementById("btnEditMode");
  const btnSave = document.getElementById("btnSave");
  const btnCancel = document.getElementById("btnCancel");
  const badges = document.querySelectorAll(".badge-required");

  inputs.forEach((input) => (input.disabled = true));

  if (btnEdit) btnEdit.style.display = "inline-block";
  if (btnSave) btnSave.style.display = "none";
  if (btnCancel) btnCancel.style.display = "none";
  badges.forEach((badge) => (badge.style.display = "none"));
}

// --- 2. 更新モーダル関連 ---

function openUpdateModal() {
  // 1. バリデーション (必須チェック)
  const nameInput = document.getElementById("userName");
  if (nameInput && !nameInput.value.trim()) {
    alert("氏名は必須入力です");
    return;
  }

  // ★追加: 2. 変更有無のチェック
  const inputs = document.querySelectorAll(
    '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select',
  );

  let hasChanges = false;
  inputs.forEach((input) => {
    // 元の値と比較
    if (
      input.dataset.originalValue !== undefined &&
      input.value !== input.dataset.originalValue
    ) {
      hasChanges = true;
    }
  });

  if (!hasChanges) {
    alert("変更内容がありません。");
    return; // モーダルを開かずに終了
  }

  // 3. モーダル表示
  const modal = document.getElementById("updateModal");
  if (modal) {
    modal.classList.add("active");
    setTimeout(() => {
      const passInput = document.getElementById("updatePassword");
      if (passInput) passInput.focus();
    }, 100);
  }
}

// --- 3. 削除モーダル関連 ---

function openDeleteModal() {
  const modal = document.getElementById("deleteModal");

  const loginIdHidden = document.getElementById("loginIdHidden");
  const userNameInput = document.getElementById("userName");
  const userIdInput = document.getElementById("userId");

  if (modal && loginIdHidden && userNameInput && userIdInput) {
    document.getElementById("modalLoginIdDisplay").textContent =
      loginIdHidden.value;
    document.getElementById("modalUserNameDisplay").textContent =
      userNameInput.value;
    document.getElementById("deleteTargetId").value = userIdInput.value;

    modal.classList.add("active");
  }
}

function closeDeleteModal() {
  const modal = document.getElementById("deleteModal");
  if (modal) modal.classList.remove("active");

  const passInput = document.getElementById("deletePassword");
  if (passInput) passInput.value = "";
}

function togglePasswordVisibility() {
  const passInput = document.getElementById("deletePassword");
  const icon = passInput ? passInput.nextElementSibling : null;

  if (passInput && icon) {
    if (passInput.type === "password") {
      passInput.type = "text";
      icon.classList.remove("fa-eye-slash");
      icon.classList.add("fa-eye");
    } else {
      passInput.type = "password";
      icon.classList.remove("fa-eye");
      icon.classList.add("fa-eye-slash");
    }
  }
}

// =========================================================
// 初期化処理 (ここだけは読み込み完了を待つ)
// =========================================================
document.addEventListener("DOMContentLoaded", function () {
  // モーダル外クリックで閉じる処理
  window.addEventListener("click", function (e) {
    const updateModal = document.getElementById("updateModal");
    const deleteModal = document.getElementById("deleteModal");

    if (updateModal && e.target === updateModal) {
      closeUpdateModal();
    }
    if (deleteModal && e.target === deleteModal) {
      closeDeleteModal();
    }
  });
});
