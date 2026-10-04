package com.kauan.gamelog.shared.mail;

/** Um e-mail de texto simples. */
public record Mail(String to, String subject, String text) {}
