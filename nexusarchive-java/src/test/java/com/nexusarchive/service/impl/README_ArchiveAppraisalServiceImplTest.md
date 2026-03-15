# ArchiveAppraisalServiceImpl Test Quick Guide

## Test File Information

**File**: `ArchiveAppraisalServiceImplTest.java`
**Lines**: 1,013
**Test Methods**: 29
**Target**: `ArchiveAppraisalServiceImpl`

## Quick Start

### Run All Tests
```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn test -Dtest=ArchiveAppraisalServiceImplTest
```

### Run Specific Test Category
```bash
# Run only creation tests
mvn test -Dtest=ArchiveAppraisalServiceImplTest\$CreateAppraisalListTests

# Run only submission tests
mvn test -Dtest=ArchiveAppraisalServiceImplTest\$SubmitAppraisalConclusionTests

# Run only status transition tests
mvn test -Dtest=ArchiveAppraisalServiceImplTest\$StatusTransitionTests
```

### Run with Coverage
```bash
mvn test -Dtest=ArchiveAppraisalServiceImplTest jacoco:report
```

## Test Categories Overview

### 1. CreateAppraisalListTests (7 tests)
Tests for creating appraisal lists with validation and edge cases.

**Key Scenarios**:
- Single and multiple archive creation
- Empty/null validation
- Archive existence checks
- Status validation (must be EXPIRED)
- Fonds validation
- Archive snapshot generation

### 2. SubmitAppraisalConclusionTests (7 tests)
Tests for submitting appraisal conclusions.

**Key Scenarios**:
- Three conclusion types: APPROVED, REJECTED, DEFERRED
- Appraisal list existence validation
- Status validation (must be PENDING)
- Invalid conclusion detection
- JSON parsing error handling

### 3. GetAppraisalListDetailTests (6 tests)
Tests for retrieving appraisal list details.

**Key Scenarios**:
- Successful detail retrieval
- Archive item conversion
- Expiration date calculation
- Null value handling
- Different retention period formats

### 4. ExportAppraisalListTests (1 test)
Tests for export functionality (currently not implemented).

### 5. EdgeCasesAndIntegrationTests (4 tests)
Performance and edge case testing.

**Key Scenarios**:
- Large dataset handling (100 archives)
- Special characters in snapshot
- Different retention period formats
- Invalid format handling

### 6. StatusTransitionTests (3 tests)
State machine transition verification.

**Key Scenarios**:
- EXPIRED → APPRAISING (creation)
- APPRAISING → APPRAISING (approved)
- APPRAISING → EXPIRED (rejected)
- APPRAISING → NORMAL (deferred)

## Test Data Helpers

The test class provides helper methods for creating test data:

```java
// Create test archive
createTestArchive(id, archiveCode, destructionStatus, fondsNo,
                 retentionPeriod, retentionStartDate)

// Create test appraisal list
createTestAppraisalList(id, status, archiveIdsJson)
```

## Mocking Strategy

- **AppraisalListMapper**: Mocked for database operations
- **ArchiveMapper**: Mocked for archive operations
- **ObjectMapper**: Real instance with JavaTimeModule registered
- **ArgumentCaptor**: Used to verify captured arguments

## Assertions Used

- **AssertJ**: Fluent assertion library for readable tests
- **Verification**: Mockito verify() for mock interaction verification
- **Exception Testing**: assertThatThrownBy() for exception scenarios

## Common Test Patterns

### Happy Path Test
```java
@Test
@DisplayName("应该成功...")
void shouldSuccessfully...() {
    // Given
    // Setup test data and mocks

    // When
    // Execute method under test

    // Then
    // Assert results and verify mocks
}
```

### Exception Test
```java
@Test
@DisplayName("应该抛出异常 - 当...")
void shouldThrowExceptionWhen...() {
    // Given
    // Setup test data

    // When & Then
    assertThatThrownBy(() -> {
        // Execute method
    })
        .isInstanceOf(ExpectedException.class)
        .hasMessage("expected message");

    // Verify no side effects
    verify(mapper, never()).someMethod();
}
```

## Coverage Verification

After running tests, check coverage:
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

Target coverage: **80%+**

## Troubleshooting

### Tests Fail to Compile
1. Ensure main code is compiled: `mvn compile`
2. Check for missing dependencies
3. Verify Java 17+ is being used

### Tests Fail to Run
1. Check if database mocks are properly configured
2. Verify ObjectMapper configuration
3. Check test resource files

### Coverage Below Target
1. Run specific test category to identify gaps
2. Add tests for uncovered branches
3. Verify edge cases are covered

## Best Practices Followed

1. ✓ **Test Independence**: Each test is isolated
2. ✓ **Descriptive Names**: Clear test method names
3. ✓ **Given-When-Then**: Structured test format
4. ✓ **Mock Verification**: Proper mock interaction checks
5. ✓ **Edge Cases**: Comprehensive boundary testing
6. ✓ **Documentation**: Javadoc comments for all tests

## Related Files

- **Implementation**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveAppraisalServiceImpl.java`
- **Interface**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveAppraisalService.java`
- **Entities**: `AppraisalList.java`, `Archive.java`
- **DTOs**: `AppraisalListDetail.java`

## Test Maintenance

When modifying `ArchiveAppraisalServiceImpl`:

1. Update this test file for new/changed methods
2. Add new test cases for new functionality
3. Update edge cases as business rules change
4. Maintain 80%+ coverage
5. Run all tests before committing

---

**Last Updated**: 2026-03-15
**Framework**: JUnit 5 + Mockito + AssertJ
