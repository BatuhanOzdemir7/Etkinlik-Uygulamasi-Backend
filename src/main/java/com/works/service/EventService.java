package com.works.service;

import com.works.dto.EventCreateRequestDto;
import com.works.dto.EventUpdateRequestDto;
import com.works.dto.UserResponseDto;
import com.works.entity.Event;
import com.works.entity.EventStatus;
import com.works.entity.User;
import com.works.repository.EventRepository;
import com.works.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final ModelMapper modelMapper = new ModelMapper();

    public Event create(EventCreateRequestDto eventCreateRequestDto) {
        if (eventCreateRequestDto.getEventDate() != null && eventCreateRequestDto.getEventTime() != null) {
            if (eventCreateRequestDto.getEventDate().isEqual(LocalDate.now()) && eventCreateRequestDto.getEventTime().isBefore(LocalTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bugün için geçmiş bir saate etkinlik oluşturamazsınız.");
            }
        }
        Event event = modelMapper.map(eventCreateRequestDto, Event.class);
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<User> optionalUser = userRepository.findById(sessionUser.getId());
        if (optionalUser.isPresent()) {
            event.setOwner(optionalUser.get());
            event.getParticipants().add(optionalUser.get());
        }
        return eventRepository.save(event);
    }

    public List<Event> createAll(List<EventCreateRequestDto> eventCreateRequestDtos) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Etkinlik oluşturmak için oturum açmalısınız.");
        }
        User currentUser = userRepository.findById(sessionUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oturum açan kullanıcı bulunamadı."));
        List<Event> eventList = eventCreateRequestDtos.stream()
                .map(dto -> {
                    if (dto.getEventDate() != null && dto.getEventTime() != null) {
                        if (dto.getEventDate().isEqual(LocalDate.now()) && dto.getEventTime().isBefore(LocalTime.now())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bugün için geçmiş bir saate etkinlik oluşturamazsınız: " + dto.getTitle());
                        }
                    }
                    Event event = modelMapper.map(dto, Event.class);
                    event.setOwner(currentUser);
                    event.getParticipants().add(currentUser);
                    return event;
                })
                .toList();
        return eventRepository.saveAll(eventList);
    }

    public ResponseEntity<Object> deleteOne(Long id) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(id);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            if (!event.getOwner().getId().equals(sessionUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bu etkinliği silme yetkiniz bulunmuyor."));
            }
            eventRepository.deleteById(id);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Etkinlik başarıyla silindi."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı. ID: " + id));
    }

    public ResponseEntity<Object> update(EventUpdateRequestDto dto) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(dto.getId());

        if (optionalEvent.isPresent()) {
            Event existingEvent = optionalEvent.get();

            // Yetki kontrolü: sadece sahibi güncelleyebilir
            if (!existingEvent.getOwner().getId().equals(sessionUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Bu etkinliği düzenleme yetkiniz bulunmuyor."));
            }

            // Sadece düzenlenebilir alanları güncelle —
            // owner, participants ve status modelMapper ile EZİLMEZ
            existingEvent.setTitle(dto.getTitle());
            existingEvent.setDescription(dto.getDescription());
            existingEvent.setEventDate(dto.getEventDate());
            existingEvent.setEventTime(dto.getEventTime());
            existingEvent.setLocation(dto.getLocation());
            existingEvent.setCategory(dto.getCategory());

            eventRepository.save(existingEvent);
            return ResponseEntity.ok(Map.of("success", true, "message", "Etkinlik başarıyla güncellendi."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
    }

    public Page<Event> eventList(int page) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, PageRequest.of(page, 10));
    }

    public Page<Event> search(String q, int page, String sortDir) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                sortDir.equalsIgnoreCase("desc") ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC,
                "eventDate"
        );
        Pageable pageable = PageRequest.of(page, 10, sort);
        return eventRepository.searchActiveEvents(EventStatus.PUBLISHED, q, pageable);
    }

    public ResponseEntity<Object> joinEvent(Long eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            Optional<User> optionalUser = userRepository.findById(sessionUser.getId());
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
            }
            User user = optionalUser.get();
            if (event.getParticipants().contains(user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Bu etkinliğe zaten katıldınız."));
            }
            event.getParticipants().add(user);
            eventRepository.save(event);
            return ResponseEntity.ok(Map.of("success", true, "message", "Etkinliğe başarıyla katıldınız."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
    }

    public ResponseEntity<Object> leaveEvent(Long eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            Optional<User> optionalUser = userRepository.findById(sessionUser.getId());
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
            }
            User user = optionalUser.get();
            if (!event.getParticipants().contains(user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Bu etkinliğe zaten kayıtlı değilsiniz."));
            }
            event.getParticipants().remove(user);
            eventRepository.save(event);
            return ResponseEntity.ok(Map.of("success", true, "message", "Etkinlik katılımınız iptal edildi."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
    }

    public ResponseEntity<Object> getEventDetail(Long id) {
        return eventRepository.findById(id).map(event ->
                ResponseEntity.ok((Object) Map.of("success", true, "event", event))
        ).orElseGet(() ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı."))
        );
    }

    public ResponseEntity<Object> changeStatus(Long eventId, EventStatus newStatus) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            if (event.getOwner() == null || !event.getOwner().getId().equals(sessionUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bu etkinliğin durumunu değiştirme yetkiniz bulunmuyor."));
            }
            event.setStatus(newStatus);
            eventRepository.save(event);
            return ResponseEntity.ok(Map.of("success", true, "message", "Etkinlik durumu başarıyla güncellendi: " + newStatus));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
    }

    public Page<Event> getMyDrafts(int page) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Pageable pageable = PageRequest.of(page, 10);
        return eventRepository.findByOwnerIdAndStatus(sessionUser.getId(), EventStatus.DRAFT, pageable);
    }

    public Page<Event> getMyArchives(int page) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Pageable pageable = PageRequest.of(page, 10);
        return eventRepository.findByOwnerIdAndStatus(sessionUser.getId(), EventStatus.ARCHIVED, pageable);
    }

    public ResponseEntity<Object> getParticipants(Long eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            if (!event.getOwner().getId().equals(sessionUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bu etkinliğin katılımcı listesini görme yetkiniz bulunmuyor."));
            }
            return ResponseEntity.ok(Map.of("success", true, "participants", event.getParticipants()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
    }
}