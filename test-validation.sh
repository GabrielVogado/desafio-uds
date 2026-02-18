#!/bin/bash

# Desafio UDS - Complete Test & Validation Script
# This script validates all changes and confirms the application is working correctly

set -e  # Exit on error

echo "=================================================="
echo "Desafio UDS - Complete Validation Script"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080/api"
TIMEOUT=30
TOTAL_TESTS=0
PASSED_TESTS=0

# Helper functions
log_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}[Test $TOTAL_TESTS]${NC} $1"
}

log_pass() {
    PASSED_TESTS=$((PASSED_TESTS + 1))
    echo -e "${GREEN}✓ PASS${NC}: $1"
}

log_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
}

log_info() {
    echo -e "${YELLOW}ℹ INFO${NC}: $1"
}

# Check if server is running
check_server() {
    echo -e "${BLUE}Checking server connectivity...${NC}"

    if ! timeout $TIMEOUT curl -s "$BASE_URL/auth/health" > /dev/null 2>&1; then
        echo -e "${RED}ERROR: Cannot connect to $BASE_URL${NC}"
        echo -e "${YELLOW}Make sure the application is running with: docker-compose up --build${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Server is running${NC}"
    echo ""
}

# Test 1: Health Check
test_health_check() {
    log_test "Health Check (GET /auth/health)"

    RESPONSE=$(curl -s -X GET "$BASE_URL/auth/health")

    if echo "$RESPONSE" | grep -q "running"; then
        log_pass "Health check returned success"
    else
        log_fail "Health check failed"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Test 2: User Registration
test_user_registration() {
    log_test "User Registration (POST /auth/register)"

    RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"testuser_$(date +%s)\",
            \"email\": \"test_$(date +%s)@example.com\",
            \"password\": \"TestPassword123!\"
        }")

    if echo "$RESPONSE" | grep -q "token"; then
        TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        USERNAME=$(echo "$RESPONSE" | grep -o '"username":"[^"]*' | cut -d'"' -f4)
        log_pass "User registered successfully (username: $USERNAME)"
        echo "$TOKEN"  # Return token
    else
        log_fail "User registration failed"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Test 3: User Login
test_user_login() {
    log_test "User Login (POST /auth/login)"

    # First register a user
    REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"logintest_$(date +%s)\",
            \"email\": \"logintest_$(date +%s)@example.com\",
            \"password\": \"LoginTest123!\"
        }")

    USERNAME=$(echo "$REGISTER_RESPONSE" | grep -o '"username":"[^"]*' | cut -d'"' -f4)

    # Now login
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$USERNAME\",
            \"password\": \"LoginTest123!\"
        }")

    if echo "$LOGIN_RESPONSE" | grep -q "token"; then
        TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        log_pass "User login successful"
        echo "$TOKEN"  # Return token
    else
        log_fail "User login failed"
        echo "Response: $LOGIN_RESPONSE"
        return 1
    fi
}

# Test 4: Invalid Login
test_invalid_login() {
    log_test "Invalid Login (POST /auth/login with wrong password)"

    RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"nonexistent\",
            \"password\": \"wrongpassword\"
        }")

    if echo "$RESPONSE" | grep -q "401\|Unauthorized"; then
        log_pass "Invalid login correctly rejected"
    else
        # Check if we get an error
        if echo "$RESPONSE" | grep -q "credentials\|Unauthorized"; then
            log_pass "Invalid login correctly rejected"
        else
            log_fail "Invalid login not properly rejected"
            echo "Response: $RESPONSE"
            return 1
        fi
    fi
}

