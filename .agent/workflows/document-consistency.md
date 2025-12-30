---
description: Ensure documentation and source code annotations are consistent with the actual codebase structure.
---

# Document Consistency Check Workflow

Run this workflow after any significant code modification, refactoring, or file structure change.

## 1. Identify Scope
- List all directories where files were added, renamed, or deleted.
- List all critical source files (Entities, Services, Controllers, Core Components) that were modified.

## 2. Directory Documentation (`README.md`)
For each affected directory:
1. **Check Existence**: Does `README.md` exist?
   - If NO: Create it.
2. **Check Header**: Does it start with the standard header?
   ```
   // Input: [Description]
   // Output: [Description]
   // Pos: [Path]
   // 一旦我所属的文件夹有所变化，请更新我。
   ```
3. **Verify Content**:
   - Compare the list of files in `README.md` with the actual `ls` output.
   - Update the file list table/list to reflect current reality.
   - Ensure descriptions are accurate.

## 3. Source Code Headers
For each modified key file:
1. **Check Header**: Does it have the standard 3-line header?
   ```java
   // Input: [Dependencies]
   // Output: [Output/Role]
   // Pos: [Path]
   // 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
   ```
2. **Update Content**: If the file's purpose or dependencies changed significantly, update the `Input` and `Output` fields.

## 4. Verification
- [ ] All new directories have `README.md`.
- [ ] All modified directories have updated `README.md` file lists.
- [ ] All modified source files have accurate Headers.
