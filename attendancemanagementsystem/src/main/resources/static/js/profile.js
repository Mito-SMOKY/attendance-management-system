document.addEventListener('DOMContentLoaded', () => {
    const showMoreButton = document.getElementById('showMoreButton');
    // クラス名を subject-list に修正
    const subjectItems = document.querySelectorAll('.subject-list li'); 
    const initialVisibleCount = 5; 
    let isExpanded = false;

    // 教科が初期表示数以下の場合はボタンを非表示にする
    if (subjectItems.length <= initialVisibleCount) {
        showMoreButton.style.display = 'none';
        return;
    }

    // 初期状態で隠すべき要素を設定
    subjectItems.forEach((item, index) => {
        if (index >= initialVisibleCount) {
            // クラス名を hidden-subject に修正
            item.classList.add('hidden-subject'); 
        }
    });

    // ボタンクリック時の挙動
    showMoreButton.addEventListener('click', () => {
        isExpanded = !isExpanded; 

        subjectItems.forEach((item, index) => {
            if (index >= initialVisibleCount) {
                if (isExpanded) {
                    item.classList.remove('hidden-subject');
                } else {
                    item.classList.add('hidden-subject');
                }
            }
        });

        showMoreButton.textContent = isExpanded ? '閉じる' : 'さらに表示';
    });
});