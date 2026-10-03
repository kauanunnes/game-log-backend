package com.kauan.gamelog.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ForbiddenException extends ErrorResponseException {
    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN);
        setTitle("Sem permissão");
        setDetail(detail);
    }
}
