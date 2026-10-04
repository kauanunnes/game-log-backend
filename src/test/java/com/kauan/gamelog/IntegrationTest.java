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
 * fica desligado mesmo que exista um {@code .env} com credenciais, e os e-mails ficam em memória.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        properties = {
            "game-log.igdb.client-id=",
            "game-log.igdb.client-secret=",
            // BCrypt no custo 12 deixaria cada cadastro e login dos testes lento
            "game-log.auth.bcrypt-strength=4"
        })
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingMailer.class})
public @interface IntegrationTest {}
