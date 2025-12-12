# Refactoring Ingestion to Event-Driven Architecture

## Overview
Refactored the SIP ingestion process from a synchronous monolith to an event-driven, asynchronous architecture using Spring Events. This improves system responsiveness and throughput.

## Architectural Changes

### 1. Event Bus & Flow Split
The ingestion process is now split into three phases:

*   **Sync Phase (`IngestController` -> `IngestService`)**:
    *   Validates business rules.
    *   Saves files to a temporary directory.
    *   Initializes request status (`RECEIVED`).
    *   Publishes `VoucherReceivedEvent`.
    *   Returns HTTP 200 immediately with `requestId`.

*   **Async Phase 1 (`ComplianceListener`)**:
    *   Listens to `VoucherReceivedEvent`.
    *   Updates status to `CHECKING`.
    *   Performs "Four Nature Check" (Authenticity, Integrity, Usability, Safety).
    *   If passed: Updates status to `CHECK_PASSED` and publishes `CheckPassedEvent`.
    *   If failed: Updates status to `FAILED`.

*   **Async Phase 2 (`ProcessingListener`)**:
    *   Listens to `CheckPassedEvent`.
    *   Updates status to `PROCESSING`.
    *   Performs AIP packaging and physical archiving.
    *   **Saves `Archive` entity to Database** (Critical for association).
    *   Triggers `SmartParserService`.
    *   Triggers `AutoAssociationService`.
    *   Updates status to `COMPLETED`.

### 2. Concurrency Control
*   Configured `ThreadPoolTaskExecutor` in `AsyncConfig`.
*   Core Pool Size: CPU Cores * 2.
*   Thread Name Prefix: `IngestAsync-`.

### 3. Status Tracking
*   New Entity: `IngestRequestStatus` (table: `sys_ingest_request_status`).
*   New Endpoint: `GET /api/v1/archive/sip/status/{requestId}`.

## Verification

### 1. Send Ingest Request
```bash
POST /api/v1/archive/sip/ingest
Content-Type: application/json
{ ... SIP JSON ... }
```
**Response**:
```json
{
  "code": 200,
  "data": {
    "requestId": "req-123",
    "status": "RECEIVED",
    "message": "请求已接收，正在后台处理。请通过 /status/req-123 查询进度。"
  }
}
```

### 2. Poll Status
```bash
GET /api/v1/archive/sip/status/req-123
```
**Response (In Progress)**:
```json
{
  "code": 200,
  "data": {
    "requestId": "req-123",
    "status": "CHECKING",
    "message": "正在进行四性检测..."
  }
}
```
**Response (Completed)**:
```json
{
  "code": 200,
  "data": {
    "requestId": "req-123",
    "status": "COMPLETED",
    "message": "归档完成，已存储至: /data/archives/..."
  }
}
```

## Code Locations
*   **Events**: `com.nexusarchive.event`
*   **Listeners**: `com.nexusarchive.listener`
*   **Config**: `com.nexusarchive.config.AsyncConfig`
*   **Entity**: `com.nexusarchive.entity.IngestRequestStatus`
