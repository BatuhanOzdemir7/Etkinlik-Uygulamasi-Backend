package com.works.repository;

import com.works.CacheTestConfig;
import com.works.entity.Event;
import com.works.entity.EventStatus;
import com.works.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(CacheTestConfig.class)
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByStatus_Success() {
        // Arrange - Hafıza içi veritabanına test verileri hazırlanıyor
        User user = new User();
        user.setName("Batuhan");
        user.setSurname("Özdemir");
        user.setNickname("batuhan_ozdemir");
        user.setEmail("batuhan@example.com");
        user.setPassword("pass123");
        userRepository.save(user);

        Event event1 = new Event();
        event1.setTitle("Yayınlanan Etkinlik");
        event1.setDescription("Açıklama 1");
        event1.setOwner(user);
        event1.setStatus(EventStatus.PUBLISHED);
        event1.setEventDate(LocalDate.now().plusDays(2));
        event1.setEventTime(LocalTime.of(10, 0));
        event1.setLocation("Bursa");
        event1.setCategory("Yazılım");
        eventRepository.save(event1);

        Event event2 = new Event();
        event2.setTitle("Taslak Etkinlik");
        event2.setDescription("Açıklama 2");
        event2.setOwner(user);
        event2.setStatus(EventStatus.DRAFT);
        event2.setEventDate(LocalDate.now().plusDays(3));
        event2.setEventTime(LocalTime.of(11, 0));
        event2.setLocation("İstanbul");
        event2.setCategory("Kültür");
        eventRepository.save(event2);

        // Act - Sadece PUBLISHED statüsündeki etkinlikleri çekiyoruz
        Page<Event> resultPage = eventRepository.findByStatus(EventStatus.PUBLISHED, PageRequest.of(0, 10));

        // Assert - Doğrulama işlemleri yapılıyor
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        assertEquals("Yayınlanan Etkinlik", resultPage.getContent().get(0).getTitle());
    }

    @Test
    void testFindByOwnerIdAndStatus_Success() {
        // Arrange
        User user = new User();
        user.setName("Ege");
        user.setSurname("Başaran");
        user.setNickname("ege_basaran"); // BU SATIRI EKLEMELİSİN
        user.setEmail("ege@example.com");
        user.setPassword("pass123");
        User savedUser = userRepository.save(user);

        Event draftEvent = new Event();
        draftEvent.setTitle("Ege'nin Taslağı");
        draftEvent.setDescription("Özel Açıklama");
        draftEvent.setOwner(savedUser);
        draftEvent.setStatus(EventStatus.DRAFT);
        draftEvent.setEventDate(LocalDate.now().plusDays(1));
        draftEvent.setEventTime(LocalTime.of(15, 0));
        draftEvent.setLocation("İzmir");
        draftEvent.setCategory("Müzik");
        eventRepository.save(draftEvent);

        // Act - Belirli bir kullanıcıya ait DRAFT verilerini sorguluyoruz
        Page<Event> resultPage = eventRepository.findByOwnerIdAndStatus(savedUser.getId(), EventStatus.DRAFT, PageRequest.of(0, 10));

        // Assert
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        assertEquals("Ege'nin Taslağı", resultPage.getContent().get(0).getTitle());
    }
}