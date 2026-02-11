<div align="center">
<img src="https://pbs.twimg.com/profile_banners/926219963333038086/1518645789" alt="QuickPerf"/>
</div>

<div>
<blockquote>
<p><h3>QuickPerf is a testing library for Java to quickly evaluate and improve some performance-related properties</h3></p>
</blockquote>
</div>

---
<p align="center">	
  <a href="https://github.com/quick-perf/quickperf/blob/master/LICENSE.txt">
    <img src="https://img.shields.io/badge/license-Apache2-blue.svg"
         alt = "License">
  </a>
</p>

---

# QuickPerf Comprehensive Guide

QuickPerf is a testing library for Java that allows you to quickly evaluate and improve performance-related properties of your application. It provides annotations to measure and assert performance metrics such as SQL execution count, heap allocation, and more.

## 1. Modules Overview

QuickPerf is modular, allowing you to include only what you need.

| Module | Description |
| :--- | :--- |
| **`quick-perf-core`** | Core logic and interfaces used by other modules. |
| **`quick-perf-jvm-parent`** | Parent module for JVM-related features. |
| **`quick-perf-jvm-core`** | Core logic for JVM performance measurement (e.g., heap allocation). |
| **`quick-perf-jvm-annotations`** | Annotations for JVM performance testing (e.g., `@MeasureHeapAllocation`). |
| **`quick-perf-jfr-annotations`** | Annotations for profiling with JDK Flight Recorder (`@ProfileJvm`). |
| **`quick-perf-sql-parent`** | Parent module for SQL-related features. |
| **`quick-perf-sql-annotations`** | Annotations for SQL performance testing (e.g., `@ExpectSelect`, `@DisableSameSelects`). |
| **`quick-perf-spring`** | Parent module for Spring integration. |
| **`quick-perf-sql-spring6`** | Spring 6 (Spring Boot 3) integration for SQL features. |
| **`quick-perf-springboot3-sql-starter`** | Starter module for easy integration with Spring Boot 3 applications. |
| **`quick-perf-web`** | Web module providing Liveness features and test generation for Spring Web applications. |

## 2. Using QuickPerf in a Spring Boot 3 Application

To use QuickPerf in a Spring Boot 3 application, you typically need the `quick-perf-springboot3-sql-starter`.

### Maven Configuration

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.quickperf</groupId>
    <artifactId>quick-perf-springboot3-sql-starter</artifactId>
    <version>1.1.2-SNAPSHOT</version> <!-- Use the latest version -->
    <scope>test</scope>
</dependency>
```

For JVM annotations, add:

```xml
<dependency>
    <groupId>org.quickperf</groupId>
    <artifactId>quick-perf-jvm-annotations</artifactId>
    <version>1.1.2-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### Configuration

For Spring Boot integration, QuickPerf is automatically configured via the starter. You can use the annotations directly in your tests.

### Spring Boot 3 & JUnit 5 Example

```java
import org.junit.jupiter.api.Test;
import org.quickperf.spring.sql.QuickPerfTest; // Required for Spring Boot integration
import org.quickperf.sql.annotation.ExpectSelect;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@QuickPerfTest // activates QuickPerf processing
public class MyServiceTest {

    @Test
    @ExpectSelect(1) // Asserts that exactly one SELECT statement is executed
    public void should_find_user() {
        // service.findUser(...);
    }
}
```

## 3. SQL Performance Features (KPIs)

QuickPerf helps you detect common database performance issues (N+1 selects, excessive queries, etc.) directly in your tests.

### Common Annotations (`sql-annotations`)

*   **`@ExpectSelect(int value)`**: Verifies the number of `SELECT` statements executed.
*   **`@ExpectInsert(int value)`**, **`@ExpectUpdate`**, **`@ExpectDelete`**: Verify counts for other SQL statement types.
*   **`@ExpectMaxSelect(int value)`**: Verifies that the number of `SELECT` statements does not exceed a maximum.
*   **`@DisableSameSelects`**: Fails the test if the exact same `SELECT` statement (same SQL and same parameters) is executed more than once. This is excellent for detecting caching opportunities.
*   **`@AnalyzeSql`**: Provides detailed analysis of executed SQL queries without failing the test by default.
*   **`@DisableLikeWithLeadingWildcard`**: Fails if a `LIKE` query starts with a wildcard (`%value`), which prevents index usage.
*   **`@PermitSelect`**, **`@PermitInsert`**, etc.: Explicitly allows certain statement types (useful when you want to ban others by default or strictly control usage).

### Liveness (Web Module)

The `quick-perf-web` module provides "Liveness" features to monitor performance in a running application (e.g., dev or staging env), not just tests.

**Key Properties (configure in `application.properties`):**

*   **N+1 Detection**: `quickperf.database.n+1.detected=true`
*   **SQL Execution Time**: `quickperf.database.sql.execution-time.detected=true` (Set threshold with `thresholdInMs`)
*   **SQL Count**: `quickperf.database.sql.execution.detected=true`
*   **Connection Profiling**: `quickperf.database.connection.profiled=true`

See [Liveness Properties](liveness_properties.md) for a full list.

## 4. JVM Performance Features

The JVM modules check memory allocation and garbage collection behavior.

### Annotations (`jvm-annotations` & `jfr-annotations`)

*   **`@MeasureHeapAllocation`**: Measures the heap allocation of the test method thread.
    ```java
    @Test
    @MeasureHeapAllocation
    public void convert_data() { ... }
    ```
*   **`@ExpectMaxHeapAllocation(value = X, unit = ...)`**: Asserts that heap allocation does not exceed a specific limit.
*   **`@ProfileJvm`**: Profiles the JVM execution using JDK Flight Recorder (JFR). This creates a `.jfr` file that you can open with JDK Mission Control to analyze CPU, memory, and hot methods.
    *   *Note: Requires running with a JDK that supports JFR (most modern OpenJDKs do).*

## 5. JUnit Modules

QuickPerf supports both JUnit 4 and JUnit 5.

*   **JUnit 5**: The core extensions are built for Junit 5. In Spring Boot tests, use `@QuickPerfTest` to integrate the Spring context with QuickPerf's JUnit 5 extension.
*   **JUnit 4**: Legacy support exists (e.g., `QuickPerfSpringRunner`), but for Spring Boot 3, **JUnit 5 is the standard**.
    *   *Note: The `web` module test generation recently removed JUnit 4 support to align with Spring Boot 3 best practices.*

---

## License
[Apache License 2.0](/LICENSE.txt)
