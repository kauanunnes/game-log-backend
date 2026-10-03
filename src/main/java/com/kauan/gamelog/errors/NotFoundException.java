package com.kauan.gamelog.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class NotFoundException extends ErrorResponseException {
    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND);
        setTitle("Não encontrado");
        setDetail(detail);
    }
}
