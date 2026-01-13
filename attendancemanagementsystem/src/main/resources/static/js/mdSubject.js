/**
 * 編集モードの切り替えおよび保存処理
 */
function toggleEditMode() {
    const body = document.body;
    const btn = document.querySelector(".edit-btn");
    const form = document.getElementById("matrixForm");

    if (body.classList.contains("editing-mode")) {
        // --- 適用（保存）ボタン押下時 ---
        if (!confirm("現在の状態で保存しますか？")) {
            return;
        }
        // フォーム送信
        form.submit();
    } else {
        // --- 編集ボタン押下時 ---
        body.classList.add("editing-mode");
        btn.textContent = "適用";
    }
}

/**
 * テーブルセルのクリック処理（チェックの切り替え）
 * @param {HTMLElement} cell クリックされたtd要素
 */
function toggleMark(cell) {
    // 編集モード時のみクリック有効
    if (document.body.classList.contains("editing-mode")) {
        cell.classList.toggle("checked");

        // チェックボックスの同期
        const checkbox = cell.querySelector('input[type="checkbox"]');
        if (checkbox) {
            checkbox.checked = cell.classList.contains("checked");
        }
    }
}

/**
 * 行のフィルタリング（検索・学年絞り込み）
 */
function filterRows() {
    const selectedGrade = document.getElementById("gradeFilter").value;
    const searchInput = document.getElementById("searchInput");
    const keyword = searchInput ? searchInput.value.toLowerCase() : "";
    const rows = document.querySelectorAll(".data-row");

    rows.forEach((row) => {
        const rowGrade = row.getAttribute("data-grade");
        let subjectName = "";
        const textElem = row.querySelector(".subject-text");
        if (textElem) {
            subjectName = textElem.innerText.toLowerCase();
        }

        const isGradeMatch = selectedGrade === "" || rowGrade == selectedGrade;
        const isKeywordMatch = keyword === "" || subjectName.indexOf(keyword) > -1;

        if (isGradeMatch && isKeywordMatch) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

/**
 * 行削除処理
 * @param {HTMLButtonElement} btn 削除ボタン
 */
function deleteRow(btn) {
    if (confirm("この行を削除しますか？\n（保存すると、この教科の紐づけは全て解除されます）")) {
        const row = btn.closest("tr");
        
        // 行内のチェックボックスをすべてOFFにする（念のため）
        const checkboxes = row.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach((cb) => (cb.checked = false));

        // 行を非表示にする（submit時にチェックボックスが送られないようにDOMから消す）
        row.remove();
    }
}

/**
 * 初期化処理
 * ページ読み込み完了後にイベントリスナーを設定
 */
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById("searchInput");
    const searchBtn = document.getElementById("searchBtn");

    // Enterキーで検索ボタンをクリックするイベント
    if (searchInput && searchBtn) {
        searchInput.addEventListener("keypress", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                searchBtn.click();
            }
        });
    }
});