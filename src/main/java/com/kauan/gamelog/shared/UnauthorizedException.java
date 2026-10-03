package com.kauan.gamelog.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class UnauthorizedException extends ErrorResponseException {
    public UnauthorizedException(String detail) {
        super(HttpStatus.UNAUTHORIZED);
        setTitle("Não autenticado");
        setDetail(detail);
    }
}
