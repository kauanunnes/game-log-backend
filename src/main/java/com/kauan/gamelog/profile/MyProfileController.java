package com.kauan.gamelog.profile;

import com.kauan.gamelog.profile.dto.ProfileDTO;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/profile")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class MyProfileController {
    private final ProfileService profileService;

    public MyProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** O mesmo cabeçalho de {@code /users/{username}}, completo mesmo com o perfil privado. */
    @GetMapping
    public ProfileDTO profile(@CurrentUserId Long userId) {
        return profileService.mine(userId);
    }
}
