package com.kauan.gamelog.auth.dto;

import com.kauan.gamelog.user.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RN09: a senha vai até 64 caracteres porque o BCrypt só considera os primeiros 72 bytes. */
public record RegisterRequest(
        @NotBlank(message = "informe um username") @ValidUsername
        String username,

        @NotBlank(message = "informe um e-mail")
        @Email(message = "e-mail inválido")
        @Size(max = 254, message = "e-mail longo demais")
        String email,

        @NotBlank(message = "informe uma senha") @Size(min = 8, max = 64, message = "use de 8 a 64 caracteres")
        String password) {}
