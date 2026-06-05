#!/bin/bash
# 蛋炒饭课程表 — 一键打包脚本
# 用法: ./build.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="$SCRIPT_DIR/app/build.gradle.kts"

echo "========================================"
echo "  蛋炒饭课程表 — 一键打包"
echo "========================================"

# 1. 读取当前版本
VC=$(grep -oP 'versionCode\s*=\s*\K\d+' "$GRADLE_FILE")
VN=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE_FILE")

if [ -z "$VC" ] || [ -z "$VN" ]; then
    echo "错误：无法读取版本号，请检查 $GRADLE_FILE"
    exit 1
fi

echo "当前版本: v$VN ($VC)"

# 2. 版本号递增
NEW_VC=$((VC + 1))
# versionName: 主版本.次版本 → 次版本+1
MAJOR=$(echo "$VN" | cut -d. -f1)
MINOR=$(echo "$VN" | cut -d. -f2)
NEW_MINOR=$((MINOR + 1))
NEW_VN="$MAJOR.$NEW_MINOR"

echo "新版本:   v$NEW_VN ($NEW_VC)"

# 3. 写入新版本号
sed -i "s/versionCode = $VC/versionCode = $NEW_VC/" "$GRADLE_FILE"
sed -i "s/versionName = \"$VN\"/versionName = \"$NEW_VN\"/" "$GRADLE_FILE"
echo "✓ 版本号已更新"

# 4. 构建 Release
echo ""
echo "正在构建 Release APK..."
"$SCRIPT_DIR/gradlew" assembleRelease -p "$SCRIPT_DIR"
echo "✓ 构建完成"

# 5. 重命名 APK
echo ""
echo "正在重命名 APK..."
"$SCRIPT_DIR/gradlew" renameApk -p "$SCRIPT_DIR"
echo "✓ 重命名完成"

# 6. 显示结果
APK_FILE="$SCRIPT_DIR/蛋炒饭课程表_v$NEW_VN.apk"
SIZE=$(du -h "$APK_FILE" | cut -f1)

echo ""
echo "========================================"
echo "  打包完成！"
echo "  文件: 蛋炒饭课程表_v$NEW_VN.apk"
echo "  版本: v$NEW_VN ($NEW_VC)"
echo "  大小: $SIZE"
echo "  路径: $APK_FILE"
echo "========================================"
