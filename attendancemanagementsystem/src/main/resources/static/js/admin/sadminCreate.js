document.addEventListener("DOMContentLoaded", function () {
  // パスワード表示切り替え (共通化)
  window.togglePassword = function (inputId, iconElement) {
    const input = document.getElementById(inputId);
    if (input && iconElement) {
      if (input.type === "password") {
        input.type = "text";
        iconElement.classList.remove("fa-eye-slash");
        iconElement.classList.add("fa-eye");
      } else {
        input.type = "password";
        iconElement.classList.remove("fa-eye");
        iconElement.classList.add("fa-eye-slash");
      }
    }
  };

  // モーダル表示
  window.showConfirmModal = function () {
    const form = document.getElementById("createForm");

    // 必須項目の簡易チェック (HTML5のバリデーションを実行)
    if (!form.reportValidity()) {
      return; // 未入力があれば吹き出しを出して終了
    }

    // モーダルを表示
    const modal = document.getElementById("confirmModal");
    modal.classList.add("active");

    // モーダル内のパスワード欄にフォーカス
    setTimeout(() => {
      document.getElementById("currentAdminPassword").focus();
    }, 100);
  };

  // モーダル閉じる
  window.closeConfirmModal = function () {
    const modal = document.getElementById("confirmModal");
    modal.classList.remove("active");
    // 入力値をクリア
    document.getElementById("currentAdminPassword").value = "";
  };

  // 確定（フォーム送信）
  window.submitCreate = function () {
    const currentPassInput = document.getElementById("currentAdminPassword");

    if (!currentPassInput.value) {
      alert("パスワードを入力してください。");
      return;
    }

    // 送信
    document.getElementById("createForm").submit();
  };

  // ログインIDの入力制限
  const loginIdInput = document.getElementById("loginId");
  if (loginIdInput) {
    loginIdInput.addEventListener("input", function () {
      this.value = this.value.replace(/[^a-zA-Z0-9]/g, "");
    });
  }

  // 氏名の入力制限 
  const nameInput = document.getElementById("userName"); 
  if (nameInput) {
    nameInput.addEventListener("input", function () {
      // 半角スペース・全角スペースを空文字に置換
      this.value = this.value.replace(/[ 　]+/g, "");
    });
  }

  // モーダル外クリックで閉じる
  const modal = document.getElementById("confirmModal");
  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target === modal) {
        closeConfirmModal();
      }
    });
  }
});
