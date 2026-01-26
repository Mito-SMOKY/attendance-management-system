document.addEventListener('DOMContentLoaded', function() {
    
    // --- 設定 ---
    const API_ENDPOINT = '/admin/api/students/request-target'; 
    const PAGE_SIZE = 10;

    // --- 要素の取得 ---
    const tableBody = document.getElementById('studentTableBody');
    const paginationContainer = document.getElementById('pagination');
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    
    // pageModeの取得 (存在しない場合はデフォルト値を設定)
    const pageModeElem = document.getElementById('pageMode');
    const pageMode = pageModeElem ? pageModeElem.value : 'select';
    
    // 追加要素（全選択チェックボックス、件数表示、決定ボタン）
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const selectionCountLabel = document.getElementById('selectionCount');
    const submitBtn = document.getElementById('submitSelectionBtn');

    // --- 状態管理 ---
    let currentPage = 0;
    let currentKeyword = '';
    
    // 選択されたIDを保持するSet
    const selectedIds = new Set();
    
    // 現在のページに表示されている「選択可能な」IDのリスト（一括選択用）
    let currentPageAvailableStudentIds = [];

    // --- 初期表示 ---
    fetchData(0);

    // --- イベントリスナー ---
    if (searchBtn) {
        searchBtn.addEventListener('click', () => executeSearch());
    }
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                executeSearch();
            }
        });
    }

    // 「全選択」チェックボックスのイベント
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const isChecked = this.checked;
            
            // 現在表示されている「選択可能な」生徒IDに対して処理
            currentPageAvailableStudentIds.forEach(id => {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.delete(id);
                }
            });
    
            // 画面上の個別チェックボックスも連動させる（disabledなものは除外）
            const checkboxes = document.querySelectorAll('.student-checkbox:not(:disabled)');
            checkboxes.forEach(cb => cb.checked = isChecked);
    
            updateSelectionUI();
        });
    }

    // 決定ボタンのイベント
    if (submitBtn) {
        submitBtn.addEventListener('click', function() {
            if (selectedIds.size === 0) return;
            
            const idArray = Array.from(selectedIds);
            let url = '';
    
            // ★重要: モードごとの遷移先URLを振り分け
            if (pageMode === 'delete') {
                url = `/admin/request/delete/confirm?ids=${idArray.join(',')}`;
            } else if (pageMode === 'status') {
                url = `/admin/request/status/confirm?ids=${idArray.join(',')}`;
            } else if (pageMode === 'course') {
                url = `/admin/request/department/confirm?ids=${idArray.join(',')}`;
            } else {
                // デフォルト（拡張用）
                url = `/admin/request/confirm?mode=${pageMode}&ids=${idArray.join(',')}`;
            }
    
            window.location.href = url;
        });
    }

    // --- 関数定義 ---

    function executeSearch() {
        if (searchInput) {
            currentKeyword = searchInput.value.trim();
        }
        currentPage = 0;
        selectedIds.clear(); 
        updateSelectionUI();
        
        fetchData(0);
    }

    function fetchData(page) {
        const params = new URLSearchParams({
            page: page,
            size: PAGE_SIZE,
            keyword: currentKeyword,
            mode: pageMode
        });

        fetch(`${API_ENDPOINT}?${params.toString()}`)
            .then(response => {
                if (!response.ok) throw new Error('API request failed');
                return response.json();
            })
            .then(data => {
                const list = data.content || [];
                const totalPages = data.totalPages || 0;
                const pageNum = typeof data.number !== 'undefined' ? data.number : page;

                // 現在のページのIDリストを更新（申請中は除外）
                currentPageAvailableStudentIds = list
                    .filter(s => !s.isPending) // 申請中は除外
                    .map(s => s.userId);

                renderTable(list);
                renderPagination(pageNum, totalPages);
                
                updateMasterCheckboxState();
            })
            .catch(error => {
                console.error('Error:', error);
                if (tableBody) {
                    tableBody.innerHTML = '<tr><td colspan="7">データの取得に失敗しました</td></tr>';
                }
            });
    }

    function renderTable(list) {
        if (!tableBody) return;
        
        tableBody.innerHTML = '';

        if (list.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" style="padding: 20px;">対象の生徒が見つかりません</td></tr>';
            if (selectAllCheckbox) {
                selectAllCheckbox.checked = false;
                selectAllCheckbox.disabled = true;
            }
            return;
        }

        if (selectAllCheckbox) {
            selectAllCheckbox.disabled = false;
        }

        list.forEach(student => {
            const row = document.createElement('tr');
            
            const userId = student.userId;
            const loginId = escapeHtml(student.loginId);
            const dept = escapeHtml(student.department);
            const grade = escapeHtml(student.grade);
            const cls = escapeHtml(student.classroom);
            const name = escapeHtml(student.name);
            const status = escapeHtml(student.status);
            
            // 申請中フラグ
            const isPending = student.isPending === true;

            const isChecked = selectedIds.has(userId) ? 'checked' : '';
            const isDisabled = isPending ? 'disabled' : '';
            // 申請中は背景色を変えてわかりやすく
            const rowClass = isPending ? 'style="background-color: #f9f9f9; color: #999;"' : '';

            // 状態欄の表示作成
            let statusDisplay = status;
            if (isPending) {
                statusDisplay += ' <p style="color:red; font-weight:bold; font-size:0.8em; margin-left:5px;">(申請中)</p>';
            }

            row.innerHTML = `
                <td ${rowClass}>
                    <input type="checkbox" class="student-checkbox" value="${userId}" ${isChecked} ${isDisabled}>
                </td>
                <td ${rowClass}>${statusDisplay}</td>
                <td ${rowClass}>${loginId}</td>
                <td ${rowClass}>${dept}</td>
                <td ${rowClass}>${grade}</td>
                <td ${rowClass}>${cls}</td>
                <td ${rowClass}>${name}</td>
            `;
            tableBody.appendChild(row);
        });

        // 個別のチェックボックスにイベントを設定
        const checkboxes = document.querySelectorAll('.student-checkbox');
        checkboxes.forEach(cb => {
            if (!cb.disabled) {
                cb.addEventListener('change', function() {
                    const id = parseInt(this.value); 
                    if (this.checked) {
                        selectedIds.add(id);
                    } else {
                        selectedIds.delete(id);
                    }
                    updateSelectionUI();
                    updateMasterCheckboxState();
                });
            }
        });
    }

    // UI更新（件数表示、ボタン活性化）
    function updateSelectionUI() {
        if (!selectionCountLabel || !submitBtn) return;

        const count = selectedIds.size;
        selectionCountLabel.textContent = `${count}件選択中`;
        
        if (count > 0) {
            submitBtn.disabled = false;
            submitBtn.style.opacity = '1';
        } else {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.6';
        }
    }

    // 「全選択」チェックボックスの状態更新
    function updateMasterCheckboxState() {
        if (!selectAllCheckbox) return;

        if (currentPageAvailableStudentIds.length === 0) {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.disabled = true;
            return;
        }
        
        selectAllCheckbox.disabled = false;
        
        // 全ての「選択可能な」生徒が選択されているかチェック
        const allSelected = currentPageAvailableStudentIds.every(id => selectedIds.has(id));
        selectAllCheckbox.checked = allSelected;
    }

    // --- ページネーション ---
    function renderPagination(current, total) {
        if (!paginationContainer) return;
        
        paginationContainer.innerHTML = '';
        if (total <= 1) return;

        const prevLink = createPageItem('＜', current - 1, current > 0);
        paginationContainer.appendChild(prevLink);

        let start = Math.max(0, current - 2);
        let end = Math.min(total - 1, current + 2);

        if (start === 0) end = Math.min(total - 1, 4);
        if (end === total - 1) start = Math.max(0, total - 5);

        for (let i = start; i <= end; i++) {
            const item = createPageItem(i + 1, i, true);
            if (i === current) item.classList.add('active');
            paginationContainer.appendChild(item);
        }

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

    // 文字列エスケープ処理
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