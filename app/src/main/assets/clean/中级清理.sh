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
TARGET_DIR2=""
TARGET_DIR3=""
TARGET_DIR4=""
TARGET_DIR5=""
TARGET_DIR6=""
TARGET_DIR7=""
TARGET_DIR8=""
TARGET_DIR9=""
TARGET_DIR10=""

if [ $EXTERNAL_RESOLVE_STATUS -eq 0 ]; then
    TARGET_DIR2="$EXTERNAL_BASE_DIR/cache"
    TARGET_DIR3="$EXTERNAL_BASE_DIR/files"
    TARGET_DIR4="$TARGET_DIR3/UE4Game/ShadowTrackerExtra"
    TARGET_DIR5="$TARGET_DIR4/ShadowTrackerExtra/Saved"
    TARGET_DIR6="$TARGET_DIR5/Paks"
    TARGET_DIR7="$TARGET_DIR3/ProgramBinaryCache"
    TARGET_DIR8="$TARGET_DIR5/SaveGames"
    TARGET_DIR9="$TARGET_DIR5/Config"
    TARGET_DIR10="$TARGET_DIR6/avatarpaks"
fi

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

if [ -n "$TARGET_DIR2" ] && [ -d "$TARGET_DIR2" ]; then
    echo "删除 $TARGET_DIR2 ..."
    rm -rf "$TARGET_DIR2"
else
    echo "第二个目标目录下的cache文件夹不存在: ${TARGET_DIR2:-未解析}"
fi

if [ -n "$TARGET_DIR7" ] && [ -d "$TARGET_DIR7" ]; then
    echo "删除 $TARGET_DIR7 ..."
    rm -rf "$TARGET_DIR7"
else
    echo "第7个目标目录下的cache文件夹不存在: ${TARGET_DIR7:-未解析}"
fi

if [ -n "$TARGET_DIR8" ] && [ -d "$TARGET_DIR8" ]; then
    echo "删除 $TARGET_DIR8 ..."
    rm -rf "$TARGET_DIR8"
else
    echo "第8个目标目录下的cache文件夹不存在: ${TARGET_DIR8:-未解析}"
fi

if [ -n "$TARGET_DIR9" ] && [ -d "$TARGET_DIR9" ]; then
    echo "删除 $TARGET_DIR9 ..."
    rm -rf "$TARGET_DIR9"
else
    echo "第9个目标目录下的cache文件夹不存在: ${TARGET_DIR9:-未解析}"
fi

if [ -n "$TARGET_DIR10" ] && [ -d "$TARGET_DIR10" ]; then
    echo "删除 $TARGET_DIR10 ..."
    rm -rf "$TARGET_DIR10"
else
    echo "第10个目标目录下的cache文件夹不存在: ${TARGET_DIR10:-未解析}"
fi

if [ -n "$TARGET_DIR3" ] && [ -d "$TARGET_DIR3" ]; then
    echo "开始删除 $TARGET_DIR3 目录下除了UE4Game和ProgramBinaryCache之外的所有文件和文件夹..."
    find "$TARGET_DIR3" -mindepth 1 -maxdepth 1 ! -name 'UE4Game' ! -name 'ProgramBinaryCache' -exec rm -rf {} \;
    echo "删除完成@aadaili"
else
    echo "第三个目标目录不存在: ${TARGET_DIR3:-未解析}"
fi

if [ -n "$TARGET_DIR4" ] && [ -d "$TARGET_DIR4" ]; then
    echo "开始删除 $TARGET_DIR4 目录下除了ShadowTrackerExtra之外的所有文件夹..."
    find "$TARGET_DIR4" -mindepth 1 -maxdepth 1 ! -name '.' ! -name '..' ! -name 'ShadowTrackerExtra' -exec rm -rf {} \;
    echo "删除完成@aadaili"
else
    echo "第四个目标目录不存在: ${TARGET_DIR4:-未解析}"
fi

if [ -n "$TARGET_DIR5" ] && [ -d "$TARGET_DIR5" ]; then
    echo "开始删除 $TARGET_DIR5 目录下除了Config、Paks、SaveGames和SrcVersion.ini之外的所有文件和文件夹..."
    find "$TARGET_DIR5" -mindepth 1 -maxdepth 1 \
        ! -name 'Config' ! -name 'Paks' ! -name 'SaveGames' ! -name 'SrcVersion.ini' \
        -exec rm -rf {} \;
    echo "删除完成@aadaili"
else
    echo "第五个目标目录不存在: ${TARGET_DIR5:-未解析}"
fi

if [ -n "$TARGET_DIR6" ] && [ -d "$TARGET_DIR6" ]; then
    echo "开始删除 $TARGET_DIR6 目录下的eifsCache1、eifsCache2、eifsCache3和eifsCache5文件夹..."
    for cache in eifsCache1 eifsCache2 eifsCache3 eifsCache5; do
        if [ -d "$TARGET_DIR6/$cache" ]; then
            echo "删除文件夹: $TARGET_DIR6/$cache"
            rm -rf "$TARGET_DIR6/$cache"
        else
            echo "文件夹不存在: $TARGET_DIR6/$cache"
        fi
    done
    echo "删除完成 初泽牛逼"
else
    echo "第六个目标目录不存在: ${TARGET_DIR6:-未解析}"
fi

echo "操作完成 公益频道 @aadaili"
