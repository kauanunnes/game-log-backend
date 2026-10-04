package com.kauan.gamelog.shared;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erro de tipo (ex.: {@code ?sort=aleatorio}) é 400; regra de validação é 422. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        boolean onlyTypeErrors =
                ex.getGlobalErrors().isEmpty() && ex.getFieldErrors().stream().allMatch(FieldError::isBindingFailure);
        HttpStatus result = onlyTypeErrors ? HttpStatus.BAD_REQUEST : HttpStatus.UNPROCESSABLE_CONTENT;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                result, onlyTypeErrors ? "Algum parâmetro tem o tipo errado." : "Confira os campos indicados.");
        problem.setTitle(onlyTypeErrors ? "Requisição inválida" : "Dados inválidos");
        problem.setProperty(
                "errors",
                ex.getFieldErrors().stream()
                        .map(error -> new FieldIssue(
                                error.getField(),
                                error.isBindingFailure() ? "valor inválido" : error.getDefaultMessage()))
                        .toList());
        return handleExceptionInternal(ex, problem, headers, result, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O corpo da requisição não é um JSON válido para esta rota.");
        problem.setTitle("Requisição inválida");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleUnauthenticated(AuthenticationException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar.");
        problem.setTitle("Não autenticado");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleForbidden(AccessDeniedException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Você não tem permissão para isso.");
        problem.setTitle("Sem permissão");
        return problem;
    }

    /** O {@code traceId} volta na resposta: quem relatar o erro passa o código, e o caso aparece no log. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Algo deu errado. Tente de novo em instantes.");
        problem.setTitle("Erro inesperado");
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }
}
