package web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.env.Environment;

@Configuration
public class WebSecurityConfig {
    @Bean
    public javax.sql.DataSource dataSource(Environment environment) {
        String url = environment.getProperty("DB_URL");
        String host = environment.getProperty("DB_HOST");
        String username = environment.getProperty("DB_USER", "sa");
        String password = environment.getProperty("DB_PASSWORD", "");

        if (url == null || url.isBlank()) {
            if (host == null || host.isBlank()) {
                url = "jdbc:h2:mem:result_hub;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
            } else {
                String port = environment.getProperty("DB_PORT", "5432");
                String database = environment.getProperty("DB_NAME", "result_hub");
                url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                username = environment.getRequiredProperty("DB_USER");
                password = environment.getRequiredProperty("DB_PASSWORD");
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(10_000);
        return new HikariDataSource(config);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new WebPasswordEncoder();
    }

    @Bean
    public UserDetailsPasswordService userDetailsPasswordService(WebRepository repository) {
        return (user, newPassword) -> {
            repository.updatePassword(user.getUsername(), newPassword);
            return User.withUserDetails(user).password(newPassword).build();
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(WebRepository repository,
                                                              PasswordEncoder passwordEncoder,
                                                              UserDetailsPasswordService passwordService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(username -> repository.findUser(username)
                .map(account -> {
                    String role = "STUDENT".equalsIgnoreCase(account.getRole()) ? "STUDENT" : "ADMIN";
                    return User.withUsername(account.getUsername())
                            .password(account.getPassword())
                            .roles(role)
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password.")));
        provider.setUserDetailsPasswordService(passwordService);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    DaoAuthenticationProvider authenticationProvider) throws Exception {
        http.authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
