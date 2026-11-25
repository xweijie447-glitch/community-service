package com.community.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository 基类：把 JSON 存储包装成类似 Spring Data 的常用方法集合
 */
public abstract class JsonRepository<T extends HasId> {

    protected final JsonStore<T> store;

    protected JsonRepository(StorageProperties properties, ObjectMapper mapper, String name, Class<T> type) {
        Path dir = properties.resolveDir();
        this.store = new JsonStore<>(dir, name, type, mapper);
    }

    public List<T> findAll() {
        return store.findAll();
    }

    public Optional<T> findById(Long id) {
        return store.findById(id);
    }

    public boolean existsById(Long id) {
        return store.existsById(id);
    }

    public List<T> findAllById(Collection<Long> ids) {
        List<T> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        ids.forEach(id -> store.findById(id).ifPresent(result::add));
        return result;
    }

    public T save(T entity) {
        return store.save(entity);
    }

    public List<T> saveAll(Collection<T> entities) {
        return store.saveAll(entities);
    }

    public void delete(T entity) {
        store.delete(entity);
    }

    public void deleteById(Long id) {
        store.deleteById(id);
    }

    public long count() {
        return store.count();
    }

    protected Page<T> page(List<T> all, Pageable pageable) {
        if (pageable == null) {
            return new PageImpl<>(all);
        }
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(new ArrayList<>(all.subList(from, to)), pageable, all.size());
    }

    public String storageFile() {
        return store.getFile().toAbsolutePath().toString();
    }
}
