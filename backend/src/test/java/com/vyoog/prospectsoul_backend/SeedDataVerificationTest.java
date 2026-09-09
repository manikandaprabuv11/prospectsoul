package com.vyoog.prospectsoul_backend;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SeedDataVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Keycloak keycloak;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_ADMIN"))
                .jwt(jwt -> jwt.subject("c3000000-0000-0000-0000-000000000001")
                        .claim("preferred_username", "admin"));
    }

    @Test
    void flywayMigrationsRanCleanly() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT count(*) FROM flyway_schema_history WHERE success = true")) {
            rs.next();
            int successfulMigrations = rs.getInt(1);
            assertThat(successfulMigrations).isGreaterThanOrEqualTo(6);
        }
    }

    @Test
    void sevenUsersExistInDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(7);
        }
    }

    @Test
    void fiveRolesExistInDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM roles")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(5);
        }
    }

    @Test
    void sevenUserRoleAssignmentsExist() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM user_roles")) {
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(7);
        }
    }

    @Test
    void auditLogTableExists() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT count(*) FROM information_schema.tables WHERE table_name = 'audit_log'")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
