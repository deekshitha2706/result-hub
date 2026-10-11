package web;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WebBootstrap implements ApplicationRunner {
    private final WebRepository repository;
    private final PasswordEncoder passwords;
    private final Environment environment;

    public WebBootstrap(WebRepository repository, PasswordEncoder passwords, Environment environment) {
        this.repository = repository;
        this.passwords = passwords;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.countUsers() != 0) return;

        String password = environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD");
        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Set BOOTSTRAP_ADMIN_PASSWORD to a value of at least 12 characters before starting an empty database.");
        }
        String username = environment.getProperty("BOOTSTRAP_ADMIN_USERNAME", "admin");
        if (username.isBlank()) throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME must not be blank.");
        repository.createUser(username, passwords.encode(password), "ADMIN");
    }
}
