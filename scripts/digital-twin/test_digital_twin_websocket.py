import json
import time
import threading
import websocket
import sys
from datetime import datetime

# Configuration
WS_URL = "ws://localhost:8082/ws/machine-updates"

def timestamp():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

class WebSocketTester:
    def __init__(self, url, duration=30):
        self.url = url
        self.duration = duration
        self.ws = None
        self.thread = None
        self.message_count = 0
        self.stop_event = threading.Event()

    def on_open(self, ws):
        print(f"[{timestamp()}] ✅ Connected to {self.url}")
        print(f"[{timestamp()}] Listening for messages...")

    def on_message(self, ws, message):
        self.message_count += 1
        try:
            parsed = json.loads(message)
            print(f"[{timestamp()}] Message {self.message_count} (JSON):\n{json.dumps(parsed, indent=2)}")
        except json.JSONDecodeError:
            print(f"[{timestamp()}] Message {self.message_count} (Text): {message}")

    def on_error(self, ws, error):
        print(f"[{timestamp()}] WebSocket error: {error}")

    def on_close(self, ws, close_status_code, close_msg):
        print(f"[{timestamp()}] WebSocket connection closed")

    def start(self):
        self.ws = websocket.WebSocketApp(
            self.url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        self.thread = threading.Thread(target=self.ws.run_forever)
        self.thread.daemon = True
        self.thread.start()

        print(f"[{timestamp()}] WebSocket client started. Running for {self.duration} seconds...")
        start_time = time.time()
        try:
            while time.time() - start_time < self.duration and not self.stop_event.is_set():
                time.sleep(1)
        except KeyboardInterrupt:
            print(f"[{timestamp()}] ⚠️ Interrupted by user")
        finally:
            self.stop()

    def stop(self):
        self.stop_event.set()
        if self.ws:
            self.ws.close()
        if self.thread:
            self.thread.join(timeout=2)
        print(f"[{timestamp()}] Test completed. Total messages received: {self.message_count}")

def main():
    duration = 30  # default duration in seconds
    if len(sys.argv) > 1:
        try:
            duration = int(sys.argv[1])
        except ValueError:
            print(f"[!] Invalid duration argument. Using default {duration}s")

    print(f"[{timestamp()}] Starting WebSocket test for {WS_URL} ({duration}s)")
    tester = WebSocketTester(WS_URL, duration=duration)
    tester.start()

if __name__ == "__main__":
    main()
