document.addEventListener('DOMContentLoaded', function() {
    
    // --- 設定 ---
    const API_ENDPOINT = '/admin/api/students'; // データを取得するAPIのURL（※適宜合わせてください）
    const PAGE_SIZE = 10; // 1ページあたりの表示件数

    // --- 要素の取得 ---
    const tableBody = document.getElementById('studentTableBody');
    const paginationContainer = document.getElementById('pagination');
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');

    // --- 状態管理 ---
    let currentPage = 0;
    let currentKeyword = '';

    // --- 初期表示 ---
    fetchData(0);

    // --- イベントリスナー ---
    
    // 検索ボタンクリック
    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            executeSearch();
        });
    }

    // 検索窓でのEnterキー
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault(); // フォーム送信を防ぐ
                executeSearch();
            }
        });
    }

    // 検索実行処理
    function executeSearch() {
        currentKeyword = searchInput.value.trim();
        currentPage = 0; // 検索時は1ページ目に戻す
        fetchData(0);
    }

    // --- データ取得処理 ---
    function fetchData(page) {
        // クエリパラメータの構築
        const params = new URLSearchParams({
            page: page,
            size: PAGE_SIZE,
            keyword: currentKeyword
        });

        fetch(`${API_ENDPOINT}?${params.toString()}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('API request failed');
                }
                return response.json();
            })
            .then(data => {
                // Spring DataのPageオブジェクトの構造を想定
                // data.content: リストデータ, data.totalPages: 総ページ数, data.number: 現在ページ
                const list = data.content || data.students || []; // APIの返し方に合わせて調整可能なように記述
                const totalPages = data.totalPages || 0;
                const pageNum = typeof data.number !== 'undefined' ? data.number : page;

                renderTable(list);
                renderPagination(pageNum, totalPages);
            })
            .catch(error => {
                console.error('Error:', error);
                tableBody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding: 20px;">データの取得に失敗しました</td></tr>';
            });
    }

    // --- テーブル描画処理 ---
    function renderTable(list) {
        tableBody.innerHTML = '';

        if (list.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding: 20px;">データが見つかりません</td></tr>';
            return;
        }

        list.forEach(student => {
            const row = document.createElement('tr');
            
            // APIから返ってくるJSONのキー名に合わせてください
            // ここでは一般的な名前(loginId, name, grade, department, classroom)を想定
            const loginId = escapeHtml(student.loginId || '-');
            const dept = escapeHtml(student.department || '-');
            const grade = escapeHtml(student.grade || '-');
            const cls = escapeHtml(student.classroom || '-');
            const name = escapeHtml(student.name || '-');

            row.innerHTML = `
                <td>${loginId}</td>
                <td>${dept}</td>
                <td>${grade}</td>
                <td>${cls}</td>
                <td>${name}</td>
            `;
            tableBody.appendChild(row);
        });
    }

    // --- ページネーション描画処理 ---
    function renderPagination(current, total) {
        paginationContainer.innerHTML = '';

        if (total <= 1) return; // 1ページしかないならページネーション非表示

        // 「前へ」ボタン
        const prevLink = createPageItem('＜', current - 1, current > 0);
        paginationContainer.appendChild(prevLink);

        // ページ番号 (簡易的に前後2ページを表示するロジック)
        let start = Math.max(0, current - 2);
        let end = Math.min(total - 1, current + 2);

        // 端の調整
        if (start === 0) {
            end = Math.min(total - 1, 4); // 最初の方なら最大5個表示
        }
        if (end === total - 1) {
            start = Math.max(0, total - 5); // 最後の方なら最大5個表示
        }

        for (let i = start; i <= end; i++) {
            const item = createPageItem(i + 1, i, true);
            if (i === current) {
                item.classList.add('active'); // CSSで .active スタイルが当たります
            }
            paginationContainer.appendChild(item);
        }

        // 「次へ」ボタン
        const nextLink = createPageItem('＞', current + 1, current < total - 1);
        paginationContainer.appendChild(nextLink);
    }

    function createPageItem(text, pageIndex, enabled) {
        const a = document.createElement('a');
        a.href = '#';
        a.className = 'page-link';
        a.textContent = text;
        
        if (!enabled) {
            a.style.pointerEvents = 'none';
            a.style.opacity = '0.5';
        } else {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                fetchData(pageIndex);
            });
        }
        return a;
    }

    // XSS対策用エスケープ
    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});