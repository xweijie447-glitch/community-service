package com.community.repository;

import com.community.domain.Review;
import com.community.storage.JsonRepository;
import com.community.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 评价仓储（data/reviews.json）
 */
@Repository
public class ReviewRepository extends JsonRepository<Review> {

    private static final Comparator<Review> CREATED_DESC =
            Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Review::getId, Comparator.reverseOrder());

    public ReviewRepository(StorageProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper, "reviews", Review.class);
    }

    public Optional<Review> findByOrderId(Long orderId) {
        return store.findAll().stream().filter(r -> orderId.equals(r.getOrderId())).findFirst();
    }

    public boolean existsByOrderId(Long orderId) {
        return findByOrderId(orderId).isPresent();
    }

    public List<Review> findByResidentIdOrderByCreatedAtDesc(Long residentId) {
        return store.findAll().stream()
                .filter(r -> residentId.equals(r.getResidentId()))
                .sorted(CREATED_DESC)
                .toList();
    }

    public List<Review> findByStaffIdOrderByCreatedAtDesc(Long staffId) {
        return store.findAll().stream()
                .filter(r -> staffId.equals(r.getStaffId()))
                .sorted(CREATED_DESC)
                .toList();
    }

    public List<Review> findTop20ByOrderByIdDesc() {
        return store.findAll().stream()
                .sorted(Comparator.comparing(Review::getId).reversed())
                .limit(20)
                .toList();
    }

    public double avgScoreByStaff(Long staffId) {
        return store.findAll().stream()
                .filter(r -> staffId.equals(r.getStaffId()))
                .mapToInt(Review::getScore)
                .average().orElse(5.0);
    }

    public long countByStaffId(Long staffId) {
        return store.findAll().stream().filter(r -> staffId.equals(r.getStaffId())).count();
    }
}
