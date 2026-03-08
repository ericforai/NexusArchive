# PDF Signature Verification Service Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a production-usable PDF signature verification service that normalizes results to `VALID`, `INVALID`, or `UNKNOWN`.

**Architecture:** Add a dedicated PDF signature verification service under the existing signature module instead of burying the logic inside `Sm2SignatureService`. The service will read PDF bytes, inspect PDF signature dictionaries with PDFBox, verify CMS payloads with BouncyCastle, and normalize library-specific outcomes into a stable application result for later workflow integration.

**Tech Stack:** Java 17, Spring Boot, PDFBox 2.0.30, BouncyCastle, JUnit 5, AssertJ

---

### Task 1: Define Stable Verification Result Types

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/PdfSignatureVerificationStatus.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/PdfSignatureVerificationResult.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/README.md`

**Step 1: Add the status enum**

Create an enum with `VALID`, `INVALID`, `UNKNOWN` and a stable lowercase code for later workflow/API mapping.

**Step 2: Add the result object**

Create a DTO with:
- `status`
- `signed`
- `message`
- `signerName`
- `certificateSubject`
- `certSerialNumber`
- `algorithm`
- `signTime`
- `verifiedAt`
- `signatureCount`

Add static factories for `valid(...)`, `invalid(...)`, and `unknown(...)`.

**Step 3: Update the dto README**

List the new result types in `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/README.md`.

### Task 2: Write the Failing Tests First

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/service/signature/PdfSignatureVerificationServiceTest.java`
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/service/README.md`

**Step 1: Write the unsigned PDF test**

Add a test that builds a minimal unsigned PDF in memory and asserts:
- status is `UNKNOWN`
- `signed` is `false`
- message explains that no signature was found

**Step 2: Run the single test to verify RED**

Run:

```bash
mvn -f nexusarchive-java/pom.xml test -Dtest=PdfSignatureVerificationServiceTest#verify_unsigned_pdf_returns_unknown
```

Expected: FAIL because the service does not exist yet.

**Step 3: Write the malformed PDF test**

Assert malformed bytes return:
- status `UNKNOWN`
- `signed == null`
- parse-failure message

**Step 4: Write the valid signed PDF test**

Generate a real signed PDF in the test using PDFBox + BouncyCastle and assert:
- status `VALID`
- `signed` is `true`
- signer metadata is populated

**Step 5: Write the tampered signed PDF test**

Append trailing bytes to a valid signed PDF and assert:
- status `INVALID`
- `signed` is `true`
- message explains the document is no longer fully covered by the signature

### Task 3: Implement the PDF Verification Service

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/PdfSignatureVerificationService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/PdfBoxPdfSignatureVerificationService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/README.md`

**Step 1: Add the service interface**

Expose `verify(InputStream pdfStream)` and `verify(byte[] pdfBytes)` so higher-level workflow code can call it without controller coupling.

**Step 2: Implement minimal parsing logic**

Read the bytes once, load the PDF with PDFBox, and detect whether any signature dictionaries are present.

**Step 3: Implement CMS verification**

For each signature:
- ensure the signature covers the whole document
- extract CMS contents
- load signer certificate
- validate certificate dates
- verify the signer payload with BouncyCastle

Map outcomes to:
- `VALID`: signature exists and verification succeeds
- `INVALID`: signature exists and explicit verification fails
- `UNKNOWN`: no signature, malformed PDF, unsupported structure, or parser failure

**Step 4: Run the focused test suite to verify GREEN**

Run:

```bash
mvn -f nexusarchive-java/pom.xml test -Dtest=PdfSignatureVerificationServiceTest
```

Expected: PASS

**Step 5: Refactor carefully**

Clean up helper methods only after the focused tests are green. Keep the public service contract minimal.

### Task 4: Final Verification and Workpad Sync

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/README.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/README.md`
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/service/README.md`

**Step 1: Run final verification**

Run:

```bash
mvn -f nexusarchive-java/pom.xml test -Dtest=PdfSignatureVerificationServiceTest,DigitalSignatureServiceTest
```

Expected: PASS

**Step 2: Check diff**

Run:

```bash
git status --short
git diff -- docs/plans/2026-03-06-pdf-signature-verification-service.md \
  nexusarchive-java/src/main/java/com/nexusarchive/dto/signature \
  nexusarchive-java/src/main/java/com/nexusarchive/service/signature \
  nexusarchive-java/src/test/java/com/nexusarchive/service
```

Expected: only the planned files are changed.

**Step 3: Update the Linear workpad**

Mark implementation and validation as complete, record the exact Maven command, and note any residual limitations.
