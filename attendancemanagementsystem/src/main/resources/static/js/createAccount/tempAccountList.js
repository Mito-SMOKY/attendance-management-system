document.addEventListener("DOMContentLoaded", function () {
  const urlParams = new URLSearchParams(window.location.search);
  const shouldDownload = urlParams.get("download");
  const dataListIdInput = document.getElementById("dataListIdValue");
  const dataListId = dataListIdInput ? dataListIdInput.value : null;

  // PDFダウンロード用のメイン関数
  async function handlePdfDownload(id) {
    try {
      // 1. まずはHEADリクエストやGETリクエストでステータスを確認
      const response = await fetch("/admin/downloadPdf/" + id);

      if (response.ok) {
        // 2. 正常(200 OK)ならバイナリとして取得してダウンロード実行
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        // ファイル名はサーバー側のContent-Dispositionに従うか、ここで指定
        a.download = `account_list_${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
      } else {
        // 3. エラー（404や500など）の場合はアラートを表示
        alert(
          "データの保持期限が切れたか、対象のデータが見つかりません。\n一覧画面からやり直してください。",
        );
      }
    } catch (error) {
      console.error("Download error:", error);
      alert("通信エラーが発生しました。");
    }
  }

  // 自動ダウンロード（URLパラメータがある場合）
  if (shouldDownload === "true" && dataListId) {
    urlParams.delete("download");
    const newQuery = urlParams.toString() ? "?" + urlParams.toString() : "";
    const newUrl = window.location.pathname + newQuery;
    window.history.replaceState({}, "", newUrl);

    setTimeout(function () {
      handlePdfDownload(dataListId);
    }, 500);
  }

  // 手動ダウンロード（ボタンクリック）
  const downloadBtn = document.getElementById("downloadPdfBtn");
  if (downloadBtn) {
    downloadBtn.addEventListener("click", function () {
      const id = this.getAttribute("data-id");
      handlePdfDownload(id);
    });
  }
});
