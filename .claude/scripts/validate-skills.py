#!/usr/bin/env python3
"""
AI Skills Standard Validator v1.0

验证 Skill 是否符合 AISS v1.0 标准规范。

用法:
    python validate-skills.py              # 验证所有 Skills
    python validate-skills.py entropy-reduction  # 验证单个 Skill
    python validate-skills.py --fix        # 尝试自动修复问题
"""

import argparse
import os
import re
import sys
from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import List, Optional


class Level(Enum):
    ERROR = "ERROR"
    WARNING = "WARNING"
    INFO = "INFO"


@dataclass
class ValidationResult:
    level: Level
    message: str
    file: Optional[str] = None
    line: Optional[int] = None
    fixable: bool = False


class SkillValidator:
    """AI Skills Standard 验证器"""

    # 禁止的文件名
    FORBIDDEN_FILES = [
        "README.md",
        "INSTALLATION_GUIDE.md",
        "QUICK_REFERENCE.md",
        "CHANGELOG.md",
        "CONTRIBUTING.md",
        "AUTHORS.md",
        "LICENSE.md",
    ]

    # SKILL.md 最大行数
    MAX_SKILL_LINES = 500

    # description 最小词数
    MIN_DESCRIPTION_WORDS = 20

    # description 最大词数
    MAX_DESCRIPTION_WORDS = 500

    def __init__(self, skills_dir: Path, strict: bool = False):
        self.skills_dir = Path(skills_dir)
        self.strict = strict
        self.results: List[ValidationResult] = []

    def validate_all(self) -> bool:
        """验证所有 Skills"""
        if not self.skills_dir.exists():
            self.add_result(
                Level.ERROR, f"Skills 目录不存在: {self.skills_dir}"
            )
            return False

        found = False
        for item in self.skills_dir.iterdir():
            if item.is_dir() and not item.name.startswith("."):
                skill_dir = item
                skill_file = skill_dir / "SKILL.md"
                if skill_file.exists():
                    found = True
                    self.validate_skill(skill_dir)
                elif (skill_dir / "skill.md").exists():
                    # 大小写不敏感检查
                    self.add_result(
                        Level.WARNING,
                        f"文件名应为大写: skill.md → SKILL.md",
                        str(skill_dir / "skill.md"),
                        fixable=True,
                    )

        if not found:
            self.add_result(Level.ERROR, f"未找到任何 Skill (SKILL.md)")

        self.print_results()
        return not any(r.level == Level.ERROR for r in self.results)

    def validate_skill(self, skill_dir: Path) -> None:
        """验证单个 Skill"""
        skill_file = skill_dir / "SKILL.md"

        self.check_frontmatter(skill_file)
        self.check_file_organization(skill_dir)
        self.check_skill_size(skill_file)
        self.check_metadata_file(skill_dir)

    def add_result(
        self,
        level: Level,
        message: str,
        file: Optional[str] = None,
        line: Optional[int] = None,
        fixable: bool = False,
    ) -> None:
        self.results.append(ValidationResult(level, message, file, line, fixable))

    def check_frontmatter(self, skill_file: Path) -> None:
        """检查 YAML frontmatter"""
        try:
            content = skill_file.read_text(encoding="utf-8")
        except Exception as e:
            self.add_result(Level.ERROR, f"无法读取文件: {e}", str(skill_file))
            return

        # 检查 frontmatter 格式
        if not content.startswith("---"):
            self.add_result(Level.ERROR, "缺少 YAML frontmatter (应以 --- 开头)", str(skill_file), 1)
            return

        # 提取 frontmatter
        frontmatter_end = content.find("---", 3)
        if frontmatter_end == -1:
            self.add_result(Level.ERROR, "YAML frontmatter 未正确关闭 (需要 --- 结束)", str(skill_file), 1)
            return

        frontmatter = content[3:frontmatter_end]

        # 检查必需字段
        name_match = re.search(r'^name:\s*(.+)$', frontmatter, re.MULTILINE)
        if not name_match:
            self.add_result(Level.ERROR, "缺少必需字段: name", str(skill_file), 1)
        else:
            name = name_match.group(1).strip().strip('"\'')
            if not re.match(r'^[a-z0-9]+(-[a-z0-9]+)*$', name):
                self.add_result(
                    Level.WARNING,
                    f"name 应使用 kebab-case: '{name}'",
                    str(skill_file),
                    1,
                )

        description_match = re.search(r'^description:\s*\|?\s*(.+)$', frontmatter, re.MULTILINE | re.DOTALL)
        if not description_match:
            self.add_result(Level.ERROR, "缺少必需字段: description", str(skill_file), 1)
        else:
            description = description_match.group(1).strip()
            word_count = len(description.split())
            if word_count < self.MIN_DESCRIPTION_WORDS:
                self.add_result(
                    Level.ERROR,
                    f"description 过短 ({word_count} 词，最少 {self.MIN_DESCRIPTION_WORDS} 词)",
                    str(skill_file),
                    1,
                )
            if "Use when" not in description and "use when" not in description:
                self.add_result(
                    Level.WARNING,
                    "description 应包含 'Use when' 触发场景说明",
                    str(skill_file),
                    1,
                )

        # 检查禁止在 frontmatter 中添加额外字段（除了 license）
        lines = frontmatter.strip().split('\n')
        for line in lines[2:]:  # 跳过 name 和 description
            if line.strip() and not line.strip().startswith('#'):
                key = line.split(':')[0].strip()
                if key not in ['license']:
                    self.add_result(
                        Level.WARNING,
                        f"frontmatter 中不建议添加额外字段: {key} (请使用 skill-metadata.yml)",
                        str(skill_file),
                        1,
                    )

    def check_file_organization(self, skill_dir: Path) -> None:
        """检查文件组织"""
        for item in skill_dir.iterdir():
            if item.is_file():
                name = item.name
                if name in self.FORBIDDEN_FILES:
                    self.add_result(
                        Level.ERROR,
                        f"禁止的文件: {name} (标准 3.3)",
                        str(item),
                        fixable=True,
                    )
                elif name.lower() in [f.lower() for f in self.FORBIDDEN_FILES]:
                    self.add_result(
                        Level.WARNING,
                        f"不建议的文件: {name} (大小写不匹配标准)",
                        str(item),
                    )

    def check_skill_size(self, skill_file: Path) -> None:
        """检查 SKILL.md 大小"""
        try:
            content = skill_file.read_text(encoding="utf-8")
            line_count = len(content.split('\n'))
            if line_count > self.MAX_SKILL_LINES:
                self.add_result(
                    Level.WARNING,
                    f"SKILL.md 过长 ({line_count} 行，建议 < {self.MAX_SKILL_LINES} 行)。考虑将内容拆分到 references/",
                    str(skill_file),
                )
        except Exception:
            pass

    def check_metadata_file(self, skill_dir: Path) -> None:
        """检查 skill-metadata.yml"""
        metadata_file = skill_dir / "skill-metadata.yml"
        if not metadata_file.exists():
            self.add_result(
                Level.INFO,
                "缺少 skill-metadata.yml (推荐用于跨平台)",
                str(skill_dir),
            )
            return

        try:
            import yaml
            with open(metadata_file) as f:
                data = yaml.safe_load(f)

            if isinstance(data, dict):
                if 'skillFormat' in data:
                    format_ver = data['skillFormat']
                    if not format_ver.startswith('aiss-'):
                        self.add_result(
                            Level.WARNING,
                            f"skillFormat 应以 'aiss-' 开头: {format_ver}",
                            str(metadata_file),
                        )
                if 'version' in data:
                    version = data['version']
                    if not re.match(r'^\d+\.\d+\.\d+', version):
                        self.add_result(
                            Level.WARNING,
                            f"version 应为语义化版本 (如 1.0.0): {version}",
                            str(metadata_file),
                        )
        except ImportError:
            self.add_result(
                Level.INFO,
                "安装 pyyaml 以验证 skill-metadata.yml 内容: pip install pyyaml",
                str(metadata_file),
            )
        except Exception as e:
            self.add_result(
                Level.WARNING,
                f"skill-metadata.yml 解析失败: {e}",
                str(metadata_file),
            )

    def print_results(self) -> None:
        """打印验证结果"""
        # 按技能分组
        by_skill = {}
        for r in self.results:
            if r.file:
                skill = Path(r.file).parent.name
            else:
                skill = "(全局)"
            by_skill.setdefault(skill, []).append(r)

        print("\n" + "=" * 60)
        print("AI Skills Standard v1.0 - 验证结果")
        print("=" * 60)

        error_count = sum(1 for r in self.results if r.level == Level.ERROR)
        warning_count = sum(1 for r in self.results if r.level == Level.WARNING)
        info_count = sum(1 for r in self.results if r.level == Level.INFO)

        for skill, results in by_skill.items():
            print(f"\n📁 {skill}")
            for r in results:
                icon = {"ERROR": "❌", "WARNING": "⚠️", "INFO": "ℹ️"}[r.level.value]
                fix_note = " [可自动修复]" if r.fixable else ""
                print(f"  {icon} {r.level.value}: {r.message}{fix_note}")

        print("\n" + "-" * 60)
        print(f"总计: {error_count} 错误, {warning_count} 警告, {info_count} 信息")
        print("=" * 60)

        if error_count == 0 and warning_count == 0:
            print("\n✅ 所有 Skills 符合 AISS v1.0 标准！\n")


def main():
    parser = argparse.ArgumentParser(description="AI Skills Standard 验证器")
    parser.add_argument(
        "skill",
        nargs="?",
        help="要验证的 Skill 名称（留空验证所有）",
    )
    parser.add_argument(
        "--skills-dir",
        default=".claude/skills",
        help="Skills 目录路径 (默认: .claude/skills)",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="严格模式（警告也视为错误）",
    )
    parser.add_argument(
        "--fix",
        action="store_true",
        help="自动修复可修复的问题",
    )

    args = parser.parse_args()

    # 确定 Skills 目录
    if os.path.isabs(args.skills_dir):
        skills_dir = Path(args.skills_dir)
    else:
        # 从脚本位置推导项目根目录
        script_dir = Path(__file__).parent
        project_root = script_dir.parent.parent
        skills_dir = project_root / args.skills_dir

    validator = SkillValidator(skills_dir, strict=args.strict)

    if args.skill:
        # 验证单个 Skill
        skill_dir = skills_dir / args.skill
        if not skill_dir.exists():
            print(f"❌ Skill 不存在: {skill_dir}")
            sys.exit(1)
        validator.validate_skill(skill_dir)
        validator.print_results()
    else:
        # 验证所有 Skills
        success = validator.validate_all()
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
