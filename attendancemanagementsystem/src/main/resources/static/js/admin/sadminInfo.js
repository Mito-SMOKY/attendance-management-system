// =========================================================
// 関数定義 (DOMContentLoadedの外に書くことでHTMLから確実に見えるようにする)
// =========================================================

// --- 1. 編集モード切替 ---
function toggleEditMode() {
  // ★変更: :not(.no-edit) を追加して、メールアドレスなどを除外する
  const inputs = document.querySelectorAll(
    '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select',
  );

  const btnEdit = document.getElementById("btnEditMode");
  const btnSave = document.getElementById("btnSave");
  const badges = document.querySelectorAll(".badge-required");

  if (!inputs.length) return;

  // (以下変更なし)
  let isDisabled = inputs[0].disabled;

  if (isDisabled) {
    // 編集モードON
    inputs.forEach((input) => (input.disabled = false));
    if (btnEdit) btnEdit.style.display = "none";
    if (btnSave) btnSave.style.display = "inline-block";
    badges.forEach((badge) => (badge.style.display = "inline-block"));
  } else {
    // 編集モードOFF
    inputs.forEach((input) => (input.disabled = true));
    if (btnEdit) btnEdit.style.display = "inline-block";
    if (btnSave) btnSave.style.display = "none";
    badges.forEach((badge) => (badge.style.display = "none"));
  }
}

// --- 2. 更新モーダル関連 ---

function openUpdateModal() {
  // 簡易バリデーション
  const nameInput = document.getElementById("userName");
  if (nameInput && !nameInput.value.trim()) {
    alert("氏名は必須入力です");
    return;
  }

  const modal = document.getElementById("updateModal");
  if (modal) {
    modal.classList.add("active");
    setTimeout(() => {
      const passInput = document.getElementById("updatePassword");
      if (passInput) passInput.focus();
    }, 100);
  }
}

function closeUpdateModal() {
  const modal = document.getElementById("updateModal");
  if (modal) modal.classList.remove("active");

  const passInput = document.getElementById("updatePassword");
  if (passInput) passInput.value = "";
}

function submitUpdate() {
  const passInput = document.getElementById("updatePassword");
  if (passInput && !passInput.value) {
    alert("パスワードを入力してください");
    return;
  }
  const form = document.getElementById("detailForm");
  if (form) form.submit();
}

function toggleUpdatePasswordVisibility() {
  const passInput = document.getElementById("updatePassword");
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
