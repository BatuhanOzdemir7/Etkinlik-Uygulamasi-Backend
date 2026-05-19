package com.works.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Set;
import java.util.HashSet;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "event_date")
    @JdbcTypeCode(SqlTypes.DATE)
    private LocalDate eventDate;

    @Column(name = "event_time")
    @JdbcTypeCode(SqlTypes.TIME)
    private LocalTime eventTime;

    @Column(name = "location")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String location;

    @Column(name = "category")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String category;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "event_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT; // Yeni oluşturulan etkinlikler varsayılan olarak "DRAFT" başlar.

}