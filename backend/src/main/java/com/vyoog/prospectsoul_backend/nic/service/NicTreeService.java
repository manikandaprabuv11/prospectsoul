package com.vyoog.prospectsoul_backend.nic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.vyoog.prospectsoul_backend.nic.dto.response.NicTreeNodeResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.mapper.NicCodeMapper;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-memory cache of the whole NIC tree (Kickoff §7 C1 AC 10). Invalidated
 * on any mutation via {@link #invalidate()}. Rebuild is one flat query plus
 * an in-memory grouping — cheap even at ~5,000 codes.
 */
@Service
@RequiredArgsConstructor
public class NicTreeService {

    private final NicCodeRepository nicCodeRepository;
    private final NicCodeMapper nicCodeMapper;

    private final AtomicReference<List<NicTreeNodeResponse>> cachedRoots = new AtomicReference<>();

    public void invalidate() {
        cachedRoots.set(null);
    }

    @Transactional(readOnly = true)
    public List<NicTreeNodeResponse> fullTree() {
        List<NicTreeNodeResponse> cached = cachedRoots.get();
        if (cached != null) return cached;
        List<NicTreeNodeResponse> rebuilt = rebuild(null, Integer.MAX_VALUE);
        cachedRoots.set(rebuilt);
        return rebuilt;
    }

    @Transactional(readOnly = true)
    public List<NicTreeNodeResponse> subtree(UUID rootId, int depth) {
        return rebuild(rootId, depth);
    }

    private List<NicTreeNodeResponse> rebuild(UUID rootId, int depth) {
        List<NicCode> all = nicCodeRepository.findAll();
        Map<UUID, List<NicCode>> byParent = new HashMap<>();
        for (NicCode c : all) {
            UUID pid = c.getParentId();
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }
        List<NicCode> seeds;
        if (rootId == null) {
            seeds = Optional.ofNullable(byParent.get(null)).orElse(List.of());
        } else {
            seeds = all.stream().filter(c -> c.getId().equals(rootId)).toList();
        }
        seeds.forEach(s -> sortByCode(s));
        List<NicTreeNodeResponse> roots = new ArrayList<>(seeds.size());
        for (NicCode seed : seeds) {
            roots.add(build(seed, byParent, depth));
        }
        return roots;
    }

    private NicTreeNodeResponse build(NicCode node, Map<UUID, List<NicCode>> byParent, int depth) {
        List<NicCode> raw = byParent.getOrDefault(node.getId(), List.of());
        long count = raw.size();
        List<NicTreeNodeResponse> children;
        if (depth <= 0) {
            children = List.of();
        } else {
            children = new ArrayList<>(raw.size());
            raw.stream()
                    .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                    .forEach(child -> children.add(build(child, byParent, depth - 1)));
        }
        return nicCodeMapper.toTreeNode(node, count, children);
    }

    private void sortByCode(NicCode node) {
        // no-op — sort happens per-parent list in build(); kept for future use.
    }
}
