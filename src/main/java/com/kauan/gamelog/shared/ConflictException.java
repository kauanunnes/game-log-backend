package com.kauan.gamelog.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ConflictException extends ErrorResponseException {
    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT);
        setTitle("Conflito");
        setDetail(detail);
    }
}
