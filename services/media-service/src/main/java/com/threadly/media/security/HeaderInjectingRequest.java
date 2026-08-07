package com.threadly.media.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class HeaderInjectingRequest extends HttpServletRequestWrapper {
    private final Map<String, String> extra;
    public HeaderInjectingRequest(HttpServletRequest request, Map<String, String> extra) {
        super(request); this.extra = extra;
    }
    @Override public String getHeader(String name) {
        for (var e : extra.entrySet()) if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        return super.getHeader(name);
    }
    @Override public Enumeration<String> getHeaders(String name) {
        String v = getHeader(name);
        if (v != null && extra.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name)))
            return Collections.enumeration(List.of(v));
        return super.getHeaders(name);
    }
    @Override public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) names.add(original.nextElement());
        names.addAll(extra.keySet());
        return Collections.enumeration(names);
    }
}
