---
description: Run the Delivery Acceptance Gatekeeper check
---

# Delivery Gatekeeper Check

This workflow executes the comprehensive delivery acceptance test script. It verifies:
- **Resilient Cold Start**: App starts with DB Down (Health 503).
- **Auto Recovery**: App recovers when DB starts (Health 200).
- **Fail-Fast**: Latency limits (<1.5s).
- **Security**: Gatekeeper functionality during initialization.

**Usage**:
Run this before any delivery, deployment, or after major upgrades.

## Steps

1. **Prerequisites**
   - Ensure local environment is clear (no running java processes on 8080).
   - Ensure `brew services` is available for PostgreSQL management.

2. **Run Verification**
   ```bash
   node scripts/delivery_gatekeeper.cjs
   ```
   
3. **Analyze Result**
   - **PASS**: Proceed with delivery.
   - **FAIL**: Check `logs/gatekeeper_test.log` and fix veto items.
