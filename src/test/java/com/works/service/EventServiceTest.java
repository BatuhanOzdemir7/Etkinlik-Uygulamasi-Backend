package com.works.service;

import com.works.dto.EventCreateRequestDto;
import com.works.dto.UserResponseDto;
import com.works.entity.Event;
import com.works.entity.EventStatus;
import com.works.entity.User;
import com.works.repository.EventRepository;
import com.works.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpSession session;

    @InjectMocks
    EventService eventService;

    @Test
    void createEvent_success() { //başarılı şekilde etkinlik yaratılabilecek mi
        EventCreateRequestDto requestDto = new EventCreateRequestDto();
        requestDto.setTitle("Bahar Şenliği");
        requestDto.setDescription("Üniversite bahar şenliği etkinliği");

        // User ID değerleri Long formatına (1L) uygun olarak düzeltildi
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User dbUser = new User();
        dbUser.setId(1L);
        dbUser.setName("Batuhan");
        dbUser.setSurname("Özdemir");

        // Event ID değeri Integer formatına (100) uygun olarak bırakıldı
        Event savedEvent = new Event();
        savedEvent.setId(100);
        savedEvent.setTitle("Bahar Şenliği");
        savedEvent.setOwner(dbUser);
        savedEvent.setParticipants(new HashSet<>());
        savedEvent.getParticipants().add(dbUser);

        // Mock aramaları User ID'sine (1L) göre ayarlandı
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.create(requestDto);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("Bahar Şenliği", result.getTitle());
        assertEquals(1L, result.getOwner().getId());
        assertEquals(1, result.getParticipants().size());

        verify(request, times(1)).getSession();
        verify(userRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void deleteOne_success_whenUserIsOwner() { //etkinliğin sahibi kendi etkinliğini silebilecek mi (mutlu yol)
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User owner = new User();
        owner.setId(1L); // Oturumdaki kullanıcı ile aynı ID

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // ACT
        ResponseEntity response = eventService.deleteOne(100L);

        // ASSERT
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        // Veritabanı silme komutunun tam olarak 1 kez çalıştırıldığını doğrula
        verify(eventRepository, times(1)).deleteById(100L);
    }

    @Test
    void deleteOne_fails_whenUserIsNotOwner() { //başka bir kullanıcı sahibi olmadığı etkinliği silebilecek mi (kötü yol)
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(2L); // Oturumda farklı biri var (Örneğin Ege'nin hesabı)

        User owner = new User();
        owner.setId(1L); // Etkinliğin asıl sahibi sensin

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // ACT
        ResponseEntity response = eventService.deleteOne(100L);

        // ASSERT
        // Sistem 403 (Forbidden/Yasak) hatası fırlatmalı
        assertEquals(403, response.getStatusCode().value());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Bu etkinliği silme yetkiniz bulunmuyor.", body.get("message"));

        // EN KRİTİK DOĞRULAMA: Yetkisiz işlem olduğu için veritabanında silme metodu ASLA çalışmamalı!
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteOne_fails_whenEventNotFound() { //eritabanında olmayan (veya önceden silinmiş) bir ID gönderildiğinde uygulamanın çökmeden düzgün bir 404 Not Found yanıtı dönüp dönmediğini test eder. (kötü yol)
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        // Veritabanında 999 ID'li bir etkinlik aranıyor ama boş dönüyor
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT
        ResponseEntity response = eventService.deleteOne(999L);

        // ASSERT
        assertEquals(404, response.getStatusCode().value());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Event not found id: 999", body.get("message"));

        // Silme işlemi tetiklenmemeli
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void joinEvent_success() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User dbUser = new User();
        dbUser.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setParticipants(new java.util.HashSet<>());

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));

        // ACT
        ResponseEntity<Object> response = eventService.joinEvent(100L);

        // ASSERT
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("Etkinliğe başarıyla katıldınız.", body.get("message"));

        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void joinEvent_fails_whenAlreadyJoined() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User dbUser = new User();
        dbUser.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setParticipants(new java.util.HashSet<>());
        event.getParticipants().add(dbUser); // Kullanıcı listeye zaten dahil edilmiş

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));

        // ACT
        ResponseEntity<Object> response = eventService.joinEvent(100L);

        // ASSERT
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Bu etkinliğe zaten katıldınız.", body.get("message"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void joinEvent_fails_whenEventNotFound() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT
        ResponseEntity<Object> response = eventService.joinEvent(999L);

        // ASSERT
        assertEquals(404, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Etkinlik bulunamadı.", body.get("message"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void leaveEvent_success() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User dbUser = new User();
        dbUser.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setParticipants(new java.util.HashSet<>());
        event.getParticipants().add(dbUser); // Kullanıcı etkinlik listesinde mevcut

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));

        // ACT
        ResponseEntity<Object> response = eventService.leaveEvent(100L);

        // ASSERT
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("Etkinlik katılımınız iptal edildi.", body.get("message"));

        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void leaveEvent_fails_whenNotJoined() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User dbUser = new User();
        dbUser.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setParticipants(new java.util.HashSet<>()); // Katılımcı listesi boş

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));

        // ACT
        ResponseEntity<Object> response = eventService.leaveEvent(100L);

        // ASSERT
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Bu etkinliğe zaten kayıtlı değilsiniz.", body.get("message"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void update_success() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User owner = new User();
        owner.setId(1L);

        Event existingEvent = new Event();
        existingEvent.setId(100);
        existingEvent.setOwner(owner);

        // @Value mimarisi gereği tüm zorunlu 7 parametre yapıcı metot içinde gönderilir
        com.works.dto.EventUpdateRequestDto updateRequestDto = new com.works.dto.EventUpdateRequestDto(
                100L,
                "Güncellenmiş Etkinlik Başlığı",
                "Üniversite bahar şenliği organizasyonu",
                java.time.LocalDate.now().plusDays(5),
                java.time.LocalTime.of(14, 0),
                "Mete Cengiz Kültür Merkezi",
                "Kültür-Sanat"
        );

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existingEvent));

        // ACT
        ResponseEntity<Object> response = eventService.update(updateRequestDto);

        // ASSERT
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("Event updated successfully.", body.get("message"));

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void update_fails_whenNotOwner() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(2L); // Farklı bir kullanıcı

        User owner = new User();
        owner.setId(1L); // Etkinliğin asıl sahibi

        Event existingEvent = new Event();
        existingEvent.setId(100);
        existingEvent.setOwner(owner);

        // Yapıcı metot ile sahte veri paketi oluşturuluyor
        com.works.dto.EventUpdateRequestDto updateRequestDto = new com.works.dto.EventUpdateRequestDto(
                100L,
                "Güncellenmiş Etkinlik Başlığı",
                "Üniversite bahar şenliği organizasyonu",
                java.time.LocalDate.now().plusDays(5),
                java.time.LocalTime.of(14, 0),
                "Mete Cengiz Kültür Merkezi",
                "Kültür-Sanat"
        );

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existingEvent));

        // ACT
        ResponseEntity<Object> response = eventService.update(updateRequestDto);

        // ASSERT
        assertEquals(403, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));

        // Yetki dışı işlemde veritabanı save işlemi asla tetiklenmemeli
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void changeStatus_success() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User owner = new User();
        owner.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // ACT
        ResponseEntity<Object> response = eventService.changeStatus(100L, com.works.entity.EventStatus.PUBLISHED);

        // ASSERT
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void changeStatus_fails_whenNotOwner() {
        // ARRANGE
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(2L); // Farklı bir kullanıcı

        User owner = new User();
        owner.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // ACT
        ResponseEntity<Object> response = eventService.changeStatus(100L, com.works.entity.EventStatus.PUBLISHED);

        // ASSERT
        assertEquals(403, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void getEventDetail_success() {
        // Arrange
        Event event = new Event();
        event.setId(100);
        event.setTitle("Özel Etkinlik");

        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // Act
        ResponseEntity<Object> response = eventService.getEventDetail(100L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        Event returnedEvent = (Event) body.get("event");
        assertEquals("Özel Etkinlik", returnedEvent.getTitle());
    }

    @Test
    void getEventDetail_fails_whenNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Object> response = eventService.getEventDetail(999L);

        // Assert
        assertEquals(404, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Etkinlik bulunamadı.", body.get("message"));
    }

    @Test
    void eventList_success_withPagination() {
        // Arrange
        Event event1 = new Event();
        event1.setTitle("Etkinlik 1");

        Event event2 = new Event();
        event2.setTitle("Etkinlik 2");

        // Sayfalanmış veri taklidi (Page taklidi) yapıyoruz
        List<Event> mockEventList = List.of(event1, event2);
        org.springframework.data.domain.Page<Event> mockPage = new PageImpl<>(mockEventList);

        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(mockPage);

        // Act
        org.springframework.data.domain.Page<Event> resultPage = eventService.eventList(0);

        // Assert
        assertNotNull(resultPage);
        assertEquals(2, resultPage.getContent().size());
        assertEquals("Etkinlik 1", resultPage.getContent().get(0).getTitle());
        assertEquals("Etkinlik 2", resultPage.getContent().get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getParticipants_success() {
        // Arrange
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        User owner = new User();
        owner.setId(1L);

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);
        event.setParticipants(new java.util.HashSet<>());
        event.getParticipants().add(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // Act
        ResponseEntity<Object> response = eventService.getParticipants(100L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertNotNull(body.get("participants"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getParticipants_fails_whenNotOwner() {
        // Arrange
        UserResponseDto sessionUser = new UserResponseDto();
        sessionUser.setId(2L); // Farklı bir kullanıcı sisteme girmiş

        User owner = new User();
        owner.setId(1L); // Etkinliğin asıl sahibi

        Event event = new Event();
        event.setId(100);
        event.setOwner(owner);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // Act
        ResponseEntity<Object> response = eventService.getParticipants(100L);

        // Assert
        assertEquals(403, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Bu etkinliğin katılımcı listesini görme yetkiniz bulunmuyor.", body.get("message"));
    }
}