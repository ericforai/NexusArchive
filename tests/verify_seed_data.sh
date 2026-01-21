#!/bin/bash
set -e

echo "🧹 Cleaning up..."
cat tests/cleanup_e2e.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive > /dev/null

echo "🌱 Seeding test data..."
cat tests/seed_test_data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive > /dev/null

echo "🔍 Verifying data..."
COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM collection_batch WHERE batch_no = 'E2E-BATCH-001'")

if [ "$COUNT" -eq "1" ]; then
    echo "✅ Seed data loaded successfully."
    exit 0
else
    echo "❌ Seed data failed to load. Count: $COUNT"
    exit 1
fi
