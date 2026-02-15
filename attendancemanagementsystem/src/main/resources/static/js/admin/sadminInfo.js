document.addEventListener("DOMContentLoaded", function () {
  // =========================================================
  // 要素の取得
  // =========================================================
  const detailForm = document.getElementById("detailForm");
  const successMsg = document.getElementById("serverSuccessMessage");
  const errorMsg = document.getElementById("serverErrorMessage");

// 氏名の入力制限 (スペースを削除)
  const userNameInputForSanitize = document.getElementById("userName");
  if (userNameInputForSanitize) {
    userNameInputForSanitize.addEventListener("input", function () {
      // 半角スペース・全角スペースを空文字に置換
      this.value = this.value.replace(/[ 　]+/g, "");
    });
  }

  // 成功メッセージがあれば完了モーダルを表示
  if (successMsg && successMsg.value) {
    Swal.fire({
      icon: 'success',
      title: '完了',
      text: successMsg.value,
      confirmButtonColor: '#00bdca'
    });
  }

  // エラーメッセージがあればエラーモーダルを表示
  if (errorMsg && errorMsg.value) {
    Swal.fire({
      icon: 'error',
      title: 'エラー',
      text: errorMsg.value,
      confirmButtonColor: '#e74c3c'
    });
  }

  // 保存・削除などで利用しているフォームを対象にする
  const formsToLoad = ["detailForm", "updateForm", "deleteForm"];
  formsToLoad.forEach(formId => {
    const form = document.getElementById(formId);
    if (form) {
      form.addEventListener("submit", function () {
        Swal.fire({
          title: '処理中...',
          allowOutsideClick: false,
          didOpen: () => {
            Swal.showLoading();
          }
        });
      });
    }
  });
  
  // 編集モード関連ボタン
  const btnEditMode = document.getElementById("btnEditMode");
  const btnSave = document.getElementById("btnSave");
  const btnCancel = document.getElementById("btnCancel");
  const btnOpenDeleteModal = document.getElementById("btnOpenDeleteModal");

  // 更新モーダル関連
  const updateModal = document.getElementById("updateModal");
  const btnUpdateCancel = document.getElementById("btnUpdateCancel");
  const btnUpdateSubmit = document.getElementById("btnUpdateSubmit");
  const updatePassword = document.getElementById("updatePassword");
  const toggleUpdateEye = document.getElementById("toggleUpdateEye");

  // 削除モーダル関連
  const deleteModal = document.getElementById("deleteModal");
  const btnDeleteCancel = document.getElementById("btnDeleteCancel");
  const deletePassword = document.getElementById("deletePassword");
  const toggleDeleteEye = document.getElementById("toggleDeleteEye");

  // =========================================================
  // イベントリスナーの登録
  // =========================================================
  if (btnEditMode) btnEditMode.addEventListener("click", toggleEditMode);
  if (btnCancel) btnCancel.addEventListener("click", cancelEdit);
  if (btnSave) btnSave.addEventListener("click", openUpdateModal);
  if (btnOpenDeleteModal) btnOpenDeleteModal.addEventListener("click", openDeleteModal);

  // 更新モーダルのボタン
  if (btnUpdateCancel) btnUpdateCancel.addEventListener("click", closeUpdateModal);
  if (btnUpdateSubmit) btnUpdateSubmit.addEventListener("click", submitUpdate);
  if (toggleUpdateEye) toggleUpdateEye.addEventListener("click", toggleUpdatePasswordVisibility);

  // 削除モーダルのボタン
  if (btnDeleteCancel) btnDeleteCancel.addEventListener("click", closeDeleteModal);
  if (toggleDeleteEye) toggleDeleteEye.addEventListener("click", toggleDeletePasswordVisibility);

  // モーダル外クリックで閉じる処理
  window.addEventListener("click", function (e) {
    if (updateModal && e.target === updateModal) {
      closeUpdateModal();
    }
    if (deleteModal && e.target === deleteModal) {
      closeDeleteModal();
    }
  });

  // =========================================================
  // 1. 編集モード関連の関数
  // =========================================================
  function toggleEditMode() {
    const inputs = document.querySelectorAll(
      '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select'
    );
    const badges = document.querySelectorAll(".badge-required");

    if (!inputs.length) return;

    let isDisabled = inputs[0].disabled;

    if (isDisabled) {
      // 編集モード ON
      inputs.forEach((input) => {
        input.dataset.originalValue = input.value;
        input.disabled = false;
      });
      if (btnEditMode) btnEditMode.style.display = "none";
      if (btnSave) btnSave.style.display = "inline-block";
      if (btnCancel) btnCancel.style.display = "inline-block";
      badges.forEach((badge) => (badge.style.display = "inline-block"));
    } else {
      // 編集モード OFF
      inputs.forEach((input) => (input.disabled = true));
      if (btnEditMode) btnEditMode.style.display = "inline-block";
      if (btnSave) btnSave.style.display = "none";
      if (btnCancel) btnCancel.style.display = "none";
      badges.forEach((badge) => (badge.style.display = "none"));
    }
  }

  function cancelEdit() {
    const inputs = document.querySelectorAll(
      '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select'
    );

    // 値を元に戻す
    inputs.forEach((input) => {
      if (input.dataset.originalValue !== undefined) {
        input.value = input.dataset.originalValue;
      }
      input.disabled = true;
    });

    const badges = document.querySelectorAll(".badge-required");
    if (btnEditMode) btnEditMode.style.display = "inline-block";
    if (btnSave) btnSave.style.display = "none";
    if (btnCancel) btnCancel.style.display = "none";
    badges.forEach((badge) => (badge.style.display = "none"));
  }

  // =========================================================
  // 2. 更新モーダル関連の関数
  // =========================================================
  function openUpdateModal() {
    const nameInput = document.getElementById("userName");
    if (nameInput && !nameInput.value.trim()) {
      alert("氏名は必須入力です");
      return;
    }

    const inputs = document.querySelectorAll(
      '#detailForm input:not([type="hidden"]):not(.no-edit), #detailForm select'
    );

    let hasChanges = false;
    inputs.forEach((input) => {
      if (input.dataset.originalValue !== undefined && input.value !== input.dataset.originalValue) {
        hasChanges = true;
      }
    });

    if (!hasChanges) {
      alert("変更内容がありません。");
      return;
    }

    if (updateModal) {
      updateModal.classList.add("active");
      setTimeout(() => {
        if (updatePassword) updatePassword.focus();
      }, 100);
    }
  }

  // ★不足していた関数：更新モーダルを閉じる
  function closeUpdateModal() {
    if (updateModal) updateModal.classList.remove("active");
    if (updatePassword) updatePassword.value = ""; // パスワードリセット
  }

  // ★不足していた関数：更新を実行する
  function submitUpdate() {
    if (!updatePassword.value.trim()) {
      alert("パスワードを入力してください。");
      return;
    }
    // フォームを送信
    if (detailForm) {
      detailForm.submit();
    }
  }

  // ★不足していた関数：更新モーダルのパスワード表示切替
  function toggleUpdatePasswordVisibility() {
    if (updatePassword && toggleUpdateEye) {
      if (updatePassword.type === "password") {
        updatePassword.type = "text";
        toggleUpdateEye.classList.remove("fa-eye-slash");
        toggleUpdateEye.classList.add("fa-eye");
      } else {
        updatePassword.type = "password";
        toggleUpdateEye.classList.remove("fa-eye");
        toggleUpdateEye.classList.add("fa-eye-slash");
      }
    }
  }

  // =========================================================
  // 3. 削除モーダル関連の関数
  // =========================================================
  function openDeleteModal() {
    const loginIdHidden = document.getElementById("loginIdHidden");
    const userNameInput = document.getElementById("userName");
    const userIdInput = document.getElementById("userId");

    if (deleteModal && loginIdHidden && userNameInput && userIdInput) {
      document.getElementById("modalLoginIdDisplay").textContent = loginIdHidden.value;
      document.getElementById("modalUserNameDisplay").textContent = userNameInput.value;
      document.getElementById("deleteTargetId").value = userIdInput.value;

      deleteModal.classList.add("active");
    }
  }

  function closeDeleteModal() {
    if (deleteModal) deleteModal.classList.remove("active");
    if (deletePassword) deletePassword.value = "";
  }

  function toggleDeletePasswordVisibility() {
    if (deletePassword && toggleDeleteEye) {
      if (deletePassword.type === "password") {
        deletePassword.type = "text";
        toggleDeleteEye.classList.remove("fa-eye-slash");
        toggleDeleteEye.classList.add("fa-eye");
      } else {
        deletePassword.type = "password";
        toggleDeleteEye.classList.remove("fa-eye");
        toggleDeleteEye.classList.add("fa-eye-slash");
      }
    }
  }
});