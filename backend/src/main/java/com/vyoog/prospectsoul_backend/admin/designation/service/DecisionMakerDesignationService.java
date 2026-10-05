package com.vyoog.prospectsoul_backend.admin.designation.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vyoog.prospectsoul_backend.admin.designation.entity.DecisionMakerDesignation;
import com.vyoog.prospectsoul_backend.admin.designation.repository.DecisionMakerDesignationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DecisionMakerDesignationService {

    private final DecisionMakerDesignationRepository repository;
    private final Map<String, DecisionMakerDesignation> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void refreshCache() {
        cache.clear();
        for (DecisionMakerDesignation d : repository.findByActiveTrue()) {
            cache.put(normalize(d.getDesignation()), d);
            if (d.getAliases() != null) {
                for (String alias : d.getAliases()) {
                    cache.put(normalize(alias), d);
                }
            }
        }
    }

    public boolean isDecisionMaker(String designation) {
        if (designation == null || designation.isBlank()) return false;
        return cache.containsKey(normalize(designation));
    }

    public DecisionMakerDesignation resolve(String designation) {
        if (designation == null || designation.isBlank()) return null;
        return cache.get(normalize(designation));
    }

    public List<DecisionMakerDesignation> listAll() {
        return repository.findAll();
    }

    public List<DecisionMakerDesignation> listActive() {
        return repository.findByActiveTrue();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
