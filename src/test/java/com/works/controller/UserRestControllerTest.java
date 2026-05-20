package com.works.controller;

import com.works.dto.UserResponseDto;
import com.works.entity.Event;
import com.works.entity.EventStatus;
import com.works.entity.User;
import com.works.repository.EventRepository;
import com.works.repository.UserRepository;
import com.works.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    // HATANIN ÇÖZÜLDÜĞÜ YER: Artık Controller'ı değil, Service sınıfını test ediyoruz
    @InjectMocks
    private UserService userService;

    private User targetUser;
    private UserResponseDto sessionUser;
    private Event draftEvent;
    private Event publishedEvent;

    @BeforeEach
    void setUp() {
        targetUser = new User();
        targetUser.setId(1L);
        targetUser.setNickname("batuhanozdemir");

        sessionUser = new UserResponseDto();
        sessionUser.setId(1L);

        draftEvent = new Event();
        draftEvent.setId(100);
        draftEvent.setStatus(EventStatus.DRAFT);

        publishedEvent = new Event();
        publishedEvent.setId(101);
        publishedEvent.setStatus(EventStatus.PUBLISHED);
    }

    @Test
    void testGetProfile_UserNotFound_Returns404() {
        when(userRepository.findByNicknameIgnoreCase("olmayankisi")).thenReturn(Optional.empty());

        // Metodu userService üzerinden çağırıyoruz
        ResponseEntity<Object> response = userService.getProfile("olmayankisi");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testGetProfile_OwnProfile_ReturnsAllEvents() {
        when(userRepository.findByNicknameIgnoreCase("batuhanozdemir")).thenReturn(Optional.of(targetUser));

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        List<Event> allEvents = List.of(draftEvent, publishedEvent);
        when(eventRepository.findByOwnerId(targetUser.getId())).thenReturn(allEvents);
        when(eventRepository.findByParticipantsId(targetUser.getId())).thenReturn(List.of());

        // Metodu userService üzerinden çağırıyoruz
        ResponseEntity<Object> response = userService.getProfile("batuhanozdemir");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository, times(1)).findByOwnerId(targetUser.getId());
        verify(eventRepository, never()).findByOwnerIdAndStatusIn(anyLong(), anyList());
    }

    @Test
    void testGetProfile_OtherUserProfile_ReturnsOnlyPublishedEvents() {
        when(userRepository.findByNicknameIgnoreCase("baskabiri")).thenReturn(Optional.of(targetUser));

        UserResponseDto otherSessionUser = new UserResponseDto();
        otherSessionUser.setId(99L);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(otherSessionUser);

        List<Event> publishedEventsOnly = List.of(publishedEvent);
        when(eventRepository.findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED)))
                .thenReturn(publishedEventsOnly);
        when(eventRepository.findByParticipantsId(targetUser.getId())).thenReturn(List.of());

        // Metodu userService üzerinden çağırıyoruz
        ResponseEntity<Object> response = userService.getProfile("baskabiri");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository, times(1)).findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED));
        verify(eventRepository, never()).findByOwnerId(anyLong());
    }

    @Test
    void testGetProfile_AnonymousVisitor_ReturnsOnlyPublishedEvents() {
        // Hazırlık: Kullanıcı veritabanında var
        when(userRepository.findByNicknameIgnoreCase("baskabiri")).thenReturn(Optional.of(targetUser));

        // Hazırlık: Ziyaretçinin hiçbir oturumu yok (request.getSession(false) null dönüyor)
        when(request.getSession(false)).thenReturn(null);

        // Hazırlık: Yalnızca yayınlanmış etkinliklerin dönmesi
        List<Event> publishedEventsOnly = List.of(publishedEvent);
        when(eventRepository.findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED)))
                .thenReturn(publishedEventsOnly);
        when(eventRepository.findByParticipantsId(targetUser.getId())).thenReturn(List.of());

        // Eylem
        ResponseEntity<Object> response = userService.getProfile("baskabiri");

        // Doğrulama
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository, times(1)).findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED));
    }

    @Test
    void testGetProfile_SessionExistsButUserAttributeNull_ReturnsOnlyPublishedEvents() {
        // Hazırlık: Kullanıcı veritabanında var
        when(userRepository.findByNicknameIgnoreCase("baskabiri")).thenReturn(Optional.of(targetUser));

        // Hazırlık: Oturum var ancak "user" attribute'u null (Süresi dolmuş oturum durumu)
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        List<Event> publishedEventsOnly = List.of(publishedEvent);
        when(eventRepository.findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED)))
                .thenReturn(publishedEventsOnly);
        when(eventRepository.findByParticipantsId(targetUser.getId())).thenReturn(List.of());

        // Eylem
        ResponseEntity<Object> response = userService.getProfile("baskabiri");

        // Doğrulama
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository, times(1)).findByOwnerIdAndStatusIn(targetUser.getId(), List.of(EventStatus.PUBLISHED));
    }

    @Test
    void testGetProfile_UserHasNoEvents_ReturnsEmptyLists() {
        // Hazırlık: Kendi profili senaryosu
        when(userRepository.findByNicknameIgnoreCase("batuhanozdemir")).thenReturn(Optional.of(targetUser));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);

        // Hazırlık: Veritabanı boş listeler dönüyor
        when(eventRepository.findByOwnerId(targetUser.getId())).thenReturn(List.of());
        when(eventRepository.findByParticipantsId(targetUser.getId())).thenReturn(List.of());

        // Eylem
        ResponseEntity<Object> response = userService.getProfile("batuhanozdemir");

        // Doğrulama
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        com.works.dto.UserProfileDto profile = (com.works.dto.UserProfileDto) body.get("profile");

// DTO'daki sayı (count) alanlarını çağırmak yerine doğrudan dönen listelerin boyutunu ölçerek testi güvenli hale getiriyoruz
        assertEquals(0, profile.getHostedEvents().size());
        assertEquals(0, profile.getJoinedEvents().size());
        assertTrue(profile.getHostedEvents().isEmpty());
        assertTrue(profile.getJoinedEvents().isEmpty());
    }
}