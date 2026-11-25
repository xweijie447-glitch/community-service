package com.community.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * 单表 JSON 文件存储。
 * <p>设计要点：</p>
 * <ul>
 *   <li>启动时把整个 JSON 文件读入内存，读操作走内存，避免频繁 IO；</li>
 *   <li>写操作立即整体落盘（先写临时文件再原子替换），保证进程退出不丢数据；</li>
 *   <li>写操作加锁，单机多线程下不会互相覆盖，也不需要数据库事务。</li>
 * </ul>
 *
 * @param <T> 实体类型，必须有 Long 型主键
 */
@Slf4j
public class JsonStore<T extends HasId> {

    private final Path file;
    private final Class<T> type;
    private final ObjectMapper mapper;
    private final Map<Long, T> memory = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();
    private long nextId = 1L;

    public JsonStore(Path dir, String name, Class<T> type, ObjectMapper mapper) {
        this.file = dir.resolve(name + ".json");
        this.type = type;
        this.mapper = mapper;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            log.info("数据文件不存在，初始化空表：{}", file.toAbsolutePath());
            return;
        }
        try {
            List<T> list = mapper.readValue(file.toFile(),
                    mapper.getTypeFactory().constructCollectionType(List.class, type));
            for (T entity : list) {
                if (entity.getId() != null) {
                    memory.put(entity.getId(), entity);
                }
            }
            nextId = memory.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1;
            log.info("加载数据文件 {}，共 {} 条", file.getFileName(), memory.size());
        } catch (IOException e) {
            throw new IllegalStateException("数据文件解析失败：" + file.toAbsolutePath(), e);
        }
    }

    public synchronized void flush() {
        try {
            Files.createDirectories(file.getParent());
            List<T> snapshot = new ArrayList<>(memory.values());
            snapshot.sort(Comparator.comparing(HasId::getId));
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), snapshot);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("数据文件写入失败：" + file.toAbsolutePath(), e);
        }
    }

    public List<T> findAll() {
        List<T> list = new ArrayList<>(memory.values());
        list.sort(Comparator.comparing(HasId::getId));
        return list;
    }

    public Optional<T> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(memory.get(id));
    }

    public boolean existsById(Long id) {
        return id != null && memory.containsKey(id);
    }

    public long count() {
        return memory.size();
    }

    public T save(T entity) {
        synchronized (writeLock) {
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            memory.put(entity.getId(), entity);
            flush();
            return entity;
        }
    }

    public List<T> saveAll(Collection<T> entities) {
        synchronized (writeLock) {
            for (T entity : entities) {
                if (entity.getId() == null) {
                    entity.setId(nextId++);
                }
                memory.put(entity.getId(), entity);
            }
            flush();
            return new ArrayList<>(entities);
        }
    }

    public void delete(T entity) {
        if (entity != null && entity.getId() != null) {
            deleteById(entity.getId());
        }
    }

    public void deleteById(Long id) {
        synchronized (writeLock) {
            memory.remove(id);
            flush();
        }
    }

    /** 原子更新：读取-修改-写回在同一把锁内完成，用于状态流转等并发场景 */
    public T update(Long id, UnaryOperator<T> mutator) {
        synchronized (writeLock) {
            T current = memory.get(id);
            if (current == null) {
                return null;
            }
            T updated = mutator.apply(current);
            memory.put(id, updated);
            flush();
            return updated;
        }
    }

    public Path getFile() {
        return file;
    }
}
