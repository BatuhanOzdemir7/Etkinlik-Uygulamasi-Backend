package com.works.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * DTO for {@link com.works.entity.User}
 */
@Data
public class UserRegisterRequestDto implements Serializable {
    @NotNull @NotEmpty
    @Size(min = 3, max = 50)
    String nickname;
    @NotNull
    @Size(min = 2, max = 100)
    @NotEmpty
    String name;
    @NotNull
    @Size(min = 2, max = 100)
    @NotEmpty
    String surname;
    @NotNull
    @Email
    @NotEmpty
    String email;
    @NotNull
    @Size(min = 9, max = 15)
    @NotEmpty
    @Pattern(regexp = "^(?:\\+90\\d{10}|0\\s?\\d{3}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}|\\d{10})(?:\\s\\+\\d+)?$")
    String phone;
    boolean enabled;
    @NotNull
    @Size(min = 6, max = 15)
    @NotEmpty
    String password;
}