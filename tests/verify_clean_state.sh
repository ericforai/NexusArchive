#!/bin/bash
set -e

DB_NAME="nexusarchive"
DB_USER="postgres"

# Check if collection_batch has data
COUNT=$(docker exec nexus-db psql -U ${DB_USER} -d ${DB_NAME} -tAc "SELECT COUNT(*) FROM collection_batch" 2>/dev/null || echo "0")

if [ "$COUNT" -ne "0" ]; then
  echo "❌ Database is NOT clean. 'collection_batch' has $COUNT rows."
  exit 1
fi

echo "✅ Database is clean."
exit 0
