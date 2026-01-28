/* =========================================
   入力制限・セキュリティ対策用 関数群
   ========================================= */

// 危険な記号の定義（半角 ＆ 全角）
// これらはインジェクション攻撃やディレクトリトラバーサル、
// XSSの回避策として使われる可能性があるため、徹底的に削除します。
const DANGEROUS_CHARS = /[<>"'/\\;|:*?＜＞”’“‘／￥＼；：＊？]/g;

/**
 * データリスト名用
 * 対応：日本語OK。ただし、半角・全角の危険記号はすべて削除。
 */
function sanitizeListName(input) {
    // 1. 危険記号を削除 (半角・全角すべて)
    input.value = input.value.replace(DANGEROUS_CHARS, "");

    // 2. 文字数制限 (255文字)
    if (input.value.length > 255) {
        input.value = input.value.slice(0, 255);
    }
}

/**
 * ログインID用
 * 対応：半角英数字のみ（最強のホワイトリスト方式）
 * ※この方式なら、全角記号（＜など）も「英数字ではない」ため自動的に消えます
 */
function sanitizeId(input) {
    let val = input.value;

    // 1. 全角英数字（０-９ａ-ｚＡ-Ｚ）を半角に変換
    val = val.replace(/[０-９ａ-ｚＡ-Ｚ]/g, function(s) {
        return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
    });

    // 2. 半角英数字(0-9, a-z, A-Z) 以外をすべて削除
    // ここで「＜」や「；」などの記号は（半角・全角問わず）すべて消滅します
    val = val.replace(/[^0-9a-zA-Z]/g, "");

    // 3. 文字数制限 (50文字)
    if (val.length > 50) {
        val = val.slice(0, 50);
    }

    input.value = val;
}

/**
 * 氏名用
 * 対応：数字削除、危険記号(半角/全角)削除
 */
function sanitizeName(input) {
    let val = input.value;

    // 1. 危険記号を削除（半角・全角すべて）
    val = val.replace(DANGEROUS_CHARS, "");

    // 2. 数字、その他氏名として不適切な記号を削除
    // ※半角記号、全角記号の範囲を指定して削除
    const invalidNameChars = /[0-9０-９!#$%&()+,\-.\=@\[\]^_`{|}~！-／：-＠［-｀｛-～]/g;
    val = val.replace(invalidNameChars, "");

    // 3. 文字数制限 (255文字)
    if (val.length > 255) {
        val = val.slice(0, 255);
    }

    input.value = val;
}

/**
 * パスワード用
 * 対応：半角英数記号のみ許可。ただし危険記号は削除。全角は一切不可。
 */
function sanitizePassword(input) {
    let val = input.value;

    // 1. ASCII印字可能文字（半角英数記号）以外をすべて削除
    // これにより、全角文字（＜、＞、あ、漢字など）はすべて消えます
    val = val.replace(/[^ -~]/g, "");

    // 2. 残った半角文字の中から、さらに危険な記号 (< > " ' / \ ;) を削除
    val = val.replace(/[<>"'/\\;|:*?]/g, "");

    // 3. 文字数制限 (255文字)
    if (val.length > 255) {
        val = val.slice(0, 255);
    }

    input.value = val;
}

/* =========================================
   テーブル操作用 関数 (行追加・削除)
   ========================================= */

function addAccount() {
    const container = document.getElementById("account-container");
    const count = container.children.length; 
    const newRow = document.createElement("tr");

    newRow.innerHTML = `
        <td class="row-num">${count + 1}</td>
        <td>
            <input type="text" name="accounts[${count}].studentNumber" 
                   placeholder="ID(50文字)" required 
                   oninput="sanitizeId(this)">
        </td>
        <td>
            <input type="text" name="accounts[${count}].name" 
                   placeholder="氏名(255文字)" required 
                   oninput="sanitizeName(this)">
        </td>
        <td>
            <input type="text" name="accounts[${count}].password" 
                   placeholder="パスワード(255文字)" required autocomplete="off"
                   oninput="sanitizePassword(this)">
        </td>
        <td class="action-cell">
            <button type="button" class="btn-icon-delete" onclick="deleteRow(this)">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        </td>
    `;
    container.appendChild(newRow);
    newRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function deleteRow(btn) {
    const container = document.getElementById("account-container");
    if (container.children.length <= 1) {
        alert("これ以上削除できません。少なくとも1件は入力してください。");
        return;
    }
    if (confirm("この行を削除してもよろしいですか？")) {
        const row = btn.closest("tr");
        if (row) {
            container.removeChild(row);
            renumberRows();
        }
    }
}

function renumberRows() {
    const container = document.getElementById("account-container");
    const rows = container.children;
    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        const numCell = row.querySelector(".row-num");
        if (numCell) numCell.textContent = i + 1;
        
        const inputs = row.querySelectorAll("input");
        inputs.forEach(input => {
            const name = input.getAttribute("name");
            if (name) {
                input.setAttribute("name", name.replace(/accounts\[\d+\]/, `accounts[${i}]`));
            }
        });
    }
}

/* =========================================
   ページ読み込み時 (年度入力欄の制御)
   ========================================= */
document.addEventListener('DOMContentLoaded', function() {
    const yearInput = document.querySelector('input[name="academicYear"]');
    if (yearInput) {
        if (!yearInput.value) {
            const today = new Date();
            const month = today.getMonth() + 1; 
            const currentFiscalYear = (month >= 4) ? today.getFullYear() : today.getFullYear() - 1;
            yearInput.value = currentFiscalYear;
        }
        yearInput.addEventListener('keydown', function(e) {
            if (e.key === '-' || e.key === 'e') e.preventDefault();
        });
        yearInput.addEventListener('input', function() {
            if (this.value < 0) this.value = '';
            if (this.value.length > 4) this.value = this.value.slice(0, 4);
        });
    }
});