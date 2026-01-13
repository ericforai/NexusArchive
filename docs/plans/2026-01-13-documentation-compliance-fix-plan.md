# Implementation Plan - Documentation Compliance Fix

## Goal Description
Rectify project documentation to comply with `general.md` Section 7 ("Document Self-Consistency Rules"). This involves ensuring every non-whitelisted directory contains a `README.md` file with the mandatory header: `一旦我所属的文件夹有所变化，请更新我。`.

## User Review Required
> [!IMPORTANT]
> This script will batch-modify hundreds of files.
> - **New Files**: ~200+ `README.md` files will be created with placeholder text.
> - **Modified Files**: ~50+ existing `README.md` files will have the header prepended.

## Proposed Changes

### Automation Script
Create `scripts/fix_docs_compliance.py` to handle the batch processing.

#### [NEW] [fix_docs_compliance.py](file:///Users/user/nexusarchive/scripts/fix_docs_compliance.py)
- **Features**:
    - **Whitelist enforcement**: adhere to `general.md` exclusions.
    - **Missing File Creation**: Generate `README.md` with:
        ```markdown
        一旦我所属的文件夹有所变化，请更新我。

        # [Directory Name]

        (Auto-generated) [TODO: Update with directory description]
        ```
    - **Header Fix**: Check first line of existing `README.md`. If mismatch, prepend the required header.
    - **Dry Run Mode**: Default to printing changes without executing.
    - **Execute Mode**: Apply changes when flag `--execute` is passed.

## Verification Plan

### Automated Verification
1. **Execution**: Run `python3 scripts/fix_docs_compliance.py --execute`
2. **Verification**: Run `python3 scripts/fix_docs_compliance.py --verify`
   - The script will include a verification mode that behaves like the previous reporter tool.
   - **Success Criteria**: 0 missing files, 0 invalid headers.
