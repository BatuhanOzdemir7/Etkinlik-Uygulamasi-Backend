package com.works.controller;

import com.works.dto.EventCreateRequestDto;
import com.works.dto.EventUpdateRequestDto;
import com.works.entity.Event;
import com.works.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("event")
@RequiredArgsConstructor
public class EventRestController {

    final EventService eventService;

    @PostMapping("create")
    public Event create(@Valid @RequestBody EventCreateRequestDto eventCreateRequestDto){
        return eventService.create(eventCreateRequestDto);
    }

    @PostMapping("createAll")
    public List<Event> createAll(@Valid @RequestBody List<EventCreateRequestDto> eventCreateRequestDto){
        return eventService.createAll(eventCreateRequestDto);
    }

    @DeleteMapping("deleteOne/{id}")
    public ResponseEntity deleteOne(@PathVariable Long id){
        return eventService.deleteOne(id);
    }

    @PutMapping("update")
    public ResponseEntity update(@Valid @RequestBody EventUpdateRequestDto eventUpdateRequestDto){
        return eventService.update(eventUpdateRequestDto);
    }

    @GetMapping("list")
    public Page<Event> eventList(
            @RequestParam(defaultValue = "0") int page
    ){
        return eventService.eventList(page);
    }

    @GetMapping("search")
    public Page<Event> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "asc") String sortDir
    ){
        return eventService.search(q, page, sortDir);
    }

    @PostMapping("join/{id}")
    public ResponseEntity<Object> joinEvent(@PathVariable Long id){
        return eventService.joinEvent(id);
    }

    @DeleteMapping("leave/{id}")
    public ResponseEntity<Object> leaveEvent(@PathVariable Long id){
        return eventService.leaveEvent(id);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Object> getEventDetail(@PathVariable Long id){
        return eventService.getEventDetail(id);
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<Object> changeStatus(@PathVariable Long id, @RequestParam com.works.entity.EventStatus status) {
        return eventService.changeStatus(id, status);
    }

    @GetMapping("/my-drafts")
    public Page<Event> getMyDrafts(@RequestParam(defaultValue = "0") int page) {
        return eventService.getMyDrafts(page);
    }

    @GetMapping("/my-archives")
    public Page<Event> getMyArchives(@RequestParam(defaultValue = "0") int page) {
        return eventService.getMyArchives(page);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<Object> getParticipants(@PathVariable Long id) {
        return eventService.getParticipants(id);
    }

    @GetMapping("/control")
    public ResponseEntity<Map<String, Object>> control() {
        Map<String, Object> body = Map.of(
                "success", true,
                "message", "Session is valid."
        );
        return ResponseEntity.ok(body);
    }
}