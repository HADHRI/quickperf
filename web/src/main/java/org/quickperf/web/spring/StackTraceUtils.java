package org.quickperf.web.spring;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StackTraceUtils {

    private static final List<String> EXCLUDED_PACKAGES = Arrays.asList(
            "org.quickperf",
            "net.ttddyy",
            "org.springframework",
            "org.apache",
            "org.hibernate",
            "java",
            "javax",
            "jakarta",
            "jdk",
            "sun",
            "com.sun",
            "org.eclipse.jetty",
            "com.zaxxer",
            "org.h2",
            "org.junit",
            "junit");

    private StackTraceUtils() {
    }

    public static List<String> getCallStack() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.stream(stackTrace)
                .filter(StackTraceUtils::isApplicationCode)
                .map(StackTraceElement::toString)
                .limit(30) // Increased limit to capture full flow including Repositories and Controllers
                .collect(Collectors.toList());
    }

    private static boolean isApplicationCode(StackTraceElement element) {
        String className = element.getClassName();

        // 1. Exclude specific framework internals that might otherwise be allowed
        if (className.startsWith("org.springframework.data.repository.core") ||
                className.startsWith("org.springframework.data.projection") ||
                className.startsWith("org.springframework.data.repository.query") ||
                className.startsWith("org.springframework.aop") ||
                className.startsWith("org.springframework.transaction")) {
            return false;
        }

        // 2. Always include proxies (Dynamic, CGLIB, Hibernate)
        if (className.contains("$$") || className.contains("$Proxy")) {
            return true;
        }

        // 3. Always include Spring Data repositories (implementations)
        if (className.startsWith("org.springframework.data")) {
            return true;
        }

        // 4. Check general excluded packages
        for (String excluded : EXCLUDED_PACKAGES) {
            if (className.startsWith(excluded)) {
                return false;
            }
        }
        return true;
    }
}
