document.addEventListener("DOMContentLoaded", function () {
  // ---------------------------------------------------
  // URLパラメータを操作する共通関数
  // ---------------------------------------------------
  function updateUrlParam(key, value) {
    const url = new URL(window.location.href);

    if (value) {
      // 値があればセット (例: ?q=test)
      url.searchParams.set(key, value);
    } else {
      // 値がなければパラメータ削除
      url.searchParams.delete(key);
    }

    // 画面遷移実行
    window.location.href = url.toString();
  }

  // ---------------------------------------------------
  // 要素の取得
  // ---------------------------------------------------
  const searchBtn = document.getElementById("searchBtn");
  const searchInput = document.getElementById("searchInput");
  const filterBtn = document.getElementById("filterBtn");

  const modalOverlay = document.getElementById("filterModal");
  const modalCancelBtn = document.getElementById("modalCancelBtn");
  const modalApplyBtn = document.getElementById("modalApplyBtn");
  const authRadios = document.getElementsByName("authOption");

  // ---------------------------------------------------
  // 検索実行ロジック
  // ---------------------------------------------------
  function executeSearch() {
    const keyword = searchInput.value;
    // 'q' パラメータを更新してリロード (authパラメータなどは維持される)
    updateUrlParam("q", keyword);
  }

  if (searchBtn && searchInput) {
    // ① [検索]ボタンをクリックしたとき
    searchBtn.addEventListener("click", function () {
      executeSearch();
    });

    // ② テキストボックスで [Enter] キーを押したとき
    searchInput.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        e.preventDefault(); // 意図しないフォーム送信などを防ぐ
        executeSearch();
      }
    });
  }

  // ---------------------------------------------------
  // 絞り込みボタン（じょうごアイコン）の処理
  // ---------------------------------------------------
  if (filterBtn) {
    filterBtn.addEventListener("click", function () {
      const url = new URL(window.location.href);
      // URLパラメータ 'auth' の値を取得 (例: "1", "2", または null)
      const currentAuth = url.searchParams.get("auth") || "";

      // モーダル内のラジオボタンの状態を現在のURLパラメータに合わせる
      let matched = false;
      for (const radio of authRadios) {
        if (radio.value === currentAuth) {
          radio.checked = true;
          matched = true;
          break;
        }
      }

      // 万が一一致するものがなければ「全て表示(value="")」を選択
      if (!matched) {
        const defaultRadio = document.querySelector(
          'input[name="authOption"][value=""]',
        );
        if (defaultRadio) defaultRadio.checked = true;
      }

      // モーダル表示
      modalOverlay.classList.add("active");
    });
  }

  // ---------------------------------------------------
  // モーダル内のボタン処理
  // ---------------------------------------------------

  // キャンセルボタン
  if (modalCancelBtn) {
    modalCancelBtn.addEventListener("click", function () {
      modalOverlay.classList.remove("active");
    });
  }

  // モーダルの背景クリックで閉じる
  if (modalOverlay) {
    modalOverlay.addEventListener("click", function (e) {
      if (e.target === modalOverlay) {
        modalOverlay.classList.remove("active");
      }
    });
  }

  // 適用ボタン
  if (modalApplyBtn) {
    modalApplyBtn.addEventListener("click", function () {
      let selectedValue = null;

      // 選択されているラジオボタンの値を取得
      for (const radio of authRadios) {
        if (radio.checked) {
          selectedValue = radio.value;
          break;
        }
      }

      // URLパラメータを更新してリロード
      const url = new URL(window.location.href);
      if (selectedValue) {
        url.searchParams.set("auth", selectedValue);
      } else {
        url.searchParams.delete("auth");
      }
      window.location.href = url.toString();
    });
  }

  // ---------------------------------------------------
  // 行クリック時の遷移処理
  // ---------------------------------------------------
  window.goToDetail = function (id) {
    location.href = "/admin/sadminInfo/" + id;
  };
});
