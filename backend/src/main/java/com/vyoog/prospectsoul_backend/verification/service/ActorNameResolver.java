package com.vyoog.prospectsoul_backend.verification.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.entity.UserEntity;
import com.vyoog.prospectsoul_backend.admin.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns the Keycloak subject ids stored in {@code created_by} /
 * {@code verified_by} / {@code requested_by} into display names, using the
 * local {@code users} mirror.
 *
 * <p>The Verify screens show people's names ("Mani"), not subject UUIDs, but
 * the actor columns deliberately keep the id: the id is the durable identity,
 * and a renamed user must not rewrite history. Resolution therefore happens at
 * read time, once per page, and falls back to the raw id when the actor is not
 * a known user (an import job, a deactivated account, a seeded row).
 */
@Component
@RequiredArgsConstructor
public class ActorNameResolver {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, String> resolve(Collection<String> actorIds) {
        Set<UUID> ids = new HashSet<>();
        for (String actorId : actorIds) {
            if (actorId == null || actorId.isBlank()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(actorId));
            } catch (IllegalArgumentException e) {
                // Not a subject id (e.g. a system actor label); no name to resolve.
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, String> names = new HashMap<>();
        List<UserEntity> users = userRepository.findAllById(ids);
        for (UserEntity user : users) {
            names.put(user.getId().toString(), user.getFullName());
        }
        return names;
    }

    /** Convenience for a single actor. */
    @Transactional(readOnly = true)
    public String resolveOne(String actorId) {
        return nameOf(resolve(List.of(actorId == null ? "" : actorId)), actorId);
    }

    /** Resolved name, or the raw id when it cannot be resolved. */
    public static String nameOf(Map<String, String> names, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return null;
        }
        return names.getOrDefault(actorId, actorId);
    }
}
