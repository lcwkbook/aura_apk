package com.aa.Aurakernel;

import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    // 公告缓存（内存级，App关闭后失效）
    private static String cachedAnnouncement = null;
    
    // 卡密查询缓存（key=卡密, value=缓存结果）
    private static final Map<String, CardCache> cardCache = new HashMap<>();

    // ===== 公告缓存 =====
    public static String getCachedAnnouncement() {
        return cachedAnnouncement;
    }

    public static void setCachedAnnouncement(String content) {
        cachedAnnouncement = content;
    }

    public static boolean hasAnnouncement() {
        return cachedAnnouncement != null && !cachedAnnouncement.isEmpty();
    }

    // ===== 卡密缓存 =====
    public static CardCache getCachedCard(String card) {
        return cardCache.get(card);
    }

    public static void cacheCard(String card, String type, String endTime, String status) {
        cardCache.put(card, new CardCache(type, endTime, status));
    }

    public static boolean hasCachedCard(String card) {
        return cardCache.containsKey(card);
    }

    // 卡密缓存数据结构
    public static class CardCache {
        public final String type;
        public final String endTime;
        public final String status;

        public CardCache(String type, String endTime, String status) {
            this.type = type;
            this.endTime = endTime;
            this.status = status;
        }
    }
}
