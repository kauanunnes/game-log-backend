package com.kauan.gamelog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Contexto completo, MockMvc e PostgreSQL 18 num container compartilhado entre as classes de teste. O IGDB
 * fica desligado mesmo que exista um {@code .env} com credenciais, os e-mails ficam em memória e os embeddings saem de
 * um modelo falso.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {
            "game-log.igdb.client-id=",
            "game-log.igdb.client-secret=",
            // BCrypt no custo 12 deixaria cada cadastro e login dos testes lento
            "game-log.auth.bcrypt-strength=4",
            // Nada de baixar o modelo do perfil local: os testes usam o FakeEmbeddingModel
            "spring.ai.model.embedding=none",
            "game-log.embeddings.model=teste",
            // Nada de chamar o Claude de verdade: os testes usam o FakeCurator
            "game-log.ai.api-key="
        })
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingMailer.class, FakeEmbeddingModel.class, FakeCurator.class})
public @interface IntegrationTest {}
