package com.works.repository;

import com.works.entity.Event;
import com.works.entity.EventStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecifications {

    public static Specification<Event> getSearchSpecification(
            EventStatus status,
            String q,
            String category,
            String location,
            Boolean onlyFuture
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Kural: Sadece PUBLISHED (Aktif) etkinlikleri getir
            predicates.add(criteriaBuilder.equal(root.get("status"), status));

            // 2. Kural: Arama metni (q) doluysa Başlık veya Açıklamada ara
            if (q != null && !q.trim().isEmpty()) {
                String searchPattern = "%" + q.toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }

            // 3. Kural: Kategori filtresi seçilmişse
            if (category != null && !category.trim().isEmpty()) {
                String categoryPattern = "%" + category.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), categoryPattern));
            }

            // 4. Kural: Konum filtresi seçilmişse (İçerir mantığına çevrildi)
            if (location != null && !location.trim().isEmpty()) {
                String locationPattern = "%" + location.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), locationPattern));
            }

            // 5. Kural: Sadece gelecekteki etkinlikler istenmişse (DÜZELTİLEN KISIM)
            if (onlyFuture != null && onlyFuture) {
                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();

                // eventDate > today
                Predicate futureDate = criteriaBuilder.greaterThan(root.get("eventDate"), today);

                // eventDate == today AND eventTime > now
                Predicate todayDate = criteriaBuilder.equal(root.get("eventDate"), today);
                Predicate futureTime = criteriaBuilder.greaterThan(root.get("eventTime"), now);
                Predicate todayAndFutureTime = criteriaBuilder.and(todayDate, futureTime);

                predicates.add(criteriaBuilder.or(futureDate, todayAndFutureTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}