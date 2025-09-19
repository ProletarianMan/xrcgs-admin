package com.xrcgs.iam.datascope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.iam.entity.SysDept;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods used by the data scope calculation pipeline.
 */
public final class DataScopeUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DataScopeUtil() {
    }

    public static Set<Long> parseIdSet(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return Collections.emptySet();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonArray);
            if (node == null || !node.isArray()) {
                return Collections.emptySet();
            }
            Set<Long> ids = new LinkedHashSet<>();
            for (JsonNode element : node) {
                Long value = toLong(element);
                if (value != null) {
                    ids.add(value);
                }
            }
            return ids;
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
    }

    private static Long toLong(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (text != null) {
                text = text.trim();
                if (!text.isEmpty()) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    public static Map<Long, Set<Long>> buildChildrenMap(List<SysDept> departments) {
        Map<Long, Set<Long>> children = new HashMap<>();
        if (departments == null) {
            return children;
        }
        for (SysDept dept : departments) {
            if (dept == null || dept.getId() == null) {
                continue;
            }
            Long parentId = dept.getParentId();
            if (parentId == null) {
                parentId = 0L;
            }
            children.computeIfAbsent(parentId, k -> new LinkedHashSet<>()).add(dept.getId());
        }
        return children;
    }

    public static Set<Long> collectWithChildren(Long deptId, Map<Long, Set<Long>> childrenMap) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !result.add(current)) {
                continue;
            }
            Set<Long> children = childrenMap.get(current);
            if (children != null) {
                for (Long child : children) {
                    if (child != null) {
                        queue.add(child);
                    }
                }
            }
        }
        return result;
    }

    public static void merge(Set<Long> target, Collection<Long> source) {
        if (target == null || source == null) {
            return;
        }
        for (Long id : source) {
            if (id != null) {
                target.add(id);
            }
        }
    }

    public static long nullSafeVersion(Long version) {
        return version == null ? 0L : version;
    }
}
