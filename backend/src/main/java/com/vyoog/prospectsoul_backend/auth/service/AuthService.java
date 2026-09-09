package com.vyoog.prospectsoul_backend.auth.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    public void triggerPasswordReset(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(targetRealm)
                    .users()
                    .searchByEmail(email, true);

            if (users.isEmpty()) {
                log.debug("Password reset requested for unknown email");
                return;
            }

            UserRepresentation user = users.getFirst();
            keycloak.realm(targetRealm)
                    .users()
                    .get(user.getId())
                    .executeActionsEmail(List.of("UPDATE_PASSWORD"));

            log.info("Password reset email triggered for user {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to trigger password reset: {}", e.getMessage());
        }
    }
}
