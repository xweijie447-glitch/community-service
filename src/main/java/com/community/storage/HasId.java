package com.community.storage;

/**
 * 所有需要持久化的实体都带主键，供通用 JSON 存储使用
 */
public interface HasId {

    Long getId();

    void setId(Long id);
}
