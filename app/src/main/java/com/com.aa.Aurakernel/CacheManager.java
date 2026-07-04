package com.aa.ABC;

import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    // 公告缓存（按 variableId 区分，每个内核有自己的公告）
    private static final Map<String, String> announcementCache = new HashMap<>();
    
    // 卡密查询缓存（key=卡密, value=缓存结果）
    private static final Map<String, CardCache> cardCache = new HashMap<>();

    // ===== 公告缓存（按 variableId） =====
    public static String getCachedAnnouncement(String variableId) {
        return announcementCache.get(variableId);
    }

    public static void setCachedAnnouncement(String variableId, String content) {
        if (content != null && !content.isEmpty()) {
            announcementCache.put(variableId, content);
        }
    }

    public static boolean hasAnnouncement(String variableId) {
        return announcementCache.containsKey(variableId) 
            && announcementCache.get(variableId) != null 
            && !announcementCache.get(variableId).isEmpty();
    }

    // ===== 卡密缓存（不变） =====
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
