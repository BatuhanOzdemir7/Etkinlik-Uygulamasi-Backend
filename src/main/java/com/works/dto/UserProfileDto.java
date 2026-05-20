package com.works.dto;

import com.works.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Profil sayfası için kullanıcı + etkinlik geçmişi DTO'su.
 * /user/profile/me ve /user/profile/{id} endpoint'lerinde kullanılır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    // Temel kullanıcı bilgileri
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String phone;
    private String bio;
    private String badge;

    // İstatistikler
    private int hostedCount;
    private int joinedCount;

    // Etkinlik listeleri
    // Kendi profilinde: tüm statuslar görünür
    // Başkasının profilinde: sadece YAYINDA olanlar
    private List<Event> hostedEvents;
    private List<Event> joinedEvents;
}