package com.kauan.gamelog.shared;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** Regra de negócio violada (422), com um código estável e os campos envolvidos. */
public class UnprocessableException extends ErrorResponseException {
    public UnprocessableException(String code, String detail, List<FieldIssue> errors) {
        super(HttpStatus.UNPROCESSABLE_CONTENT);
        setTitle("Dados inválidos");
        setDetail(detail);
        getBody().setProperty("code", code);
        getBody().setProperty("errors", errors);
    }

    public static UnprocessableException field(String code, String field, String message) {
        return new UnprocessableException(code, message, List.of(new FieldIssue(field, message)));
    }
}
