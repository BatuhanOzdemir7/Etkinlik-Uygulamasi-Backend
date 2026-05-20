package com.works.controller;

import com.works.dto.UserRegisterRequestDto;
import com.works.entity.User;
import com.works.dto.UserLoginRequestDto;
import com.works.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserRestController {

    final UserService userService;

    @PostMapping("/register")
    public ResponseEntity register(@Valid @RequestBody UserRegisterRequestDto userRegisterRequestDto) {
        return userService.register(userRegisterRequestDto);
    }

    @PostMapping("/login")
    public ResponseEntity login(@Valid @RequestBody UserLoginRequestDto userLoginRequestDto) {
        return userService.login(userLoginRequestDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout() {
        return userService.logout();
    }

    /**
     * Oturum açmış kullanıcının kendi bilgilerini çekmesi için endpoint.
     * SessionFilter tarafından korunur — oturumsuz istekler 401 alır.
     * Response: { "success": true, "user": { id, name, surname, email, phone } }
     */
    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        return userService.me();
    }

    // Kendi tam profil sayfası — tüm etkinlikler dahil
    @GetMapping("/profile/me")
    public ResponseEntity<Object> profileMe() {
        return userService.getProfileMe();
    }

    // Başka kullanıcının profili — gizlilik filtreli
    @GetMapping("/profile/{id}")
    public ResponseEntity<Object> profileById(@PathVariable Long id) {
        return userService.getProfileById(id);
    }

    // Profil güncelleme — bio ve badge
    @PutMapping("/profile/update")
    public ResponseEntity<Object> updateProfile(@RequestBody Map<String, String> body) {
        return userService.updateProfile(body.get("bio"), body.get("badge"));
    }
}