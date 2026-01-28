/* =========================================
   入力制限・セキュリティ対策用
   ========================================= */

// 危険な記号の定義（半角 ＆ 全角）
// インジェクション、XSS、ディレクトリトラバーサル対策
const DANGEROUS_CHARS = /[<>"'/\\;|:*?＜＞”’“‘／￥＼；：＊？]/g;

/**
 * データリスト名用
 * HTML側で oninput="sanitizeListName(this)" と指定して使用
 * 対応：日本語OK。ただし、半角・全角の危険記号はすべて削除。
 */
function sanitizeListName(input) {
    let val = input.value;

    // 1. 危険記号を削除 (半角・全角すべて)
    val = val.replace(DANGEROUS_CHARS, "");

    // 2. 文字数制限 (255文字)
    if (val.length > 255) {
        val = val.slice(0, 255);
    }

    input.value = val;
}

/* =========================================
   ページ読み込み時 (年度入力欄の制御)
   ========================================= */
document.addEventListener('DOMContentLoaded', function() {
    
    const yearInput = document.getElementById('academicYear');
    
    if (yearInput) {
        // -----------------------------------------------
        // 1. 初手で現在の年度を自動入力
        // -----------------------------------------------
        if (!yearInput.value) {
            const today = new Date();
            const month = today.getMonth() + 1; 
            const currentFiscalYear = (month >= 4) ? today.getFullYear() : today.getFullYear() - 1;
            yearInput.value = currentFiscalYear;
        }

        // -----------------------------------------------
        // 2. キー入力制限（e, -, + を無効化）
        // -----------------------------------------------
        yearInput.addEventListener('keydown', function(e) {
            if (['e', 'E', '-', '+', '.'].includes(e.key)) {
                e.preventDefault();
            }
        });

        // -----------------------------------------------
        // 3. 入力値変更時の制限（4文字制限）
        // -----------------------------------------------
        yearInput.addEventListener('input', function() {
            if (this.value.length > 4) {
                this.value = this.value.slice(0, 4);
            }
        });

        yearInput.addEventListener('blur', function() {
            if (this.value.length > 4) {
                this.value = this.value.slice(0, 4);
            }
        });
    }
});