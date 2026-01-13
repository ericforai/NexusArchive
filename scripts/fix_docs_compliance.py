#!/usr/bin/env python3
"""
Documentation Compliance Fixer

Ensures all non-whitelisted directories have a README.md with the required header.
"""

import os
import argparse

REQUIRED_HEADER = "一旦我所属的文件夹有所变化，请更新我。"
WHITELIST_DIRS = {
    '.git', '.idea', '.vscode', 'node_modules', 'target', 'dist', 'build',
    'coverage', '.agent', '.claude', 'logs', 'backups', 'test-results',
    '__pycache__', '.run', '.husky', '.github', 'BOOT-INF', '.worktrees',
    'backup_20251228_165623', 'uploads', 'data', 'db', 'reports'
}

def is_whitelisted(path, root_dir):
    """Check if any component of the path is in the whitelist."""
    rel_path = os.path.relpath(path, root_dir)
    parts = rel_path.split(os.sep)
    for part in parts:
        if part in WHITELIST_DIRS:
            return True
        # Also skip hidden directories not explicitly handled
        if part.startswith('.') and part not in ['.']:
            return True
    return False


def get_readme_template(dir_name):
    """Generate a README.md template."""
    return f"""{REQUIRED_HEADER}

# {dir_name}

(Auto-generated) [TODO: Update with directory description]
"""


def fix_compliance(root_dir, execute=False, verify_only=False):
    """Fix or verify README.md compliance."""
    missing_readme = []
    invalid_header = []
    fixed_headers = []
    created_files = []

    for dirpath, dirnames, filenames in os.walk(root_dir):
        if is_whitelisted(dirpath, root_dir):
            continue

        rel_path = os.path.relpath(dirpath, root_dir)
        readme_path = os.path.join(dirpath, 'README.md')

        if 'README.md' not in filenames:
            missing_readme.append(rel_path)
            if execute:
                dir_name = os.path.basename(dirpath)
                with open(readme_path, 'w', encoding='utf-8') as f:
                    f.write(get_readme_template(dir_name))
                created_files.append(rel_path)
        else:
            try:
                with open(readme_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    first_line = content.split('\n')[0].strip() if content else ''

                if REQUIRED_HEADER not in first_line:
                    invalid_header.append(rel_path)
                    if execute:
                        new_content = REQUIRED_HEADER + "\n\n" + content
                        with open(readme_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        fixed_headers.append(rel_path)
            except Exception as e:
                print(f"Error processing {readme_path}: {e}")

    # Output
    if verify_only:
        print("=== Verification Report ===")
        print(f"Missing README.md: {len(missing_readme)}")
        print(f"Invalid Header: {len(invalid_header)}")
        if len(missing_readme) == 0 and len(invalid_header) == 0:
            print("\n✅ COMPLIANCE CHECK PASSED")
        else:
            print("\n🔴 COMPLIANCE CHECK FAILED")
            if missing_readme:
                print("\nMissing README.md:")
                for p in missing_readme[:20]:
                    print(f"  - {p}")
                if len(missing_readme) > 20:
                    print(f"  ... and {len(missing_readme) - 20} more")
            if invalid_header:
                print("\nInvalid Header:")
                for p in invalid_header[:20]:
                    print(f"  - {p}")
                if len(invalid_header) > 20:
                    print(f"  ... and {len(invalid_header) - 20} more")
    else:
        print("=== Dry Run / Execution Report ===")
        print(f"Directories missing README.md: {len(missing_readme)}")
        print(f"README.md with invalid header: {len(invalid_header)}")
        if execute:
            print(f"\n--- Actions Taken ---")
            print(f"Created {len(created_files)} new README.md files.")
            print(f"Fixed headers in {len(fixed_headers)} README.md files.")
        else:
            print("\n(Dry Run Mode - No changes made. Use --execute to apply.)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fix documentation compliance.")
    parser.add_argument("--execute", action="store_true", help="Apply changes.")
    parser.add_argument("--verify", action="store_true", help="Verify compliance only.")
    args = parser.parse_args()

    root_dir = os.getcwd()
    print(f"Root directory: {root_dir}\n")
    fix_compliance(root_dir, execute=args.execute, verify_only=args.verify)