# Test 5: Create Document (requires auth)
test_create_document() {
    local TOKEN=$1
    log_test "Create Document (POST /documents)"

    RESPONSE=$(curl -s -X POST "$BASE_URL/documents" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"title\": \"Test Document $(date +%s)\",
            \"description\": \"This is a test document\",
            \"tags\": [\"test\", \"validation\"]
        }")

    if echo "$RESPONSE" | grep -q '"id"'; then
        DOC_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
        log_pass "Document created successfully (ID: $DOC_ID)"
        echo "$DOC_ID"  # Return document ID
    else
        log_fail "Document creation failed"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Test 6: List Documents (requires auth)
test_list_documents() {
    local TOKEN=$1
    log_test "List Documents (GET /documents)"

    RESPONSE=$(curl -s -X GET "$BASE_URL/documents?page=0&size=10" \
        -H "Authorization: Bearer $TOKEN")

    if echo "$RESPONSE" | grep -q "content"; then
        log_pass "Documents listed successfully"
    else
        log_fail "Document listing failed"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Test 7: Get Document Details (requires auth)
test_get_document() {
    local TOKEN=$1
    local DOC_ID=$2
    log_test "Get Document Details (GET /documents/$DOC_ID)"

    RESPONSE=$(curl -s -X GET "$BASE_URL/documents/$DOC_ID" \
        -H "Authorization: Bearer $TOKEN")

    if echo "$RESPONSE" | grep -q '"id"'; then
        log_pass "Document details retrieved successfully"
    else
        log_fail "Failed to retrieve document details"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Test 8: Unauthorized Access (no token)
test_unauthorized_access() {
    log_test "Unauthorized Access (GET /documents without token)"

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/documents")

    if [ "$HTTP_CODE" == "401" ]; then
        log_pass "Unauthorized access correctly rejected (HTTP 401)"
    else
        log_fail "Unauthorized access not properly rejected (HTTP $HTTP_CODE)"
        return 1
    fi
}

# Test 9: JWT Token Validation
test_jwt_validation() {
    log_test "JWT Token Validation (GET /documents with invalid token)"

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/documents" \
        -H "Authorization: Bearer invalid_token_12345")

    if [ "$HTTP_CODE" == "401" ]; then
        log_pass "Invalid token correctly rejected (HTTP 401)"
    else
        log_fail "Invalid token not properly rejected (HTTP $HTTP_CODE)"
        return 1
    fi
}

# Test 10: CORS Configuration
test_cors() {
    log_test "CORS Configuration (OPTIONS request)"

    RESPONSE=$(curl -s -X OPTIONS "$BASE_URL/documents" \
        -H "Origin: http://localhost:4200" \
        -H "Access-Control-Request-Method: GET")

    if [ -n "$RESPONSE" ]; then
        log_pass "CORS headers configured"
    else
        log_fail "CORS not properly configured"
        return 1
    fi
}

# Main execution
main() {
    echo ""

    # Check server first
    check_server

    # Run all tests
    echo -e "${BLUE}=== Running Tests ===${NC}"
    echo ""

    # Basic tests (no auth)
    test_health_check || true
    test_invalid_login || true
    test_unauthorized_access || true
    test_cors || true

    # Auth tests
    TOKEN=$(test_user_registration) || true
    test_user_login || true
    test_jwt_validation || true

    # Protected endpoint tests
    if [ -n "$TOKEN" ]; then
        DOC_ID=$(test_create_document "$TOKEN") || true
        test_list_documents "$TOKEN" || true

        if [ -n "$DOC_ID" ]; then
            test_get_document "$TOKEN" "$DOC_ID" || true
        fi
    fi

    # Summary
    echo ""
    echo -e "${BLUE}=== Test Summary ===${NC}"
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"

    FAILED=$((TOTAL_TESTS - PASSED_TESTS))
    if [ $FAILED -eq 0 ]; then
        echo -e "Failed: ${GREEN}0${NC}"
        echo ""
        echo -e "${GREEN}✓ All tests passed!${NC}"
        return 0
    else
        echo -e "Failed: ${RED}$FAILED${NC}"
        echo ""
        echo -e "${RED}✗ Some tests failed${NC}"
        return 1
    fi
}

# Run main
main

