document.addEventListener('DOMContentLoaded', function() {
    
    // --- 設定 ---
    const API_ENDPOINT = '/admin/api/students/request-target'; 
    const PAGE_SIZE = 10;

    // --- 要素の取得 ---
    const tableBody = document.getElementById('studentTableBody');
    const paginationContainer = document.getElementById('pagination');
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    
    const pageModeElem = document.getElementById('pageMode');
    const pageMode = pageModeElem ? pageModeElem.value : 'select';
    
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const selectionCountLabel = document.getElementById('selectionCount');
    const submitBtn = document.getElementById('submitSelectionBtn');

    // --- 状態管理 ---
    let currentPage = 0;
    let currentKeyword = '';
    
    const selectedIds = new Set();
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

    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const isChecked = this.checked;
            
            currentPageAvailableStudentIds.forEach(id => {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.delete(id);
                }
            });
    
            const checkboxes = document.querySelectorAll('.student-checkbox:not(:disabled)');
            checkboxes.forEach(cb => cb.checked = isChecked);
    
            updateSelectionUI();
        });
    }

    if (submitBtn) {
        submitBtn.addEventListener('click', function() {
            if (selectedIds.size === 0) return;
            
            const idArray = Array.from(selectedIds);
            
            // ★NaNチェック: もしIDが取れていない場合はアラートを出す
            if (idArray.some(id => isNaN(id))) {
                alert('エラー: 一部の生徒IDが正しく取得できませんでした。画面を更新して再度お試しください。');
                return;
            }

            let url = '';
            if (pageMode === 'delete') {
                url = `/admin/request/delete/confirm?ids=${idArray.join(',')}`;
            } else if (pageMode === 'status') {
                url = `/admin/request/status/confirm?ids=${idArray.join(',')}`;
            } else if (pageMode === 'course') {
                url = `/admin/request/department/confirm?ids=${idArray.join(',')}`;
            } else {
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

                // サーバー側で "userId" に統一したため、ここで正しくIDが取れるようになります
                currentPageAvailableStudentIds = list
                    .filter(s => !s.isPending)
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
            
            // ★サーバー修正により student.userId が確実に値を持つようになります
            const userId = student.userId; 
            
            const loginId = escapeHtml(student.loginId);
            const dept = escapeHtml(student.department);
            const grade = escapeHtml(student.grade);
            const cls = escapeHtml(student.classroom);
            const name = escapeHtml(student.name);
            const status = escapeHtml(student.status); // サーバー修正により値が入ります
            
            const isPending = student.isPending === true;
            const isChecked = selectedIds.has(userId) ? 'checked' : '';
            
            // 申請中は行をグレーアウト
            const rowClass = isPending ? 'style="background-color: #f9f9f9; color: #999;"' : '';

            let checkboxCellContent;
            if (isPending) {
                checkboxCellContent = '<span style="color: #ff4d4f; font-weight: bold; font-size: 0.85rem;">申請中</span>';
            } else {
                checkboxCellContent = `<input type="checkbox" class="student-checkbox" value="${userId}" ${isChecked}>`;
            }

            row.innerHTML = `
                <td ${rowClass}>${checkboxCellContent}</td>
                <td ${rowClass}>${status}</td>
                <td ${rowClass}>${loginId}</td>
                <td ${rowClass}>${dept}</td>
                <td ${rowClass}>${grade}</td>
                <td ${rowClass}>${cls}</td>
                <td ${rowClass}>${name}</td>
            `;
            tableBody.appendChild(row);
        });

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

    function updateMasterCheckboxState() {
        if (!selectAllCheckbox) return;
        if (currentPageAvailableStudentIds.length === 0) {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.disabled = true;
            return;
        }
        selectAllCheckbox.disabled = false;
        const allSelected = currentPageAvailableStudentIds.every(id => selectedIds.has(id));
        selectAllCheckbox.checked = allSelected;
    }

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