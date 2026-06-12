#!/system/bin/sh

PKG="com.tencent.tmgp.pubgmhd"

resolve_internal_pkg_dir() {
    for candidate in /data/user/*/"$PKG"; do
        if [ -d "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    if [ -d "/data/data/$PKG" ]; then
        printf '%s\n' "/data/data/$PKG"
        return 0
    fi

    return 1
}

resolve_internal_user_id() {
    internal_dir="$1"

    case "$internal_dir" in
        /data/user/*)
            user_part=${internal_dir#/data/user/}
            printf '%s\n' "${user_part%%/*}"
            return 0
            ;;
    esac

    return 1
}

resolve_external_pkg_dir() {
    preferred_user_id="$1"

    if [ -n "$preferred_user_id" ]; then
        preferred_dir="/storage/emulated/$preferred_user_id/Android/data/$PKG"
        if [ -d "$preferred_dir" ]; then
            printf '%s\n' "$preferred_dir"
            return 0
        fi
    fi

    for candidate in /storage/emulated/*/Android/data/"$PKG"; do
        if [ -d "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    return 1
}

log_path_state() {
    label="$1"
    path="$2"

    if [ -n "$path" ] && [ -e "$path" ]; then
        echo "$label 已解析: $path"
        ls -ld "$path" 2>/dev/null || echo "$label 无法读取详情: $path"
    else
        echo "$label 未找到"
    fi
}

INTERNAL_BASE_DIR="$(resolve_internal_pkg_dir)"
INTERNAL_RESOLVE_STATUS=$?
INTERNAL_USER_ID="$(resolve_internal_user_id "$INTERNAL_BASE_DIR" 2>/dev/null)"
EXTERNAL_BASE_DIR="$(resolve_external_pkg_dir "$INTERNAL_USER_ID")"
EXTERNAL_RESOLVE_STATUS=$?

TARGET_DIR1="$INTERNAL_BASE_DIR"
TARGET_DIR2="$EXTERNAL_BASE_DIR"

echo "==== 环境诊断 ===="
id 2>/dev/null || echo "无法执行 id"
echo "内部用户ID: ${INTERNAL_USER_ID:-未解析}"
log_path_state "内部目录" "$INTERNAL_BASE_DIR"
log_path_state "外部目录" "$EXTERNAL_BASE_DIR"
echo "=================="

if [ $INTERNAL_RESOLVE_STATUS -eq 0 ] && [ -d "$TARGET_DIR1" ]; then
    echo "开始删除 $TARGET_DIR1 下的所有子文件夹..."
    for dir in "$TARGET_DIR1"/*; do
        if [ -d "$dir" ]; then
            echo "删除文件夹: $dir"
            rm -rf "$dir"
        fi
    done
else
    echo "跳过内部目录清理，未找到目标目录"
fi

if [ $EXTERNAL_RESOLVE_STATUS -eq 0 ] && [ -d "$TARGET_DIR2" ]; then
    echo "删除 $TARGET_DIR2 ..."
    rm -rf "$TARGET_DIR2"
else
    echo "外部目标目录不存在: ${TARGET_DIR2:-未解析}"
fi

echo "高级清理删除操作完成 "
echo "公益频道 @aadaili"
