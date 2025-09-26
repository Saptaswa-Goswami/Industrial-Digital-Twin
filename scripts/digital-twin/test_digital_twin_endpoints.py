import requests
import json
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8082"
HEADERS = {"Content-Type": "application/json"}

def print_request_info(method, url, headers=None):
    """Print information about the request being made"""
    print("="*80)
    print(f"REQUEST: {method} {url}")
    if headers:
        print(f"HEADERS: {headers}")
    print("-" * 80)

def print_response_info(response):
    """Print information about the response received"""
    print(f"RESPONSE STATUS: {response.status_code}")
    print(f"RESPONSE HEADERS: {dict(response.headers)}")
    
    try:
        response_json = response.json()
        print(f"RESPONSE BODY (JSON): {json.dumps(response_json, indent=2)}")
    except:
        print(f"RESPONSE BODY (TEXT): {response.text}")
    print("="*80)
    print()

def test_get_all_states():
    """Test GET /api/digital-twin/states endpoint"""
    url = f"{BASE_URL}/api/digital-twin/states"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_get_single_state(machine_id="RECP_001"):
    """Test GET /api/digital-twin/state/{machineId} endpoint"""
    url = f"{BASE_URL}/api/digital-twin/state/{machine_id}"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_delete_state(machine_id="RECP_001"):
    """Test DELETE /api/digital-twin/state/{machineId} endpoint"""
    url = f"{BASE_URL}/api/digital-twin/state/{machine_id}"
    print_request_info("DELETE", url, HEADERS)
    
    response = requests.delete(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_health_endpoint():
    """Test GET /actuator/health endpoint"""
    url = f"{BASE_URL}/actuator/health"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_info_endpoint():
    """Test GET /actuator/info endpoint"""
    url = f"{BASE_URL}/actuator/info"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_metrics_endpoint():
    """Test GET /actuator/metrics endpoint"""
    url = f"{BASE_URL}/actuator/metrics"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def test_nonexistent_machine():
    """Test endpoints with a non-existent machine ID"""
    machine_id = "NONEXISTENT_001"
    
    # Test GET for non-existent machine
    url = f"{BASE_URL}/api/digital-twin/state/{machine_id}"
    print_request_info("GET", url, HEADERS)
    
    response = requests.get(url, headers=HEADERS)
    print_response_info(response)
    
    # Test DELETE for non-existent machine
    url = f"{BASE_URL}/api/digital-twin/state/{machine_id}"
    print_request_info("DELETE", url, HEADERS)
    
    response = requests.delete(url, headers=HEADERS)
    print_response_info(response)
    
    return response

def main():
    """Main function to test all endpoints"""
    print("Testing Digital Twin Service Endpoints")
    print(f"Base URL: {BASE_URL}")
    print(f"Test Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("\n" + "="*80)
    print("DIGITAL TWIN SERVICE ENDPOINT TESTING")
    print("="*80)
    
    # Test health endpoint first to ensure service is running
    print("\n1. Testing Health Endpoint:")
    test_health_endpoint()
    
    # Test info endpoint
    print("\n2. Testing Info Endpoint:")
    test_info_endpoint()
    
    # Test metrics endpoint
    print("\n3. Testing Metrics Endpoint:")
    test_metrics_endpoint()
    
    # Test getting all states
    print("\n4. Testing GET All States Endpoint:")
    test_get_all_states()
    
    # Test getting single state
    print("\n5. Testing GET Single State Endpoint:")
    test_get_single_state()
    
    # Test deleting state
    print("\n6. Testing DELETE State Endpoint:")
    test_delete_state()
    
    # Test getting the same state again after deletion
    print("\n7. Testing GET Single State After Deletion:")
    test_get_single_state()
    
    # Test with non-existent machine
    print("\n8. Testing Endpoints with Non-existent Machine:")
    test_nonexistent_machine()
    
    print("\nTesting completed successfully!")

if __name__ == "__main__":
    main()