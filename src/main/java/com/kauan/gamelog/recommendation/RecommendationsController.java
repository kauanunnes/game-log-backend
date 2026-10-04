package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.recommendation.dto.RecommendationsDTO;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class RecommendationsController {
    private final Recommendations recommendations;

    public RecommendationsController(Recommendations recommendations) {
        this.recommendations = recommendations;
    }

    /** Calculadas na hora: são algumas buscas de vizinhos, sem chamar nenhum modelo. */
    @GetMapping("/me/recommendations")
    public RecommendationsDTO mine(@CurrentUserId Long userId) {
        return recommendations.forUser(userId);
    }
}
