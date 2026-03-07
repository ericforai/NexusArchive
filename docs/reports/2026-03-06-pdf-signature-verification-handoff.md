# ERI-11 PDF Signature Verification Handoff

## Summary

Implemented a dedicated backend PDF signature verification service that normalizes library-specific outcomes into stable application-level statuses:

- `VALID`
- `INVALID`
- `UNKNOWN`

The implementation is intentionally scoped to the service layer and is not wired into archive workflow code yet.

## Files Added

- `docs/plans/2026-03-06-pdf-signature-verification-service.md`
- `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/PdfSignatureVerificationResult.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/PdfSignatureVerificationStatus.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/PdfBoxPdfSignatureVerificationService.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/PdfSignatureVerificationService.java`
- `nexusarchive-java/src/test/java/com/nexusarchive/service/signature/PdfSignatureVerificationServiceTest.java`
- `nexusarchive-java/src/test/java/com/nexusarchive/service/signature/README.md`

## Files Updated

- `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/README.md`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/README.md`
- `nexusarchive-java/src/test/java/com/nexusarchive/service/README.md`

## Behavior

### `VALID`

Returned when:

- the PDF contains at least one signature dictionary
- the signature byte range covers the whole document
- CMS parsing succeeds
- a matching signer certificate is present
- certificate validity checks pass at the signature time when that metadata is present
- cryptographic signature verification passes

### `INVALID`

Returned when:

- the PDF contains a signature but the byte range does not cover the whole document
- the signer certificate is expired or not yet valid at the signature time
- signature verification fails explicitly

### `UNKNOWN`

Returned when:

- the PDF is unsigned
- the PDF is malformed or unreadable
- the signature structure is incomplete
- signer/certificate metadata cannot be resolved
- CMS parsing fails in a way that does not justify a hard-invalid result

## Test Coverage

`nexusarchive-java/src/test/java/com/nexusarchive/service/signature/PdfSignatureVerificationServiceTest.java`

Covers:

- unsigned PDF -> `UNKNOWN`
- malformed PDF -> `UNKNOWN`
- real signed PDF -> `VALID`
- signed PDF with a certificate that is expired now but was valid at signing time -> `VALID`
- tampered signed PDF -> `INVALID`

The test creates a real signed PDF using PDFBox + BouncyCastle instead of relying on mocks.

## Verification Evidence

Fresh command executed:

```bash
mvn -f nexusarchive-java/pom.xml test -Dtest=PdfSignatureVerificationServiceTest
```

Observed result:

- `Tests run: 5`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

## PR Draft Notes

Suggested PR title:

```text
feat: add normalized PDF signature verification service
```

Suggested summary bullets:

- add dedicated PDF signature verification service with `VALID` / `INVALID` / `UNKNOWN` output
- add stable DTOs for normalized PDF verification results
- add real signed/tampered/unsigned/malformed PDF service tests

Suggested test plan:

- `mvn -f nexusarchive-java/pom.xml test -Dtest=PdfSignatureVerificationServiceTest`

## Current Blockers

The code is ready for branch/commit/PR work, but this session cannot complete those steps:

- `gh auth status` reports the default GitHub token is invalid
- the current sandbox rejects writes under `.git`, so branch creation and commit flows fail

## Resume Checklist

When GitHub auth and `.git` writes are available again:

1. Create or switch to branch `ericluwenrong/eri-11-build-pdf-signature-verification-service-with`
2. Stage the files listed above
3. Commit with a message similar to:

```text
feat: add normalized PDF signature verification service
```

4. Push the branch
5. Create the PR using the draft notes above
6. Move the Linear issue from `In Progress` to `In Review`
