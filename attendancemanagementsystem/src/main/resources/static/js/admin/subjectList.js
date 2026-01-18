/**
 * 担当教科一覧画面用スクリプト
 */
const API_ENDPOINT = '/admin/api/subject/list';

document.addEventListener('DOMContentLoaded', () => {
    fetchData(); // 初期データ取得

    // 検索ボタンイベント
    const searchBtn = document.getElementById('searchBtn');
    if(searchBtn){
        searchBtn.addEventListener('click', fetchData);
    }
    
    // 検索ボックスでEnterキー
    const searchInput = document.getElementById('searchInput');
    if(searchInput){
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') fetchData();
        });
    }
});

// データを取得して画面を更新するメイン関数
async function fetchData() {
    try {
        // 1. 現在の入力値・選択値を取得
        const searchInput = document.getElementById('searchInput');
        const keyword = searchInput ? searchInput.value : '';
        
        // チェックされている学年を取得
        const selectedGrades = Array.from(document.querySelectorAll('input[name="grades"]:checked'))
                                      .map(cb => cb.value);
        
        // チェックされているクラスを取得
        const selectedClasses = Array.from(document.querySelectorAll('input[name="classes"]:checked'))
                                       .map(cb => cb.value);

        // 2. クエリパラメータの作成
        const params = new URLSearchParams();
        if (keyword) params.append('search', keyword);
        selectedGrades.forEach(g => params.append('grades', g));
        selectedClasses.forEach(c => params.append('classes', c));

        // 3. APIコール
        const response = await fetch(`${API_ENDPOINT}?${params.toString()}`);
        if (!response.ok) throw new Error('API Error');
        const data = await response.json();

        // 4. テーブル描画
        renderTable(data.subjects);

        // 5. フィルタの選択肢を更新
        updateFilterCheckboxes('gradeFilterContainer', 'grades', data.filterOptions.grades, selectedGrades);
        updateFilterCheckboxes('classFilterContainer', 'classes', data.filterOptions.classes, selectedClasses);

    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

// テーブルを描画する関数
function renderTable(subjects) {
    const tbody = document.getElementById('subjectTableBody');
    if(!tbody) return;
    
    tbody.innerHTML = ''; // クリア

    if (!subjects || subjects.length === 0) {
        tbody.innerHTML = '<tr><td class="no-data">担当している教科はありません。</td></tr>';
        return;
    }

    subjects.forEach(item => {
        const tr = document.createElement('tr');
        const linkUrl = `/admin/subjectInfo?departmentId=${item.departmentId}&subjectId=${item.subjectId}&grade=${item.grade}`;
        
        tr.innerHTML = `
            <td class="subject-info-cell">
                <a href="${linkUrl}" class="subject-link full-width-link">
                <div class="subject-content">
                    <span class="subject-icon"><i class="fa-solid fa-chalkboard-user"></i></span>
                    <div class="subject-text">
                        <span class="class-name">${item.classInfoStr}</span>
                        <span class="subject-name">${item.subjectName}</span>
                    </div>
                    <i class="fa-solid fa-chevron-right arrow-icon"></i>
                </div>
                </a>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// フィルタ（チェックボックス）を更新する関数
function updateFilterCheckboxes(containerId, name, availableOptions, currentSelected) {
    const container = document.getElementById(containerId);
    if(!container) return;

    container.innerHTML = '';

    if (!availableOptions || availableOptions.length === 0) {
        container.innerHTML = '<div style="color:#ccc; font-size:0.9rem;">選択可能な項目はありません</div>';
        return;
    }

    // ソート (学年は数値、クラスは文字)
    availableOptions.sort((a, b) => {
        if (typeof a === 'number' && typeof b === 'number') return a - b;
        return String(a).localeCompare(String(b));
    });

    availableOptions.forEach(opt => {
        const label = document.createElement('label');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = name; // grades または classes
        checkbox.value = opt;
        
        // 以前選択されていたならチェックを入れる
        if (currentSelected.includes(String(opt))) {
            checkbox.checked = true;
        }

        // 変更時に即時検索
        checkbox.addEventListener('change', fetchData);

        const span = document.createElement('span');
        span.textContent = (name === 'grades') ? opt + '年' : opt;

        label.appendChild(checkbox);
        label.appendChild(span);
        container.appendChild(label);
    });
}