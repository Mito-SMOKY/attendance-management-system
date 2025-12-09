import sys
import json
import time
from smartcard.System import readers
from smartcard.util import toHexString
from smartcard.Exceptions import CardConnectionException, NoCardException

# --- 設定 ---
START_PAGE = 10
SECRET_KEY = b'ThisIsMyKey12345'
WAIT_TIMEOUT = 10  # 秒
# --------------------

def get_uid(connection):
    """カードのUIDを取得するコマンド"""
    cmd = [0xFF, 0xCA, 0x00, 0x00, 0x00]
    response, sw1, sw2 = connection.transmit(cmd)
    if sw1 == 0x90:
        return toHexString(response).replace(" ", "")
    return None

def write_card_process(user_id_str, target_card_id):
    result = {"success": False, "message": "", "card_id": ""}
    
    try:
        # データ準備
        data_bytes = user_id_str.encode('utf-8')
        if len(data_bytes) > 16:
            raise ValueError("データ長すぎ")
        padded_data = data_bytes.ljust(16, b'\x00')
        obfuscated_data = bytes([p ^ k for (p, k) in zip(padded_data, SECRET_KEY)])

        # リーダー接続 & カード待機
        connection = None
        start_time = time.time()
        
        while time.time() - start_time < WAIT_TIMEOUT:
            try:
                r = readers()
                if not r:
                    time.sleep(0.5)
                    continue
                
                reader = r[0]
                connection = reader.createConnection()
                connection.connect()
                break
            except Exception:
                time.sleep(0.5)
        
        if connection is None:
            raise Exception("タイムアウト: カードが見つかりませんでした")

        # UID取得 & すり替えチェック
        current_uid = get_uid(connection)
        if not current_uid:
            raise Exception("UID取得失敗")
            
        # 大文字小文字を無視して比較
        if target_card_id and (current_uid.upper() != target_card_id.upper()):
            raise Exception(f"カード不一致エラー: 最初にかざしたカード(ID:{target_card_id})と異なります。")

        result["card_id"] = current_uid

        # 書き込み
        for i in range(4):
            page = START_PAGE + i
            chunk = obfuscated_data[i*4 : (i+1)*4]
            apdu = [0xFF, 0xD6, 0x00, page, 0x04] + list(chunk)
            resp, sw1, sw2 = connection.transmit(apdu)
            if sw1 != 0x90:
                raise Exception(f"Write Error Page {page}")

        result["success"] = True
        result["message"] = "書き込み成功"

    except Exception as e:
        result["message"] = str(e)
    
    return result

if __name__ == "__main__":
    # 引数1: 書き込むUserID
    # 引数2: ターゲットとなるCardID (すり替え防止用)
    
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "message": "引数不足"}))
        sys.exit(1)

    user_id_arg = sys.argv[1]
    
    # 第2引数があれば取得、なければNone（チェックしない）
    target_card_id_arg = sys.argv[2] if len(sys.argv) > 2 else None

    res = write_card_process(user_id_arg, target_card_id_arg)
    print(json.dumps(res))