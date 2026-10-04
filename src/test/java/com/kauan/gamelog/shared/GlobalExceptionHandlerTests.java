package com.kauan.gamelog.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Os testes de integração rodam sem tracing, então o traceId entra aqui direto no MDC. */
class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void anUnexpectedErrorCarriesTheTraceId() {
        MDC.put("traceId", "4bf92f3577b34da6");

        var problem = handler.handleUnexpected(new IllegalStateException("teste"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getProperties()).containsEntry("traceId", "4bf92f3577b34da6");
    }

    @Test
    void withoutTracingTheResponseHasNoTraceId() {
        var problem = handler.handleUnexpected(new IllegalStateException("teste"));

        assertThat(problem.getProperties()).isNull();
    }
}
