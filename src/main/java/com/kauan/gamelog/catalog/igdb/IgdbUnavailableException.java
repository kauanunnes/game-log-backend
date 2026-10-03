package com.kauan.gamelog.catalog.igdb;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

/** 503 quando o IGDB não está configurado; 502 quando ele falha. */
public class IgdbUnavailableException extends ErrorResponseException {
    public IgdbUnavailableException(HttpStatusCode status, String detail) {
        super(status);
        setTitle("IGDB indisponível");
        setDetail(detail);
    }
}
