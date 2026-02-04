document.addEventListener("DOMContentLoaded", function () {
  // 1. URLパラメータの取得
  const urlParams = new URLSearchParams(window.location.search);
  const shouldDownload = urlParams.get("download");

  // ID取得
  const dataListIdInput = document.getElementById("dataListIdValue");
  const dataListId = dataListIdInput ? dataListIdInput.value : null;

  // 2. ダウンロード条件を満たしている場合
  if (shouldDownload === "true" && dataListId) {
    // パラメータ削除
    urlParams.delete("download");
    const newQuery = urlParams.toString() ? "?" + urlParams.toString() : "";
    const newUrl = window.location.pathname + newQuery;
    window.history.replaceState({}, "", newUrl);

    // 3. ダウンロード開始 (★変更: downloadCsv -> downloadPdf)
    setTimeout(function () {
      window.location.href = "/admin/downloadPdf/" + dataListId;
    }, 500);
  }
});
