package com.community.common;

/**
 * 缓存名称与 Key 统一管理，避免各处以字符串硬编码
 */
public final class CacheNames {

    /** 服务分类列表（居民端首页 / 服务大厅） */
    public static final String SERVICE_CATEGORIES = "service:categories";
    /** 热门服务项目 Top N */
    public static final String HOT_ITEMS = "service:hot";
    /** 服务项目详情 */
    public static final String ITEM_DETAIL = "service:item";

    private CacheNames() {
    }
}
