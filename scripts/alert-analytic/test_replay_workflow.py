#!/usr/bin/env python3
"""
Automated testing script for Alert Analytics Service Replay functionality
Tests the complete replay workflow:
1. Get time boundaries
2. Start replay with long time range and multiplier 5.0
3. Test status, pause, status, resume, status, stop, status sequence
"""

import requests
import json
import time
from datetime import datetime, timedelta

# Configuration
BASE_URL = "http://localhost:8083"
TIMEOUT = 10  # seconds
MACHINE_ID = "CONV_001"

def print_header(header):
    """Print a formatted header"""
    print(f"\n{'='*80}")
    print(f"{header}")
    print(f"{'='*80}")

def print_response(operation, response, expected_status=200):
    """Print formatted response information"""
    status_match = response.status_code == expected_status
    status_icon = "[PASS]" if status_match else "[FAIL]"
    
    print(f"{status_icon} {operation}")
    print(f"   Status Code: {response.status_code} (Expected: {expected_status})")
    
    if response.content:
        try:
            content = response.json()
            print(f"   Response: {json.dumps(content, indent=2, default=str)}")
        except:
            print(f"   Response: {response.text}")
    else:
        print("   Response: <empty>")
    
    if not status_match:
        print(f"   ERROR: Unexpected status code!")
    
    return status_match

def get_time_boundaries():
    """Get the min and max timestamps for historical data"""
    print_header("STEP 1: Getting Time Boundaries")
    
    try:
        response = requests.get(
            f"{BASE_URL}/api/replay/{MACHINE_ID}/time-boundaries", 
            timeout=TIMEOUT
        )
        success = print_response("GET /api/replay/{machineId}/time-boundaries", response)
        
        if success and response.status_code == 200:
            data = response.json()
            return data.get('minTime'), data.get('maxTime')
        return None, None
    except Exception as e:
        print(f"[ERROR] Failed to get time boundaries: {str(e)}")
        return None, None

def start_replay(min_time, max_time):
    """Start a replay with a long time range and multiplier 5.0"""
    print_header("STEP 2: Starting Replay")
    
    # Use a time range that covers most of the available data
    # Parse the timestamps and adjust slightly to ensure data exists
    try:
        # Convert string timestamps to datetime objects
        min_dt = datetime.fromisoformat(min_time.replace('Z', '+00:00'))
        max_dt = datetime.fromisoformat(max_time.replace('Z', '+00:00'))
        
        # Adjust the range to ensure we have data
        # Use a smaller range toward the end to ensure we're using the most recent data
        range_duration = max_dt - min_dt
        start_time = max_dt - range_duration * 0.5  # Start 50% from the end (more recent data)
        end_time = max_dt - range_duration * 0.1    # End 10% from the end (most recent data)
        
        # Format as ISO strings
        start_time_str = start_time.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
        end_time_str = end_time.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
        
        print(f"   Using time range: {start_time_str} to {end_time_str}")
        
        response = requests.post(
            f"{BASE_URL}/api/replay/{MACHINE_ID}/start",
            params={
                'startTime': start_time_str,
                'endTime': end_time_str,
                'speedMultiplier': 5.0
            },
            timeout=TIMEOUT
        )
        
        success = print_response("POST /api/replay/{machineId}/start", response, 200)
        
        if success and response.status_code == 200:
            replay_id = response.text.strip()
            print(f"   Generated Replay ID: {replay_id}")
            return replay_id
        return None
    except Exception as e:
        print(f"[ERROR] Failed to start replay: {str(e)}")
        return None

def test_replay_operation(replay_id, operation, endpoint_suffix, method="GET", expected_status=200):
    """Test a specific replay operation"""
    url = f"{BASE_URL}/api/replay/{replay_id}/{endpoint_suffix}"
    
    try:
        if method.upper() == "POST":
            response = requests.post(url, timeout=TIMEOUT)
        else:
            response = requests.get(url, timeout=TIMEOUT)
            
        success = print_response(f"{method} /api/replay/{replay_id}/{endpoint_suffix}", response, expected_status)
        return success
    except Exception as e:
        print(f"[ERROR] Failed to {operation}: {str(e)}")
        return False

def test_status(replay_id, step_number):
    """Test the status endpoint"""
    print_header(f"STEP {step_number}: Checking Replay Status")
    time.sleep(2)  # Small delay before checking status
    return test_replay_operation(replay_id, "check status", "status", "GET", 200)

def test_pause(replay_id):
    """Test the pause endpoint"""
    print_header("STEP 4: Pausing Replay")
    time.sleep(1)  # Small delay before pausing
    return test_replay_operation(replay_id, "pause replay", "pause", "POST", 200)

def test_resume(replay_id):
    """Test the resume endpoint"""
    print_header("STEP 6: Resuming Replay")
    time.sleep(1)  # Small delay before resuming
    return test_replay_operation(replay_id, "resume replay", "resume", "POST", 200)

def test_stop(replay_id):
    """Test the stop endpoint"""
    print_header("STEP 8: Stopping Replay")
    time.sleep(1)  # Small delay before stopping
    return test_replay_operation(replay_id, "stop replay", "stop", "POST", 200)

def main():
    """Main test workflow"""
    print_header("ALERT ANALYTICS SERVICE - REPLAY WORKFLOW TEST")
    print(f"Testing endpoints at: {BASE_URL}")
    print(f"Target machine ID: {MACHINE_ID}")
    print(f"Start time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Step 1: Get time boundaries
    min_time, max_time = get_time_boundaries()
    if not min_time or not max_time:
        print("[FAIL] Could not retrieve time boundaries")
        return
    
    # Time boundaries already displayed in the API response above
    
    # Step 2: Start replay
    replay_id = start_replay(min_time, max_time)
    if not replay_id:
        print("[FAIL] Could not start replay")
        return
    
    # Step 3: Check initial status
    if not test_status(replay_id, 3):
        print("[FAIL] Initial status check failed")
        return
    
    # Step 4: Pause replay
    if not test_pause(replay_id):
        print("[FAIL] Pause operation failed")
        return
    
    # Step 5: Check status after pause
    if not test_status(replay_id, 5):
        print("[FAIL] Status check after pause failed")
        return
    
    # Step 6: Resume replay
    if not test_resume(replay_id):
        print("[FAIL] Resume operation failed")
        return
    
    # Step 7: Check status after resume
    if not test_status(replay_id, 7):
        print("[FAIL] Status check after resume failed")
        return
    
    # Step 8: Stop replay
    if not test_stop(replay_id):
        print("[FAIL] Stop operation failed")
        return
    
    # Step 9: Check status after stop (should fail with 404)
    print_header("STEP 9: Checking Status After Stop (Should Return 404)")
    time.sleep(1)  # Small delay before final status check
    success = test_replay_operation(replay_id, "check status after stop", "status", "GET", 404)
    
    if success:
        print("\n[PASS] Final status check correctly returned 404 (Not Found)")
    else:
        print("\n[FAIL] Final status check did not return expected 404 status")
    
    print_header("TEST COMPLETED")
    print(f"End time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == "__main__":
    main()