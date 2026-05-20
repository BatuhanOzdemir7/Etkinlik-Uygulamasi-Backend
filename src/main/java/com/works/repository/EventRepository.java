package com.works.repository;

import com.works.entity.Event;
import com.works.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchActiveEvents(
            @Param("status") EventStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<Event> findByOwnerIdAndStatus(Long id, EventStatus status, Pageable pageable);

    // Profil sayfası: kullanıcının sahibi olduğu TÜM etkinlikler (tüm statuslar)
    List<Event> findByOwnerId(Long ownerId);

    // Profil sayfası: kullanıcının sahibi olduğu belirli statustaki etkinlikler
    List<Event> findByOwnerIdAndStatusIn(Long ownerId, List<EventStatus> statuses);

    // Profil sayfası: kullanıcının katılımcı olduğu etkinlikler
    // Spring Data JPA @ManyToMany ilişkisinde participants.id üzerinden sorgu üretir
    List<Event> findByParticipantsId(Long userId);
}