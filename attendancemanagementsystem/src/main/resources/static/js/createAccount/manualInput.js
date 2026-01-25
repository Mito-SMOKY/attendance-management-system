function addAccount() {
        const container = document.getElementById("account-container");
        const count = container.children.length; // 現在の要素数

        // 新しい入力グループを作成
        const newGroup = document.createElement("div");
        newGroup.className = "input-group";
        newGroup.innerHTML = `
                <h3>No.${count + 1}</h3>
                <div class="input-row">
                    <label>ログインID</label>
                    <input type="text" name="accounts[${count}].studentNumber" required>
                </div>
                <div class="input-row">
                    <label>氏名</label>
                    <input type="text" name="accounts[${count}].name" required>
                </div>
                <div class="input-row">
                    <label>パスワード</label>
                    <input type="text" name="accounts[${count}].password" required>
                </div>
            `;

        container.appendChild(newGroup);
    }

function removeAccount() {
    const container = document.getElementById("account-container");
    if (container.children.length > 1) {
        container.removeChild(container.lastElementChild);
    } else {
        alert("これ以上削除できません");
    }
}