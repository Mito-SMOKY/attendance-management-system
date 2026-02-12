document.addEventListener("DOMContentLoaded", function() {
    const resultImage = document.querySelector('.result-image');
    // 結果画面内のテキスト要素（「書き込み成功！」とメッセージ）を取得
    const textElements = document.querySelectorAll('.result-area .instruction-text, .result-area .result-message');
    const redirectUrl = '/admin/nfc/idle';

    // アニメーションの実行シーケンス
    const startAnimationSequence = () => {
        if (!resultImage) return;

        // 1. まず画像だけをふわっと表示 (CSSの .fade-in を付与)
        resultImage.classList.add('fade-in');

        // 2. 画像が表示されてから 0.8秒後 に文字を表示
        setTimeout(() => {
            textElements.forEach(el => {
                el.classList.add('fade-in');
            });

            // 3. 文字が表示されてから 3秒後 に待機画面へ戻る
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 3000);

        }, 800); // 800ms = 0.8秒の時差
    };

    // 画像の読み込み制御
    if (resultImage) {
        if (resultImage.complete) {
            // キャッシュ等ですぐ表示できる場合も、一瞬待ってから開始（ちらつき防止）
            setTimeout(startAnimationSequence, 100);
        } else {
            // 読み込み完了を待ってから開始
            resultImage.onload = startAnimationSequence;
            // 万が一読み込みエラーでも進行させる
            resultImage.onerror = startAnimationSequence;
        }
    } else {
        // 画像がない場合（念のため）即時開始
        startAnimationSequence();
    }
});