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

$(function() {

// -------------------------------------------------------------
// 【補助関数】編集モードを終了し、表示モードに戻す関数
// -------------------------------------------------------------
function exitEditMode($group, currentValue) {
    // 1. 表示テキストの値を更新する
    $group.find('.display-value').text(currentValue);
    
    // 2. 確定/キャンセルボタンを非表示にする
    $group.find('.save-button, .cancel-button').addClass('hidden');
    
    // 3. 編集/変更ボタンを表示する
    $group.find('.edit-button').removeClass('hidden');
    
    // 4. 入力フィールドを非表示にする
    $group.find('.edit-field').addClass('hidden');
    
    // 5. 表示テキストを表示する
    $group.find('.display-value').removeClass('hidden');
}


// -------------------------------------------------------------
// 【補助関数】ローディング状態の表示/非表示 (簡易版)
// -------------------------------------------------------------
function showLoadingState($group) {
    // 確定ボタンを無効化し、ユーザーの連続クリックを防ぐ
    $group.find('.save-button').prop('disabled', true).text('送信中...');
    $group.find('.cancel-button').prop('disabled', true);
}

function hideLoadingState($group) {
    // ボタンを有効に戻し、テキストを元に戻す
    $group.find('.save-button').prop('disabled', false).text('確定');
    $group.find('.cancel-button').prop('disabled', false);
}


// -------------------------------------------------------------
// 【補助関数】ステータス通知の表示 (簡易版。HTML構造が必要です)
// -------------------------------------------------------------
function showStatusMessage(message, type) {
    const $statusDiv = $('#status-message'); // 画面上部などに用意した通知エリアのID
    if ($statusDiv.length === 0) {
        alert(message); // 通知エリアがない場合はalertで代用
        return;
    }
    
    $statusDiv.text(message)
              .removeClass('success error') // 既存のクラスをリセット
              .addClass(type)               // successまたはerrorクラスを付与
              .slideDown();                 // スライドで表示
    
    // 5秒後に自動で消えるように設定
    setTimeout(() => {
        $statusDiv.slideUp();
    }, 5000);
}

// 名前変更ロジック

    //  (編集モードへ移行)
    $('.edit-button').on('click', function(e) {
        e.preventDefault(); 
        var $group = $(this).closest('.form-item-group');

        // 1. 編集/変更ボタンを非表示にする
        $group.find('.edit-button').addClass('hidden');
        
        // 2. 確定/キャンセルボタンを表示する
        $group.find('.save-button, .cancel-button').removeClass('hidden');
        
        // 3. 表示テキストを非表示にする
        $group.find('.display-value').addClass('hidden');
        
        // 4. 入力フィールドを表示する
        var $editField = $group.find('.edit-field');
        $editField.removeClass('hidden');
        $editField.focus(); // 入力フィールドにフォーカスを当てる
    });


// 【2】キャンセルボタン (表示モードへ戻る)
    $('.cancel-button').on('click', function(e) {
        e.preventDefault();
        
        var $group = $(this).closest('.form-item-group');
        
        // 1. 確定/キャンセルボタンを非表示にする
        $group.find('.save-button, .cancel-button').addClass('hidden');
        
        // 2. 編集/変更ボタンを表示する
        $group.find('.edit-button').removeClass('hidden');
        
        // 3. 入力フィールドを非表示にする
        $group.find('.edit-field').addClass('hidden');
        
        // 4. 表示テキストを表示する
        $group.find('.display-value').removeClass('hidden');
        
        // 5. 【重要】入力フィールドの値を元の値に戻す
        var originalValue = $group.find('.display-value').text().trim();
        $group.find('.edit-field').val(originalValue);
    });
    
// 【3】確定ボタン (データ保存処理)
    $('.save-button').on('click', function(e) {
        e.preventDefault();
        
        var $group = $(this).closest('.form-item-group');
        var $editField = $group.find('.edit-field');
        var newValue = $editField.val();
        
        // ユーザーIDはサーバーから渡される
        var userId = $('#profileContainer').data('user-id');
        
        // UIを一時的にロック/送信中にする
        showLoadingState($group); 
        
        $.ajax({
            url: '/student/api/updateName', // 適切なバックエンドのエンドポイント
            type: 'POST', // または PUT
            contentType: 'application/json',
            data: JSON.stringify({
                userId: userId,
                newName: newValue
            }),
            success: function(response) {
                // サーバーからのレスポンスが成功だった場合
                // 1. 表示テキストの値を更新する
                $group.find('.display-value').text(newValue);
                $('.profile-container .user-header .user-name').text(newValue);
                $('#globalUserName').text(newValue);
                
                // 2. 表示モードに戻す
                exitEditMode($group, newValue); 
                
                // 3. 成功メッセージの表示
                showStatusMessage('氏名を更新しました。', 'success');
            },
            error: function(xhr, status, error) {
                // 失敗メッセージの表示
                showStatusMessage('更新に失敗しました。エラー: ' + error, 'error');
                
                // エディットモードを維持するか、キャンセル扱いにするか選択
                exitEditMode($group, $group.find('.display-value').text().trim()); // 編集前の値で表示に戻す
            },
            complete: function() {
                // UIロックを解除
                hideLoadingState($group);
            }
        });
    });


    
    // ラジオボタンを変更するだけで通知設定の保存を行う

    document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('notificationSettingForm');
    const radioButtons = document.querySelectorAll('input[name="notification_option"]');

    radioButtons.forEach(radio => {
        radio.addEventListener('change', function() {
            // 選択された値を取得
            const selectedValue = this.value; 
            
            // サーバーへ非同期で送信する関数を実行
            saveNotificationSetting(selectedValue);
        });
    });
});

/**
 * 選択された値をバックエンドAPIに非同期で送信する関数
 * @param {string} value - 送信する通知設定の値 ('ALL', 'MENTIONS', 'NONE')
 */
function saveNotificationSetting(value) {
    const apiUrl = '/student/api/notification/update'; // ★★★ バックエンドのAPIエンドポイントを仮定 ★★★
    
    // 送信するデータ（JSON形式）
    const data = {
        notificationStatus: value 
    };

    console.log(`[API CALL] Sending value: ${value}`);

    // Fetch APIを使用して非同期でPOSTリクエストを送信
    fetch(apiUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // Spring Securityなどを利用する場合、CSRFトークンの設定が必要になることが多い
            // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify(data)
    })
    .then(response => {
        if (!response.ok) {
            // ステータスコードが200番台以外の場合
            throw new Error('Network response was not ok. Status: ' + response.status);
        }
        // レスポンスがJSONであればパースする
        return response.json(); 
    })
    .then(data => {
        // 成功時の処理
        console.log('設定がサーバーに保存されました:', data);
        // ★ ユーザーへのフィードバックをここに記述 ★
        // 例: 画面上に「保存しました」という一時メッセージを表示
    })
    .catch(error => {
        // 失敗時の処理
        console.error('設定の保存中にエラーが発生しました:', error);
        // 例: エラーメッセージをユーザーに表示
    });
}
    
});