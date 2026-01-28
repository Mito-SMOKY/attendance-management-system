function showFileName(input) {
            const fileNameArea = document.getElementById('fileName');
            if (input.files && input.files.length > 0) {
                fileNameArea.innerText = "選択中: " + input.files[0].name;
            } else {
                fileNameArea.innerText = "ファイルが選択されていません";
            }
        }