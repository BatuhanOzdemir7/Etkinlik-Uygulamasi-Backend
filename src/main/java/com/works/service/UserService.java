package com.works.service;

import com.works.dto.UserProfileDto;
import com.works.entity.Event;
import com.works.entity.EventStatus;
import com.works.entity.User;
import com.works.repository.EventRepository;
import com.works.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.mindrot.jbcrypt.BCrypt;
import org.modelmapper.ModelMapper;
import com.works.dto.UserRegisterRequestDto;
import com.works.dto.UserLoginRequestDto;
import com.works.dto.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    final UserRepository userRepository;
    final EventRepository eventRepository;
    final HttpServletRequest request;
    ModelMapper modelMapper = new ModelMapper();

    public ResponseEntity register(UserRegisterRequestDto userRegisterRequestDto) {
        if (request.getSession().getAttribute("user") != null) {
            return ResponseEntity.<Object>status(400).body(Map.of(
                    "success", false,
                    "message", "Zaten giriş yapmış durumdasınız. Yeni kayıt açmak için lütfen önce çıkış (logout) yapın."
            ));
        }
        List<User> UserList = userRepository.findByEmailEqualsOrPhoneEqualsAllIgnoreCase(userRegisterRequestDto.getEmail(), userRegisterRequestDto.getPhone());
        if (UserList.size() > 0) {
            Map<String, Object> hm = Map.of("success", false, "message", "This email or phone number is already in use.");
            return ResponseEntity.badRequest().body(hm);
        }
        User user = modelMapper.map(userRegisterRequestDto, User.class);
        String hashPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashPassword);
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok().body(user);
    }

    public ResponseEntity login(UserLoginRequestDto UserLoginRequestDto) {
        if (request.getSession().getAttribute("user") != null) {
            return ResponseEntity.<Object>status(400).body(Map.of(
                    "success", false,
                    "message", "Sistemde zaten aktif bir oturumunuz bulunuyor."
            ));
        }
        Optional<User> optionalUser = userRepository.findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(UserLoginRequestDto.getUsername(), UserLoginRequestDto.getUsername());
        if (optionalUser.isPresent()) {
            User User = optionalUser.get();
            boolean isMatch = BCrypt.checkpw(UserLoginRequestDto.getPassword(), User.getPassword());
            if (isMatch) {
                UserResponseDto userResponseDto = modelMapper.map(User, UserResponseDto.class);
                request.getSession().setAttribute("user", userResponseDto);
                return ResponseEntity.ok().body(userResponseDto);
            }
        }
        Map<String, Object> hm = Map.of("success", false, "message", "Username or password is incorrect.");
        return ResponseEntity.badRequest().body(hm);
    }

    public ResponseEntity<Object> logout() {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> hm = Map.of("success", true, "message", "Başarıyla çıkış yapıldı.");
        return ResponseEntity.ok().body(hm);
    }

    /**
     * Oturum açmış kullanıcının bilgilerini döndürür.
     * SessionFilter /user/me'yi koruduğu için buraya sadece
     * geçerli oturumu olan istekler ulaşabilir.
     */
    public ResponseEntity<Object> me() {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        // SessionFilter zaten null kontrolü yapıyor ama defansif olarak tekrar kontrol ediyoruz
        if (sessionUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Oturum bulunamadı."));
        }
        return ResponseEntity.ok(Map.of("success", true, "user", sessionUser));
    }

    /**
     * Giriş yapan kullanıcının TAM profil bilgisi.
     * Taslaklar ve arşivler dahil tüm etkinlikler döner.
     */
    public ResponseEntity<Object> getProfileMe() {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<User> optionalUser = userRepository.findById(sessionUser.getId());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
        }
        User user = optionalUser.get();

        // Sahibi olduğu TÜM etkinlikler (TASLAK, YAYINDA, ARŞİVLENDİ)
        List<Event> hostedEvents = eventRepository.findByOwnerId(user.getId());

        // Katılımcı olduğu etkinlikler
        List<Event> joinedEvents = eventRepository.findByParticipantsId(user.getId());

        UserProfileDto profile = new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getPhone(),
                user.getBio(),
                user.getBadge(),
                hostedEvents.size(),
                joinedEvents.size(),
                hostedEvents,
                joinedEvents
        );

        return ResponseEntity.ok(Map.of("success", true, "profile", profile));
    }

    /**
     * Başka bir kullanıcının profili.
     * Gizlilik filtresi: sadece YAYINDA etkinlikler döner.
     */
    public ResponseEntity<Object> getProfileById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
        }
        User user = optionalUser.get();

        // Gizlilik filtresi: sadece yayındaki etkinlikler
        List<Event> hostedEvents = eventRepository.findByOwnerIdAndStatusIn(
                user.getId(), List.of(EventStatus.PUBLISHED)
        );
        List<Event> joinedEvents = eventRepository.findByParticipantsId(user.getId());

        UserProfileDto profile = new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getPhone(),
                user.getBio(),
                user.getBadge(),
                hostedEvents.size(),
                joinedEvents.size(),
                hostedEvents,
                joinedEvents
        );

        return ResponseEntity.ok(Map.of("success", true, "profile", profile));
    }

    /**
     * Kullanıcının bio ve badge bilgisini günceller.
     * Sadece kendi profilini güncelleyebilir.
     */
    public ResponseEntity<Object> updateProfile(String bio, String badge) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Optional<User> optionalUser = userRepository.findById(sessionUser.getId());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Kullanıcı bulunamadı."));
        }
        User user = optionalUser.get();
        if (bio != null) user.setBio(bio);
        if (badge != null) user.setBadge(badge);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profil güncellendi."));
    }
}