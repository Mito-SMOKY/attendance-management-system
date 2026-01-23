document.addEventListener("DOMContentLoaded", function () {
  // パスワード表示切り替え
  window.togglePassword = function () {
    const passInput = document.getElementById("password");
    const icon = document.querySelector(".toggle-eye");

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
  };

  // 登録ボタン押下時の処理
  window.submitCreate = function () {
    const form = document.getElementById("createForm");

    // ブラウザ標準バリデーション
    if (!form.reportValidity()) {
      return;
    }

    // 簡易チェック (空文字など)
    const loginId = document.getElementById("loginId").value.trim();
    const name = document.getElementById("userName").value.trim();
    const pass = document.getElementById("password").value;

    if (!loginId || !name || !pass) {
      alert("必須項目を入力してください。");
      return;
    }

    if (!confirm("この内容で管理者を登録しますか？")) {
      return;
    }

    // 送信
    form.submit();
  };

  // リアルタイム入力制限 (ログインIDは半角英数のみ)
  const loginIdInput = document.getElementById("loginId");
  if (loginIdInput) {
    loginIdInput.addEventListener("input", function () {
      // 全角文字などを削除
      this.value = this.value.replace(/[^a-zA-Z0-9]/g, "");
    });
  }
});
