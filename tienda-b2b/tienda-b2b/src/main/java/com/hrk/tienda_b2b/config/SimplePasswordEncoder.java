package com.hrk.tienda_b2b.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SimplePasswordEncoder {

    private final PasswordEncoder delegate = new BCryptPasswordEncoder();

    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }

        if (isBcryptHash(encodedPassword)) {
            return delegate.matches(rawPassword, encodedPassword);
        }

        return rawPassword.toString().equals(encodedPassword);
    }

    public boolean needsMigration(String encodedPassword) {
        return encodedPassword != null && !isBcryptHash(encodedPassword);
    }

    private boolean isBcryptHash(String encodedPassword) {
        return encodedPassword.startsWith("$2a$") ||
               encodedPassword.startsWith("$2b$") ||
               encodedPassword.startsWith("$2y$");
    }
}
