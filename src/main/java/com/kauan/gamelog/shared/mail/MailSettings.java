package com.kauan.gamelog.shared.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param appUrl endereço do front, para os links dos e-mails */
@ConfigurationProperties("game-log.mail")
public record MailSettings(
        @DefaultValue("Game Log <nao-responda@gamelog.dev>") String from,
        @DefaultValue("http://localhost:5173") String appUrl) {}
