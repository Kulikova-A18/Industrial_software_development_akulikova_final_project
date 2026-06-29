#!/bin/bash

# cd api-gateway
# mvn clean compile

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR/api-gateway" || exit 1

mkdir -p "$PROJECT_DIR/target/test-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_DIR="$PROJECT_DIR/target/test-reports/test-report_${TIMESTAMP}"
mkdir -p "$REPORT_DIR"

if mvn clean test \
    -Dmaven.test.failure.ignore=true \
    -Dtest=GatewayConfigTest \
    -Dspring.profiles.active=test \
    2>&1 | tee "$REPORT_DIR/test-output.log"; then
    TEST_EXIT_CODE=${PIPESTATUS[0]}
else
    TEST_EXIT_CODE=${PIPESTATUS[0]}
fi

if [ -d "target/surefire-reports" ]; then
    cp -r target/surefire-reports/* "$REPORT_DIR/" 2>/dev/null || true
fi
