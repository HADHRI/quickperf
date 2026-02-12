package org.quickperf.web.spring;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StackTraceUtils {

    private static final List<String> EXCLUDED_PACKAGES = Arrays.asList(
            "net.ttddyy",
            "org.springframework",
            "org.apache",
            "org.hibernate",
            "java.",
            "javax.",
            "jakarta.",
            "jdk.",
            "sun.",
            "com.sun.",
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
                .limit(10)
                .collect(Collectors.toList());
    }

    private static boolean isApplicationCode(StackTraceElement element) {
        String className = element.getClassName();

        // 1. Exclude ALL quickperf internals (handles org.quickperf,
        // com.bnpparibas.quickperf, etc.)
        if (className.contains("quickperf")) {
            return false;
        }

        // 2. Exclude JDK Dynamic Proxies ($Proxy123) - these are JDBC/framework noise
        if (className.contains("$Proxy")) {
            return false;
        }

        // 3. Include Spring Data JPA repository implementations (e.g.
        // SimpleJpaRepository)
        if (className.startsWith("org.springframework.data.jpa.repository.support")) {
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
