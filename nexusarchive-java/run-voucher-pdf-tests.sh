#!/bin/bash
# Script to run VoucherPdfGenerator tests

set -e

echo "======================================"
echo "VoucherPdfGenerator Test Runner"
echo "======================================"
echo ""

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Must run from nexusarchive-java directory"
    exit 1
fi

echo "Running VoucherPdfGenerator tests..."
echo ""

# Run the tests
mvn test -Dtest=VoucherPdfGeneratorTest "$@"

echo ""
echo "======================================"
echo "Test execution completed"
echo "======================================"
echo ""
echo "To view coverage report:"
echo "  open target/site/jacoco/index.html"
echo ""
echo "To view test results:"
echo "  open target/surefire-reports/com.nexusarchive.service.pdf.VoucherPdfGeneratorTest.txt"
