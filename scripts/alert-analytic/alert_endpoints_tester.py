#!/usr/bin/env python3
"""
Python script to test all Alert Analytics Service endpoints
Prints the curl command, HTTP code returned, and the response for each endpoint
"""
import requests
import json
import time

# Configuration
BASE_URL = "http://localhost:8083/api"
HEADERS = {"Content-Type": "application/json"}

def print_test_header(endpoint, description):
    """Print a header for each test"""
    print("="*80)
    print(f"Testing: {endpoint}")
    print(f"Description: {description}")
    print("-"*80)

def make_request(method, endpoint, params=None, data=None):
    """Make an HTTP request and return response details"""
    url = f"{BASE_URL}{endpoint}"
    
    # Build curl command
    curl_cmd = f"curl -X {method} '{url}'"
    
    if params:
        param_str = "&".join([f"{k}={v}" for k, v in params.items()])
        curl_cmd += f" -G --data-urlencode '{param_str}'"
    
    if data:
        curl_cmd += f" -d '{json.dumps(data)}'"
    
    print(f"Curl Command: {curl_cmd}")
    
    try:
        response = requests.request(method, url, headers=HEADERS, params=params, json=data)
        print(f"HTTP Status Code: {response.status_code}")
        
        # Try to pretty print JSON response, otherwise print as text
        try:
            response_json = response.json()
            print(f"Response: {json.dumps(response_json, indent=2)}")
        except:
            print(f"Response: {response.text}")
        
        print("-" * 80)
        return response.status_code, response.text if not response.headers.get('content-type', '').startswith('application/json') else response.json()
    
    except requests.exceptions.ConnectionError:
        print("ERROR: Connection refused. Is the Alert Analytics Service running on localhost:8083?")
        print("-" * 80)
        return None, None
    except Exception as e:
        print(f"ERROR: {str(e)}")
        print("-" * 80)
        return None, None

def test_alert_management_endpoints():
    """Test all alert management endpoints"""
    
    print("ALERT ANALYTICS SERVICE - ENDPOINT TESTING")
    print("This script tests all endpoints and prints curl command, HTTP code, and response")
    print("\n")
    
    # Test 1: Get all alerts
    print_test_header("/alerts", "Retrieve all alerts")
    status, response = make_request("GET", "/alerts")
    
    # Test 2: Get alerts with severity filter
    print_test_header("/alerts?severity=CRITICAL", "Retrieve alerts filtered by CRITICAL severity")
    status, response = make_request("GET", "/alerts", params={"severity": "CRITICAL"})
    
    # Test 3: Get alerts with status filter
    print_test_header("/alerts?status=NEW", "Retrieve alerts filtered by NEW status")
    status, response = make_request("GET", "/alerts", params={"status": "NEW"})
    
    # Test 4: Get alerts with machine filter
    print_test_header("/alerts?machineId=CONV_001", "Retrieve alerts for specific machine")
    status, response = make_request("GET", "/alerts", params={"machineId": "CONV_001"})
    
    # Test 5: Get alerts with multiple filters
    print_test_header("/alerts?severity=CRITICAL&status=NEW", "Retrieve alerts with multiple filters")
    status, response = make_request("GET", "/alerts", params={"severity": "CRITICAL", "status": "NEW"})
    
    # Test 6: Get alerts by machine ID path parameter
    print_test_header("/alerts/machine/CONV_001", "Retrieve alerts by machine ID path parameter")
    status, response = make_request("GET", "/alerts/machine/CONV_001")
    
    # Get a sample alert ID for testing individual alert operations
    print_test_header("/alerts (to get a sample alert ID)", "Retrieve alerts to get a sample ID for testing")
    status, response = make_request("GET", "/alerts")
    
    sample_alert_id = None
    if status and response:
        try:
            # Parse the response to get a sample alert ID
            if isinstance(response, str):
                response_data = json.loads(response)
            else:
                response_data = response
                
            if isinstance(response_data, list) and len(response_data) > 0:
                sample_alert_id = response_data[0].get('alertId')
        except:
            pass
    
    if sample_alert_id:
        # Test 7: Get specific alert by ID
        print_test_header(f"/alerts/{sample_alert_id}", "Retrieve specific alert by ID")
        status, response = make_request("GET", f"/alerts/{sample_alert_id}")
        
        # Test 8: Acknowledge an alert (only if status is NEW)
        print_test_header(f"/alerts/acknowledge/{sample_alert_id}", "Acknowledge an alert")
        status, response = make_request("POST", f"/alerts/acknowledge/{sample_alert_id}")
        
        # Test 9: Resolve an alert
        print_test_header(f"/alerts/resolve/{sample_alert_id}", "Resolve an alert")
        status, response = make_request("POST", f"/alerts/resolve/{sample_alert_id}")
        
        # Test 10: Delete an alert
        print_test_header(f"/alerts/{sample_alert_id}", "Delete an alert")
        status, response = make_request("DELETE", f"/alerts/{sample_alert_id}")
    else:
        print("Could not get a sample alert ID for individual alert tests")
        print("-" * 80)
    
    # Test 11: Get all performance metrics
    print_test_header("/analytics/metrics", "Retrieve all performance metrics")
    status, response = make_request("GET", "/analytics/metrics")
    
    # Test 12: Get performance metrics for a specific machine
    print_test_header("/analytics/metrics/CONV_001", "Retrieve performance metrics for specific machine")
    status, response = make_request("GET", "/analytics/metrics/CONV_001")
    
    # Test 13: Get historical performance metrics
    print_test_header("/analytics/metrics/CONV_001/historical", "Retrieve historical performance metrics")
    status, response = make_request("GET", "/analytics/metrics/CONV_001/historical", 
                                   params={"fromTime": str(int(time.time()) - 3600), 
                                          "toTime": str(int(time.time()))})
    
    # Test 14: Get all trends
    print_test_header("/analytics/trends", "Retrieve all trends")
    status, response = make_request("GET", "/analytics/trends")
    
    # Test 15: Get trends for a specific machine
    print_test_header("/analytics/trends/CONV_001", "Retrieve trends for specific machine")
    status, response = make_request("GET", "/analytics/trends/CONV_001")
    
    # Test 16: Get all reports
    print_test_header("/reports", "Retrieve all maintenance reports")
    status, response = make_request("GET", "/reports")
    
    # Test 17: Get reports by type
    print_test_header("/reports/type/PERFORMANCE", "Retrieve reports by type")
    status, response = make_request("GET", "/reports/type/PERFORMANCE")
    
    # Test 18: Generate a report
    print_test_header("/reports/generate", "Generate a maintenance report")
    status, response = make_request("POST", "/reports/generate", 
                                   params={"machineId": "CONV_001", "reportType": "PERFORMANCE"})
    
    # Test 19: Reload configuration
    print_test_header("/config/reload", "Reload configuration")
    status, response = make_request("POST", "/config/reload")
    
    print("\n" + "="*80)
    print("ENDPOINT TESTING COMPLETED")
    print("="*80)

if __name__ == "__main__":
    test_alert_management_endpoints()