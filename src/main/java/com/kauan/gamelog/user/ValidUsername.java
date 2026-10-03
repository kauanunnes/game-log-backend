package com.kauan.gamelog.user;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** RN08: de 3 a 20 letras, números ou _, sem diferenciar maiúsculas; nomes de rotas do site são reservados. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUsername.Validator.class)
public @interface ValidUsername {
    String message() default "use de 3 a 20 letras, números ou _";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidUsername, String> {
        private static final Pattern FORMAT = Pattern.compile("[a-z0-9_]{3,20}");
        private static final Set<String> RESERVED = Set.of(
                "about",
                "admin",
                "administrator",
                "api",
                "app",
                "auth",
                "explore",
                "feed",
                "game",
                "gamelog",
                "games",
                "help",
                "login",
                "logout",
                "me",
                "null",
                "register",
                "root",
                "settings",
                "signup",
                "support",
                "user",
                "users");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true; // o @NotBlank de quem usa cuida disso
            }
            String username = value.trim().toLowerCase(Locale.ROOT);
            if (!FORMAT.matcher(username).matches()) {
                return false;
            }
            if (!RESERVED.contains(username)) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("esse username é reservado")
                    .addConstraintViolation();
            return false;
        }
    }
}
