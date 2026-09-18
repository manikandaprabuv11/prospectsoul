package com.vyoog.prospectsoul_backend.activity.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.activity.entity.Activity;
import com.vyoog.prospectsoul_backend.activity.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    /** Timeline composition: every activity on a company, newest first. */
    List<Activity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<Activity> findByCompanyIdAndTypeOrderByCreatedAtDesc(UUID companyId, ActivityType type);
}
