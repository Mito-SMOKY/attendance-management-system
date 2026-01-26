/**
 * 入力値から「特殊文字（記号）」を削除する
 * 対応：半角記号 全般 + 全角記号 全般
 */
function removeSpecialChars(input) {
    // 半角記号 + 全角記号(！-／、：-＠、［-｀、｛-～) を削除
    const regex = /[!"#$%&'()*+,\-./:;<=>?@\[\\\]^_`{|}~！-／：-＠［-｀｛-～]/g;
    input.value = input.value.replace(regex, "");
}

/**
 * 入力値から「数字」と「特殊文字」を削除する
 * 対応：半角数字 + 全角数字 + 半角記号 + 全角記号
 */
function removeNameInvalidChars(input) {
    // 半角数字(0-9) + 全角数字(０-９) + 記号全般 を削除
    const regex = /[0-9０-９!"#$%&'()*+,\-./:;<=>?@\[\\\]^_`{|}~！-／：-＠［-｀｛-～]/g;
    input.value = input.value.replace(regex, "");
}

/**
 * 新しい行を追加する
 */
function addAccount() {
    const container = document.getElementById("account-container");
    const count = container.children.length; 

    const newRow = document.createElement("tr");

    // パスワードを text に変更し、入力制限関数を適用
    newRow.innerHTML = `
        <td class="row-num">${count + 1}</td>
        <td>
            <input type="text" name="accounts[${count}].studentNumber" 
                   placeholder="ID" required 
                   oninput="removeSpecialChars(this)">
        </td>
        <td>
            <input type="text" name="accounts[${count}].name" 
                   placeholder="氏名" required 
                   oninput="removeNameInvalidChars(this)">
        </td>
        <td>
            <input type="text" name="accounts[${count}].password" 
                   placeholder="パスワード" required autocomplete="off">
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

/**
 * 指定した行を削除する
 */
function deleteRow(btn) {
    const container = document.getElementById("account-container");
    
    if (container.children.length <= 1) {
        alert("これ以上削除できません。少なくとも1件は入力してください。");
        return;
    }

    const isConfirmed = confirm("この行を削除してもよろしいですか？");

    if (isConfirmed) {
        const row = btn.closest("tr");
        if (row) {
            container.removeChild(row);
            renumberRows();
        }
    }
}

/**
 * 行番号の振り直し
 */
function renumberRows() {
    const container = document.getElementById("account-container");
    const rows = container.children;

    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        
        const numCell = row.querySelector(".row-num");
        if (numCell) {
            numCell.textContent = i + 1;
        }

        const inputs = row.querySelectorAll("input");
        inputs.forEach(input => {
            const name = input.getAttribute("name");
            if (name) {
                const newName = name.replace(/accounts\[\d+\]/, `accounts[${i}]`);
                input.setAttribute("name", newName);
            }
        });
    }
}