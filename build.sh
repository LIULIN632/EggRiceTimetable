#!/bin/bash
# 蛋炒饭课程表 — 一键打包脚本
# 用法: ./build.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="$SCRIPT_DIR/app/build.gradle.kts"
APK_SRC="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"

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

# 2. 版本号递增（三段式 X.Y.Z：Patch +1，每段上限 99，如 1.99.99）
NEW_VC=$((VC + 1))
MAJOR=$(echo "$VN" | cut -d. -f1)
MINOR=$(echo "$VN" | cut -d. -f2)
PATCH=$(echo "$VN" | cut -d. -f3)
PATCH=${PATCH:-0}

NEW_PATCH=$((PATCH + 1))
NEW_MINOR=$MINOR
NEW_MAJOR=$MAJOR
if [ "$NEW_PATCH" -gt 99 ]; then
    NEW_PATCH=0
    NEW_MINOR=$((MINOR + 1))
fi
if [ "$NEW_MINOR" -gt 99 ]; then
    NEW_MINOR=0
    NEW_MAJOR=$((MAJOR + 1))
fi
NEW_VN="$NEW_MAJOR.$NEW_MINOR.$NEW_PATCH"

echo "新版本:   v$NEW_VN ($NEW_VC)"

# 3. 写入新版本号
sed -i.bak "s/versionCode = $VC/versionCode = $NEW_VC/" "$GRADLE_FILE"
sed -i.bak "s/versionName = \"$VN\"/versionName = \"$NEW_VN\"/" "$GRADLE_FILE"
rm -f "${GRADLE_FILE}.bak"
echo "✓ 版本号已更新"

# 4. 构建 Release
echo ""
echo "正在构建 Release APK..."
"$SCRIPT_DIR/gradlew" assembleRelease -p "$SCRIPT_DIR"
echo "✓ 构建完成"

# 5. 复制 APK 到项目根目录
APK_DEST="$SCRIPT_DIR/蛋炒饭课程表_v$NEW_VN.apk"
cp "$APK_SRC" "$APK_DEST"
echo "✓ APK 已输出: $APK_DEST"

# 6. 显示结果
SIZE=$(du -h "$APK_DEST" | cut -f1)

# 7. 清理旧版本：只保留最近 3 个
OLD_APKS=$(ls -t "$SCRIPT_DIR"/蛋炒饭课程表_v*.apk 2>/dev/null || true)
KEEP=3
COUNT=$(echo "$OLD_APKS" | wc -l)
if [ "$COUNT" -gt "$KEEP" ]; then
    REMOVED=$((COUNT - KEEP))
    echo "$OLD_APKS" | tail -n +$((KEEP + 1)) | while read -r f; do
        rm -f "$f"
    done
    echo "✓ 已清理 $REMOVED 个旧版本 APK"
fi

echo ""
echo "========================================"
echo "  打包完成！"
echo "  文件: 蛋炒饭课程表_v$NEW_VN.apk"
echo "  版本: v$NEW_VN ($NEW_VC)"
echo "  大小: $SIZE"
echo "  路径: $APK_DEST"
echo "========================================"
