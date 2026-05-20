package com.works.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
/*
H2 veritabanında "USER" kelimesi sistemin kendi işleyişi
için ayrılmış özel bir komut (reserved keyword) olduğundan,
veritabanı doğrudan bu isimde bir tablo oluşturmaya
çalıştığında SQL sözdizimi hatası vererek çöker.
Bu çakışmayı aşmak ve uygulamanın sorunsuz çalışmasını
sağlamak için @Table(name = "users") anotasyonunu kullanarak
tabloya sistem komutlarıyla karışmayacak güvenli bir isim atarız.
 */
@Data
public class User {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String surname;

    @Column(unique = true, length = 200)
    private String email;

    @Column(unique = true, length = 15)
    private String phone;

    private boolean enabled;

    @JsonIgnore // Bu anotasyon şifrenin JSON olarak dışarı sızmasını engeller
    @Column(length = 1000)
    private String password;

    // Kullanıcının profil biyografisi — boş başlar, profil düzenleme ile güncellenir
    @Column(length = 500)
    private String bio;

    // Kullanıcının sisteme göre atanan unvanı: NET_RUNNER, SİSTEM_MİMARI vb.
    // Oluşturduğu/katıldığı etkinlik sayısına göre otomatik hesaplanabilir
    // ya da kullanıcı tarafından seçilebilir
    @Column(length = 100)
    private String badge;
}