package com.works.repository;

import com.works.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmailEqualsOrPhoneEqualsAllIgnoreCase(String email, String phone);
    Optional<User> findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(String email, String phone);
    Optional<User> findByNicknameIgnoreCase(String nickname);
}