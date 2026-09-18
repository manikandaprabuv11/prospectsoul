package com.vyoog.prospectsoul_backend.activity.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.activity.entity.Activity;
import com.vyoog.prospectsoul_backend.activity.entity.ActivityType;
import com.vyoog.prospectsoul_backend.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes activity rows. Mirrors {@code AuditService}: callers must already be
 * inside the transaction that performs the domain mutation, so an activity can
 * never be committed for a mutation that rolled back.
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Activity record(UUID companyId, ActivityType type, Map<String, Object> content,
                           boolean humanVerified, String actor) {
        Activity activity = Activity.builder()
                .companyId(companyId)
                .type(type)
                .content(toJson(content))
                .verified(humanVerified)
                .createdBy(actor)
                .build();
        return activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<Activity> findForCompany(UUID companyId) {
        return activityRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    private String toJson(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JacksonException e) {
            return "{\"error\": \"serialization_failed\"}";
        }
    }
}
