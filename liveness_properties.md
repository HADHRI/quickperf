# QuickPerf Liveness Properties

This document lists all available configuration properties for QuickPerf Liveness features, which allow you to monitor performance in a running Spring Boot application.

## 1. General Configuration
| Property | Default | Description |
| :--- | :--- | :--- |
| `quickperf.enabled` | `false` | Master switch to enable QuickPerf Liveness. Often enabled by default if the starter is present, but can be used to disable it. |
| `quickperf.exclude-urls` | `/actuator` | Comma-separated list of URL patterns to exclude from QuickPerf processing. |

## 2. Database & SQL Performance (KPIs)
These properties control the detection of various SQL performance issues.

| Property | Default | Description |
| :--- | :--- | :--- |
| **N+1 Selects** | | |
| `quickperf.database.n+1.detected` | `false` | Enables detection of N+1 select patterns. |
| `quickperf.database.n+1.threshold` | `3` | The number of similar select statements that triggers an N+1 alert. |
| **SQL Execution Count** | | |
| `quickperf.database.sql.execution.detected` | `false` | Enables detection of excessive SQL execution counts per request. |
| `quickperf.database.sql.execution.threshold` | `10` | The maximum allowed number of SQL statements per request before alerting. |
| **SQL Execution Time** | | |
| `quickperf.database.sql.execution-time.detected` | `false` | Enables detection of long-running SQL queries. |
| `quickperf.database.sql.execution-time.thresholdInMs` | `0` | The threshold in milliseconds for a query to be considered slow. |
| **Connection & Usage** | | |
| `quickperf.database.connection.profiled` | `false` | Enables profiling of database connection acquisition and release. |
| `quickperf.database.sql.without-bind-param.detected` | `false` | Detects SQL queries executed without bind parameters (security/performance risk). |
| **Reporting & Debugging** | | |
| `quickperf.database.sql.displayed` | `false` | If true, executed SQL statements are included in the report. |
| `quickperf.database.sql.displayed.selected-columns` | `false` | If true, the columns selected in queries are displayed. |

## 3. HTTP & Database Interactions
| Property | Default | Description |
| :--- | :--- | :--- |
| `quickperf.synchronous-http-call.while-db-connection-maintained.detected` | `false` | Detects if a synchronous HTTP call is made while a database connection is being held (a major scalability anti-pattern). |

## 4. JVM Metrics
| Property | Default | Description |
| :--- | :--- | :--- |
| `quickperf.jvm.heap-allocation.measured` | `false` | Measures the heap memory allocated by the current thread during the request. |
| `quickperf.jvm.heap-allocation.threshold.detected` | `false` | Enables detection (alerting) if heap allocation exceeds a specific threshold. |
| `quickperf.jvm.heap-allocation.threshold.value-in-bytes` | `10 000 000` | The heap allocation threshold in bytes (default is 10 MB). |

## 5. Test Generation
These properties control the automatic generation of JUnit tests from live traffic.

| Property | Default | Description |
| :--- | :--- | :--- |
| `quickperf.test-generation.junit5.enabled` | `false` | Enables the generation of JUnit 5 tests capturing the request context. |
| `quickperf.test-generation.resource-folder-path` | *(empty)* | Absolute path to the folder where generated test resources (SQL scripts, etc.) will be saved. |
| `quickperf.test-generation.java-folder-path` | *(empty)* | Absolute path to the folder where generated Java test classes will be saved. |

> [!NOTE]
> Legacy JUnit 4 support (`quickperf.test-generation.junit4.enabled`) has been removed in favor of JUnit 5.
