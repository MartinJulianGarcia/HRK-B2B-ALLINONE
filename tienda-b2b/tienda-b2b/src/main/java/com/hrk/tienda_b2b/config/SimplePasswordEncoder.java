package com.hrk.tienda_b2b.config;

import org.springframework.stereotype.Component;

@Component
public class SimplePasswordEncoder {
    
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString(); // No real encoding for now
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.toString().equals(encodedPassword);
    }
}
