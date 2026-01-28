document.addEventListener('DOMContentLoaded', function() {
    
    // --- 設定 ---
    const API_ENDPOINT = '/admin/api/students/students';
    const OPTION_API_ENDPOINT = '/admin/api/search-options';
    const PAGE_SIZE = 10;
    const PAGINATION_SIDE_PAGES = 1; // 現在のページの左右に何ページ表示するか（1なら前後1つずつ）

    // --- 要素の取得 ---
    const tableBody = document.getElementById('studentTableBody');
    const paginationContainer = document.getElementById('pagination');
    
    // 検索・フィルター要素
    const searchInput = document.getElementById('keywordSearch'); 
    const searchBtn = document.getElementById('searchBtn');
    const departmentSelect = document.getElementById('departmentFilter');
    const courseSelect = document.getElementById('courseFilter');

    // --- CSRF対策 ---
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : '';
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : '';

    // --- 状態管理 ---
    let currentPage = 0;
    let currentKeyword = '';

    // --- グローバル関数定義 ---
    window.executeSearch = function() {
        if (searchInput) {
            currentKeyword = searchInput.value.trim();
        }
        currentPage = 0;
        fetchData(0);
    };

    // --- 初期化処理 ---
    fetchFilterOptions();
    fetchData(0);

    // --- イベントリスナー ---
    if (searchBtn) {
        searchBtn.addEventListener('click', window.executeSearch);
    }

    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                window.executeSearch();
            }
        });
    }

    // --- フィルター選択肢生成 ---
    async function fetchFilterOptions() {
        try {
            const response = await fetch(OPTION_API_ENDPOINT);
            if (!response.ok) {
                console.warn('Filter options API not found or error.');
                return;
            }
            const data = await response.json();
            
            if (departmentSelect && data.majors) {
                departmentSelect.innerHTML = '<option value="">全ての学科</option>';
                data.majors.forEach(major => {
                    const option = document.createElement('option');
                    option.value = major.majorId;
                    option.textContent = major.majorName;
                    departmentSelect.appendChild(option);
                });
            }

            if (courseSelect && data.courses) {
                courseSelect.innerHTML = '<option value="">全てのコース</option>';
                data.courses.forEach(course => {
                    const option = document.createElement('option');
                    option.value = course.courseId;
                    option.textContent = course.courseName;
                    courseSelect.appendChild(option);
                });
            }
        } catch (error) {
            console.error('Error fetching filter options:', error);
        }
    }

    // --- データ取得関数 ---
    async function fetchData(pageIndex) {
        try {
            const params = new URLSearchParams({
                page: pageIndex,
                size: PAGE_SIZE
            });

            if (currentKeyword) params.append('keyword', currentKeyword);

            if (departmentSelect && departmentSelect.value) {
                params.append('departmentId', departmentSelect.value);
            }
            if (courseSelect && courseSelect.value) {
                params.append('courseId', courseSelect.value);
            }

            const gradeRadio = document.querySelector('input[name="gradeFilter"]:checked');
            if (gradeRadio && gradeRadio.value) {
                params.append('grade', gradeRadio.value);
            }

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch(`${API_ENDPOINT}?${params.toString()}`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const data = await response.json();
            
            renderTable(data.content);
            renderPagination(data.totalPages, data.number);
            currentPage = data.number;

        } catch (error) {
            console.error('Error fetching data:', error);
            tableBody.innerHTML = '<tr><td colspan="5" style="color:red; padding:20px;">データの取得に失敗しました</td></tr>';
        }
    }

    // --- テーブル描画関数 ---
    function renderTable(students) {
        tableBody.innerHTML = '';

        if (!students || students.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" style="padding:20px;">該当する生徒が見つかりません</td></tr>';
            return;
        }

        students.forEach(student => {
            const tr = document.createElement('tr');

            const idCell = document.createElement('td');
            idCell.textContent = escapeHtml(student.loginId || ''); 
            tr.appendChild(idCell);

            const deptCell = document.createElement('td');
            deptCell.textContent = escapeHtml(student.department || '-');
            tr.appendChild(deptCell);

            const gradeCell = document.createElement('td');
            gradeCell.textContent = escapeHtml(student.grade || '-');
            tr.appendChild(gradeCell);

            const classCell = document.createElement('td');
            classCell.textContent = escapeHtml(student.classroom || '-');
            tr.appendChild(classCell);

            const nameCell = document.createElement('td');
            const link = document.createElement('a');
            link.href = `/admin/student/info/${student.userId}`; 
            link.textContent = student.name;
            link.classList.add('student-name-link');
            nameCell.appendChild(link);
            tr.appendChild(nameCell);

            tableBody.appendChild(tr);
        });
    }

    // --- ★ページネーション描画関数 (修正版) ---
    function renderPagination(totalPages, currentPageIndex) { // currentPageIndexは0始まり
        paginationContainer.innerHTML = '';
        if (totalPages <= 1) return;

        // APIは0始まりだが、表示は1始まりで計算する
        const current = currentPageIndex + 1;
        const sidePages = PAGINATION_SIDE_PAGES;

        // ＜ (前へ) ボタン
        paginationContainer.appendChild(createPageItem('＜', currentPageIndex - 1, currentPageIndex > 0));

        // ページ番号ボタンの生成ロジック
        let lastAddedPage = 0;

        for (let i = 1; i <= totalPages; i++) {
            // 条件: 最初(1) OR 最後(totalPages) OR 現在地の周辺
            if (i === 1 || i === totalPages || (i >= current - sidePages && i <= current + sidePages)) {
                
                // 直前に追加したページとの間に隙間がある場合、'...' を追加
                if (lastAddedPage !== 0 && i > lastAddedPage + 1) {
                    const dots = document.createElement('span');
                    dots.textContent = '...';
                    dots.className = 'page-dots';
                    paginationContainer.appendChild(dots);
                }

                // ボタン作成 (APIへは i-1 を渡す)
                const item = createPageItem(i, i - 1, true);
                if (i === current) {
                    item.classList.add('active');
                }
                paginationContainer.appendChild(item);
                
                lastAddedPage = i;
            }
        }

        // ＞ (次へ) ボタン
        paginationContainer.appendChild(createPageItem('＞', currentPageIndex + 1, currentPageIndex < totalPages - 1));
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

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});