package com.kauan.gamelog.user;

import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import com.kauan.gamelog.user.dto.ChangePasswordRequest;
import com.kauan.gamelog.user.dto.DeleteAccountRequest;
import com.kauan.gamelog.user.dto.MeDTO;
import com.kauan.gamelog.user.dto.ProfileForm;
import com.kauan.gamelog.user.dto.UpdateSettingsRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/me")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class MeController {
    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeDTO me(@CurrentUserId Long userId) {
        return userService.getMe(userId);
    }

    /** JSON Merge Patch: envie só o que muda; {@code null} limpa nome, bio e gênero. */
    @PatchMapping
    public MeDTO updateProfile(
            @CurrentUserId Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(implementation = ProfileForm.class)))
                    @RequestBody
                    JsonNode patch) {
        return userService.updateProfile(userId, patch);
    }

    @PatchMapping("/settings")
    public MeDTO updateSettings(@CurrentUserId Long userId, @Valid @RequestBody UpdateSettingsRequest changes) {
        return userService.updateSettings(userId, changes);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUserId Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @Valid @RequestBody DeleteAccountRequest request) {
        userService.delete(userId, request.password());
    }
}
