package web;

import org.springframework.security.crypto.password.PasswordEncoder;
import security.PasswordHasher;

public class WebPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordHasher.hash(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return PasswordHasher.matches(rawPassword.toString(), encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return PasswordHasher.needsUpgrade(encodedPassword);
    }
}
