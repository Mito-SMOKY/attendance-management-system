const dateInput = document.getElementById('dateInput');
const errorMsg = document.getElementById('errorMessage');
const submitBtn = document.getElementById('submitBtn');

// 入力イベント（リアルタイム制限）
dateInput.addEventListener('input', (e) => {
    // 1. 数字以外を即座に削除（全角数字もここで消える）
    let value = e.target.value.replace(/\D/g, '');
    e.target.value = value;

    // 2. 8桁に達した時の日付妥当性チェック
    if (value.length === 8) {
        if (isValidDate(value)) {
            errorMsg.display = 'none';
            dateInput.style.borderColor = 'green';
            submitBtn.disabled = false;
        } else {
            showError();
        }
    } else {
        // 8桁未満は送信不可
        submitBtn.disabled = true;
        dateInput.style.borderColor = '';
    }
});

// 日付が実在するか判定する関数
function isValidDate(str) {
    const y = parseInt(str.substring(0, 4));
    const m = parseInt(str.substring(4, 6)) - 1; // 月は0-11
    const d = parseInt(str.substring(6, 8));
    const date = new Date(y, m, d);
    
    // JSのDateオブジェクトは自動補正（13月→翌年1月など）するため
    // 入力値と生成された値を比較して不一致なら「不正な日付」とみなす
    return date.getFullYear() === y && 
           date.getMonth() === m && 
           date.getDate() === d;
}

function showError() {
    errorMsg.style.display = 'block';
    dateInput.style.borderColor = 'red';
    submitBtn.disabled = true;
}