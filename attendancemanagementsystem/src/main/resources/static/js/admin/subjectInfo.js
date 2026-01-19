        document.addEventListener('DOMContentLoaded', function() {
            const input = document.getElementById('subjectSearchInput');
            const list = document.getElementById('suggestionList');
            const loader = document.getElementById('searchLoader');
            let debounceTimer;

            input.addEventListener('input', function() {
                const query = this.value.trim();
                clearTimeout(debounceTimer);

                if (query.length < 1) {
                    list.style.display = 'none';
                    return;
                }

                debounceTimer = setTimeout(() => {
                    performSearch(query);
                }, 300);
            });

            input.addEventListener('blur', function() {
                setTimeout(() => {
                    list.style.display = 'none';
                }, 200);
            });

            input.addEventListener('focus', function() {
                if (list.innerHTML.trim() !== "") {
                    list.style.display = 'block';
                }
            });

            function performSearch(query) {
                loader.style.display = 'block';
                
                fetch(`/admin/api/subject/search?q=${encodeURIComponent(query)}`)
                    .then(response => response.json())
                    .then(data => {
                        loader.style.display = 'none';
                        list.innerHTML = '';

                        if (data.length === 0) {
                            const li = document.createElement('li');
                            li.className = 'no-result';
                            li.textContent = '該当する教科が見つかりません';
                            list.appendChild(li);
                        } else {
                            data.forEach(item => {
                                const li = document.createElement('li');
                                li.className = 'suggestion-item';

                                const url = `/admin/subjectInfo?departmentId=${item.departmentId}&subjectId=${item.id}&grade=${item.grade}`;
                                
                                li.innerHTML = `
                                    <a href="${url}" class="suggestion-link">
                                        <div class="suggest-name">${item.name}</div>
                                        <div class="suggest-meta">${item.classDetail}</div>
                                    </a>
                                `;
                                list.appendChild(li);
                            });
                        }
                        list.style.display = 'block';
                    })
                    .catch(err => {
                        console.error(err);
                        loader.style.display = 'none';
                    });
            }
        });