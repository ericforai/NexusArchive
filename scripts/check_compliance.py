#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Input: File System, OS Path, Regular Expressions
# Output: Compliance Report (Stdout)
# Pos: DevOps/Scripts - Project Compliance Guard
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import os
import re
import sys

# ==========================================
# 配置区
# ==========================================

# 允许检查的文件后缀
CHECK_EXTENSIONS = {'.java', '.ts', '.tsx', '.py'}

# 白名单目录 (不检查)
WHITELIST_DIRS = {
    'node_modules', 'target', '.git', 'dist', 'build', 'backups', 
    '.agent', '.vscode', '.github', 'nginx', 'uploads', 'data', 'logs'
}

# 必须包含的头注释关键词
HEADER_KEYWORDS = ['Input:', 'Output:', 'Pos:', '一旦我被更新']

# 目录 MD 必须包含的声明
DIR_MD_DECLARATION = '一旦我所属的文件夹有所变化，请更新我。'

# ==========================================
# 核心逻辑
# ==========================================

def check_file_header(file_path):
    """检查文件头注释是否符合规范"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            # 读取前 15 行
            lines = [f.readline() for _ in range(15)]
            content = "".join(lines)
            
            missing = []
            for kw in HEADER_KEYWORDS:
                if kw not in content:
                    missing.append(kw)
            
            return missing
    except Exception as e:
        return [f"Error reading file: {e}"]

def check_directory_md(dir_path):
    """检查目录是否包含合规的 README.md"""
    readme_path = os.path.join(dir_path, 'README.md')
    if not os.path.exists(readme_path):
        return "Missing README.md"
    
    try:
        with open(readme_path, 'r', encoding='utf-8') as f:
            first_line = f.readline().strip()
            if DIR_MD_DECLARATION not in first_line:
                return f"Missing declaration in first line: '{DIR_MD_DECLARATION}'"
    except Exception as e:
        return f"Error reading README.md: {e}"
    
    return None

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    issues = []
    
    print(f"🔍 Starting compliance check in: {repo_root}")
    print("=" * 60)

    for root, dirs, files in os.walk(repo_root):
        # 过滤白名单目录
        dirs[:] = [d for d in dirs if d not in WHITELIST_DIRS and not d.startswith('.')]
        
        # 检查当前目录的 README.md (如果是项目根目录或非空目录)
        if files or dirs:
            dir_issue = check_directory_md(root)
            if dir_issue:
                # 排除根目录下的某些特殊情况（如果需要）
                issues.append(f"[DIR]  {os.path.relpath(root, repo_root)}: {dir_issue}")

        # 检查文件头
        for file in files:
            ext = os.path.splitext(file)[1]
            if ext in CHECK_EXTENSIONS:
                file_path = os.path.join(root, file)
                # 排除某些文件名
                if file in ['check_compliance.py', 'README.md', 'package.json', 'pom.xml']:
                    continue
                
                missing_kws = check_file_header(file_path)
                if missing_kws:
                    issues.append(f"[FILE] {os.path.relpath(file_path, repo_root)}: Missing {missing_kws}")

    # 输出结果
    if not issues:
        print("\n✅ All documents are compliant! Good job.")
        sys.exit(0)
    else:
        print(f"\n❌ Found {len(issues)} compliance issues:\n")
        for issue in issues:
            print(issue)
        print("\n" + "=" * 60)
        print("Note: Please follow the 'Document Self-Consistency Rules' in .agent/rules/general.md")
        sys.exit(1)

if __name__ == "__main__":
    main()
