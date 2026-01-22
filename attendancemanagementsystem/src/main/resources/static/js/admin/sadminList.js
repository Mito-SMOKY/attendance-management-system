document.addEventListener("DOMContentLoaded", function () {
  // ... (共通関数 updateUrlParam や要素取得はそのまま) ...

  const searchBtn = document.getElementById("searchBtn");
  const searchInput = document.getElementById("searchInput");
  const filterBtn = document.getElementById("filterBtn");

  const modalOverlay = document.getElementById("filterModal");
  const modalCancelBtn = document.getElementById("modalCancelBtn");
  const modalApplyBtn = document.getElementById("modalApplyBtn");
  const authRadios = document.getElementsByName("authOption");

  // ... (検索ロジック executeSearch などはそのまま) ...

  // ---------------------------------------------------
  // ★変更点：絞り込みボタンでモーダルを開く
  // ---------------------------------------------------
  if (filterBtn) {
    filterBtn.addEventListener("click", function () {
      const url = new URL(window.location.href);
      // URLパラメータ 'auth' の値を取得 (例: "1", "2", または null)
      const currentAuth = url.searchParams.get("auth") || "";

      // name="authOption" の中で、valueが現在のパラメータと一致するものをチェックする
      // (一致するものがなければ value="" (全て) が選ばれるようにする)
      let matched = false;
      for (const radio of authRadios) {
        if (radio.value === currentAuth) {
          radio.checked = true;
          matched = true;
          break;
        }
      }

      // 万が一一致するものがなければ「全て表示」を選択
      if (!matched) {
        const defaultRadio = document.querySelector(
          'input[name="authOption"][value=""]',
        );
        if (defaultRadio) defaultRadio.checked = true;
      }

      modalOverlay.classList.add("active");
    });
  }

  // ---------------------------------------------------
  // ★変更なし：モーダル内のボタン処理
  // ---------------------------------------------------
  if (modalCancelBtn) {
    modalCancelBtn.addEventListener("click", function () {
      modalOverlay.classList.remove("active");
    });
  }

  if (modalOverlay) {
    modalOverlay.addEventListener("click", function (e) {
      if (e.target === modalOverlay) {
        modalOverlay.classList.remove("active");
      }
    });
  }

  if (modalApplyBtn) {
    modalApplyBtn.addEventListener("click", function () {
      let selectedValue = null;
      for (const radio of authRadios) {
        if (radio.checked) {
          selectedValue = radio.value;
          break;
        }
      }
      // updateUrlParam("auth", selectedValue === "" ? null : selectedValue);
      // 空文字ならパラメータ削除、値があればセット
      const url = new URL(window.location.href);
      if (selectedValue) {
        url.searchParams.set("auth", selectedValue);
      } else {
        url.searchParams.delete("auth");
      }
      window.location.href = url.toString();
    });
  }

  window.goToDetail = function (id) {
    location.href = "/admin/sadminInfo/" + id;
  };
});
