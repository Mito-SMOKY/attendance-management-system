// 編集モード切替
function toggleEditMode() {
  const inputs = document.querySelectorAll(
    '#detailForm input:not([type="hidden"]), #detailForm select',
  );
  const btnEdit = document.getElementById("btnEditMode");
  const btnSave = document.getElementById("btnSave");
  const badges = document.querySelectorAll(".badge-required");

  let isDisabled = inputs[0].disabled; // 現在の状態

  if (isDisabled) {
    // 編集モードON
    inputs.forEach((input) => (input.disabled = false));
    btnEdit.style.display = "none";
    btnSave.style.display = "inline-block";
    badges.forEach((badge) => (badge.style.display = "inline-block"));
  } else {
    // 編集モードOFF (キャンセル扱いならリロードした方が安全だが、簡易的に戻す)
    inputs.forEach((input) => (input.disabled = true));
    btnEdit.style.display = "inline-block";
    btnSave.style.display = "none";
    badges.forEach((badge) => (badge.style.display = "none"));
  }
}

// 保存処理
function saveUser() {
  if (confirm("変更内容を保存しますか？")) {
    document.getElementById("detailForm").submit();
  }
}

// 削除モーダルを開く
function openDeleteModal() {
  const modal = document.getElementById("deleteModal");

  // 表示用データを取得
  const loginId = document.getElementById("loginIdHidden").value;
  const name = document.getElementById("userName").value;
  const userId = document.getElementById("userId").value;

  // モーダルにセット
  document.getElementById("modalLoginIdDisplay").textContent = loginId;
  document.getElementById("modalUserNameDisplay").textContent = name;
  document.getElementById("deleteTargetId").value = userId; // 送信するのはUserID

  modal.classList.add("active");
}

// 削除モーダルを閉じる
function closeDeleteModal() {
  document.getElementById("deleteModal").classList.remove("active");
  document.getElementById("deletePassword").value = "";
}

// パスワード表示切替
function togglePasswordVisibility() {
  const passInput = document.getElementById("deletePassword");
  const icon = document.querySelector(".toggle-eye");

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
