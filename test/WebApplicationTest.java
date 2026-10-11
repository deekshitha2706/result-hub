import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import security.PasswordHasher;
import web.WebApplication;

@SpringBootTest(classes = WebApplication.class, properties = {
        "DB_URL=jdbc:h2:mem:result_hub_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "BOOTSTRAP_ADMIN_PASSWORD=test-admin-password-123"
})
@AutoConfigureMockMvc
class WebApplicationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void loginPageIsPublicAndAdminDashboardRequiresAuthentication() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminCanViewDashboardButCannotOpenStudentPortal() throws Exception {
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/student/results").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanViewOwnPortalButCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/student/results").with(user("ST-101").roles("STUDENT")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/students").with(user("ST-101").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyPasswordsAreVerifiedAndNewPasswordsAreHashed() {
        String hash = PasswordHasher.hash("change-me-123");
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.matches("change-me-123", hash));
        org.junit.jupiter.api.Assertions.assertFalse(PasswordHasher.matches("wrong-password", hash));
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.matches("legacy", "legacy"));
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.needsUpgrade("legacy"));
    }

    @Test
    void loginUsesTheConfiguredInitialAdminAccount() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "test-admin-password-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void adminCanCreateStudentCourseAndResultForStudentPortal() throws Exception {
        mvc.perform(post("/admin/students").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("rollNumber", "WEB-ST-101")
                        .param("firstName", "Web")
                        .param("lastName", "Student")
                        .param("dateOfBirth", "2004-12-26"))
                .andExpect(status().is3xxRedirection());
        String storedPassword = jdbc.queryForObject(
                "SELECT password FROM users WHERE username = ?", String.class, "WEB-ST-101");
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.matches("26122004", storedPassword));
        org.junit.jupiter.api.Assertions.assertFalse(PasswordHasher.needsUpgrade(storedPassword));

        mvc.perform(post("/admin/courses").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("courseCode", "WEB101")
                        .param("courseName", "Web Course")
                        .param("credits", "3")
                        .param("semester", "1"))
                .andExpect(status().is3xxRedirection());
        long courseId = jdbc.queryForObject(
                "SELECT course_id FROM courses WHERE course_code = ?", Long.class, "WEB101");

        mvc.perform(post("/admin/results").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("rollNumber", "WEB-ST-101")
                        .param("courseId", String.valueOf(courseId))
                        .param("marksObtained", "92")
                        .param("totalMarks", "100")
                        .param("examDate", LocalDate.now().toString())
                        .param("semester", "1")
                        .param("academicYear", "2026-27"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/student/results").with(user("WEB-ST-101").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Web Course")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("A+")));
    }

    @Test
    void successfulLegacyLoginUpgradesStoredPasswordHash() throws Exception {
        jdbc.update("INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                "legacy-admin", "legacy-password-123", "ADMIN");
        mvc.perform(post("/login").with(csrf())
                        .param("username", "legacy-admin")
                        .param("password", "legacy-password-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
        String storedPassword = jdbc.queryForObject(
                "SELECT password FROM users WHERE username = ?", String.class, "legacy-admin");
        org.junit.jupiter.api.Assertions.assertFalse(PasswordHasher.needsUpgrade(storedPassword));
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.matches("legacy-password-123", storedPassword));
    }

    @Test
    void studentCanChangeTheirPasswordWithCurrentPasswordVerification() throws Exception {
        jdbc.update("INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                "password-student", PasswordHasher.hash("current-password-123"), "STUDENT");
        mvc.perform(post("/student/account/password").with(user("password-student").roles("STUDENT")).with(csrf())
                        .param("currentPassword", "current-password-123")
                        .param("newPassword", "replacement-password-123")
                        .param("confirmPassword", "replacement-password-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/account"));
        String storedPassword = jdbc.queryForObject(
                "SELECT password FROM users WHERE username = ?", String.class, "password-student");
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.matches("replacement-password-123", storedPassword));
        org.junit.jupiter.api.Assertions.assertFalse(PasswordHasher.matches("current-password-123", storedPassword));
    }
}
