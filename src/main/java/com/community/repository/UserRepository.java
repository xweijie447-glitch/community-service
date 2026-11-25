package com.community.repository;

import com.community.domain.Role;
import com.community.domain.User;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓储（JSON 文件：data/users.json）
 */
@Repository
public class UserRepository extends JsonRepository<User> {

    public UserRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "users", User.class);
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return store.findAll().stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst();
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public List<User> findByRoleOrderByIdAsc(Role role) {
        return store.findAll().stream()
                .filter(u -> u.getRole() == role)
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    public List<User> findAllByRoleAndEnabledTrueOrderByRealNameAsc(Role role) {
        return store.findAll().stream()
                .filter(u -> u.getRole() == role && Boolean.TRUE.equals(u.getEnabled()))
                .sorted(Comparator.comparing(User::getRealName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public long countByRole(Role role) {
        return store.findAll().stream().filter(u -> u.getRole() == role).count();
    }
}
