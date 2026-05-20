package com.works.dto;

import lombok.Data;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.works.entity.User}
 */
@Data
public class UserResponseDto {
    Long id;
    String nickname;
    String name;
    String surname;
    String email;
    String phone;
}