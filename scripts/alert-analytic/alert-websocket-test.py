import json
import time
import threading
import websocket
from datetime import datetime

WS_URL = "ws://localhost:8083/ws/alert-updates"

def timestamp():
    """Return current timestamp as a formatted string."""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def on_open(ws):
    print(f"[{timestamp()}] Connected to alert updates WebSocket at {WS_URL}")
    print(f"[{timestamp()}] Listening for real-time alerts...")

def on_message(ws, message):
    try:
        parsed_data = json.loads(message)
        print(f"[{timestamp()}] Alert Update:", json.dumps(parsed_data, indent=2))
    except Exception:
        print(f"[{timestamp()}] Alert Raw Message:", message)

def on_error(ws, error):
    print(f"[{timestamp()}] Alert WebSocket error:", error)

def on_close(ws, close_status_code, close_msg):
    print(f"[{timestamp()}] Alert WebSocket connection closed")

def run():
    start_dt = datetime.now()
    print(f"=== Test started at {start_dt.strftime('%Y-%m-%d %H:%M:%S')} ===")

    ws = websocket.WebSocketApp(
        WS_URL,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close,
    )

    # Run WebSocket in background thread
    wst = threading.Thread(target=ws.run_forever)
    wst.daemon = True
    wst.start()

    print(f"[{timestamp()}] Alert WebSocket client connected. Testing will run for 1 minute...")
    print("Press Ctrl+C to stop the test.")

    try:
        time.sleep(1 * 60)  # 1 minute
    except KeyboardInterrupt:
        print(f"[{timestamp()}] Interrupted by user. Closing connection...")
    finally:
        print(f"[{timestamp()}] Test duration completed. Closing connection...")
        ws.close()
        end_dt = datetime.now()
        duration_seconds = int((end_dt - start_dt).total_seconds())
        minutes, seconds = divmod(duration_seconds, 60)
        print(f"=== Test ended at {end_dt.strftime('%Y-%m-%d %H:%M:%S')} ===")
        print(f"=== Total test duration: {minutes} min {seconds} sec ===")

if __name__ == "__main__":
    print(f"[{timestamp()}] Testing Alert WebSocket connection of Alert-Analysis Service...")
    run()

