import time
import requests
import datetime
import uuid
import urllib3
from smartcard.System import readers
from smartcard.util import toHexString
from smartcard.Exceptions import CardConnectionException, NoCardException

# ==========================================
# ★設定エリア
# ==========================================
# SSL警告を無視する設定
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
JAVA_API_URL = "https://127.0.0.1:8080/api/issue/scan"
API_KEY = "MySecretKey_Pi_to_Java_12345"
START_PAGE = 10
# ==========================================

READER_ID = '{:012x}'.format(uuid.getnode())

def get_uid(connection):
    cmd = [0xFF, 0xCA, 0x00, 0x00, 0x00]
    try:
        response, sw1, sw2 = connection.transmit(cmd)
        if sw1 == 0x90:
            return toHexString(response).replace(" ", "")
    except Exception:
        pass
    return None

def read_encrypted_hex(connection):
    """
    Page 10 から 16バイト(4ページ分) を一括で読み込む。
    エラー 6C XX が出た場合は、カードが指定する長さで再試行する。
    """
    print(f"   [読込] Page {START_PAGE} からデータ取得中...")

    # [Class, Ins, P1, P2(Page), Le(Length)]
    # 最初は 16バイト (0x10) を要求してみる
    cmd = [0xFF, 0xB0, 0x00, START_PAGE, 0x10]
    
    try:
        data, sw1, sw2 = connection.transmit(cmd)
        
        # もし「長さが違う(6C)」と言われたら、カードが指定する長さ(sw2)で再送する
        if sw1 == 0x6C:
            print(f"     -> 補正: カード要求長 {sw2}バイト で再試行")
            cmd = [0xFF, 0xB0, 0x00, START_PAGE, sw2]
            data, sw1, sw2 = connection.transmit(cmd)

        if sw1 == 0x90:
            # 読み取り成功
            hex_str = "".join("{:02X}".format(b) for b in data)
            print(f"     -> 成功! データ: {hex_str}")
            
            # データが全て0 (空っぽ) の場合
            if all(b == 0 for b in data):
                print("     -> ※データが全て '00' (未書き込み) です")
                return None
            
            # 必要なのは先頭16バイト（もし多く読めてしまっても16バイト分だけ使う）
            return hex_str[:32] 
        else:
            print(f"     -> 読込失敗: SW1={hex(sw1)}, SW2={hex(sw2)}")
            return None

    except Exception as e:
        print(f"     -> 例外発生: {e}")
        return None

def main_loop():
    print(f"--- PC用 NFC Reader (Fixed for NTAG) ---")
    print(f"Target: {JAVA_API_URL}")
    print("カードをリーダーにかざしてください...")

    last_card_uid = None
    session = requests.Session()
    session.trust_env = False
    session.verify = False  # SSL証明書を検証しない設定

    while True:
        try:
            r = readers()
            if not r:
                print("リーダーが見つかりません...", end="\r")
                time.sleep(1)
                continue

            reader = r[0]
            connection = reader.createConnection()
            
            try:
                connection.connect()
            except (NoCardException, CardConnectionException):
                if last_card_uid is not None:
                    print("\n待機中...")
                last_card_uid = None
                time.sleep(0.5)
                continue

            # UID取得
            card_id = get_uid(connection)
            if not card_id:
                continue

            if card_id == last_card_uid:
                time.sleep(0.5)
                continue
            
            last_card_uid = card_id
            print(f"\n>> 検知! CardID: {card_id}")

            # データ領域読み取り
            encrypted_data = read_encrypted_hex(connection)
            
            if encrypted_data:
                print(f"   データあり: {encrypted_data}")
            else:
                print("   データなし (新規カード扱い)")

            # 送信
            payload = {
                "card_id": card_id,
                "userId": encrypted_data, 
                "timestamp": datetime.datetime.now().isoformat(),
                "reader_id": READER_ID
            }

            headers = {
                "Content-Type": "application/json",
                "Authorization": f"Bearer {API_KEY}"
            }

            try:
                print(f"DEBUG: 送信先URL = {JAVA_API_URL}")
                print("   Web画面へ通知中...", end=" ")
                
                response = session.post(
                    JAVA_API_URL, 
                    json=payload, 
                    headers=headers, 
                    timeout=5,
                    proxies={"http": None, "https": None}, # プロキシ無効化
                    verify=False # SSL検証無効化
                )
                
                if response.status_code == 200:
                    print("OK")
                else:
                    print(f"Error {response.status_code}")
                    print(f"      {response.text}")

            except requests.exceptions.ConnectionError:
                print("通信エラー: Javaサーバー (127.0.0.1:8080) に接続できません")
            except Exception as e:
                print(f"通信エラー: {e}")

            time.sleep(1)

        except Exception as e:
            print(f"\nエラー: {e}")
            time.sleep(1)

if __name__ == "__main__":
    try:
        main_loop()
    except KeyboardInterrupt:
        print("\n終了しました")
