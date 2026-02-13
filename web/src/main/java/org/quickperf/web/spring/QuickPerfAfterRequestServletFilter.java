/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2021-2022 the original author or authors.
 */
package org.quickperf.web.spring;

import net.ttddyy.dsproxy.QueryInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quickperf.sql.SqlExecution;
import org.quickperf.sql.SqlExecutions;
import org.quickperf.sql.SqlRecorderRegistry;
import org.quickperf.sql.connection.ConnectionListenerRegistry;
import org.quickperf.sql.select.analysis.SelectAnalysis;
import org.quickperf.sql.select.analysis.SelectAnalysisExtractor;
import org.quickperf.web.spring.config.*;
import org.quickperf.web.spring.jvm.ByteWatcherSingleThread;
import org.quickperf.web.spring.jvm.ByteWatcherSingleThreadRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.qstd.QuickSqlTestData;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.Map;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class QuickPerfAfterRequestServletFilter implements Filter {

    private final DatabaseConfig databaseConfig;

    private final Log logger = LogFactory.getLog(this.getClass());

    private final JvmConfig jvmConfig;

    private final UrlConfig urlConfig;

    public QuickPerfAfterRequestServletFilter(DatabaseConfig databaseConfig,
            JvmConfig jvmConfig,
            UrlConfig urlConfig) {
        this.databaseConfig = databaseConfig;
        this.jvmConfig = jvmConfig;
        this.urlConfig = urlConfig;
        logger.debug(this.getClass().getSimpleName() + "is created");
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        Throwable problem = null;

        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        try {
            filterChain.doFilter(servletRequest, httpServletResponse);
        } catch (Throwable t) {
            problem = t;
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        String contentTypeAsString = httpServletResponse.getContentType();
        HttpContentType httpContentType = new HttpContentType(contentTypeAsString);
        String url = httpServletRequest.getRequestURL().toString();

        try {
            if (!urlConfig.checkIfExcluded(url)
                    && (httpContentType.isHtml() || httpContentType.isJson() || httpContentType.isText()
                            || httpContentType.isPdf() || httpContentType.isPdf())) {
                quickPerfProcessing(httpServletRequest, httpServletResponse);
            }
        } catch (Exception e) {
            // Propose to create QuickPerfIssue
            logger.warn("Unexpected QuickPerf issue", e);
        } finally {
            unregisterListeners();
        }

        handleProblem(problem);
    }

    private void unregisterListeners() {
        ByteWatcherSingleThreadRegistry.INSTANCE.unregister();
        SqlRecorderRegistry.INSTANCE.clear();
        ConnectionListenerRegistry.INSTANCE.clear();
        SynchronousHttpCallsRegistry.INSTANCE.unregisterHttpCalls();
        PerfEventsRegistry.INSTANCE.unregisterPerfEvents();
    }

    @Override
    public void destroy() {
    }

    private void handleProblem(Throwable problem) throws ServletException, IOException {
        if (problem != null) {
            if (problem instanceof ServletException) {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException) {
                throw (IOException) problem;
            }
            // sendProcessingError(problem, response);
        }
    }

    private void quickPerfProcessing(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {

        SqlExecutionsRecorder sqlExecutionsRecorder = SqlRecorderRegistry.INSTANCE
                .getSqlRecorderOfType(SqlExecutionsRecorder.class);

        SqlExecutions sqlExecutions = null;
        if (databaseConfig.isSqlDisplayed() || databaseConfig.isNPlusOneSelectDetected()
                || databaseConfig.isSqlExecutionDetected()
                || databaseConfig.isSqlWithoutBindParamDetected()) {
            sqlExecutions = sqlExecutionsRecorder.findRecord(null);
        }

        // --- JSON Logging for OpenSearch ---
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            long timestamp = System.currentTimeMillis();
            String reqUrl = httpServletRequest.getRequestURI();
            String reqMethod = httpServletRequest.getMethod();
            String operationName = httpServletResponse.getHeader("X-Operation-Name");

            // 1. JVM Metrics
            if (jvmConfig.isHeapAllocationMeasured() || jvmConfig.isHeapAllocationThresholdDetected()) {
                ByteWatcherSingleThread byteWatcherSingleThread = ByteWatcherSingleThreadRegistry.INSTANCE.get();
                long allocationInBytes = byteWatcherSingleThread.calculateAllocations();

                Map<String, Object> jvmData = new LinkedHashMap<>();
                jvmData.put("timestamp", timestamp);
                jvmData.put("type", "JVM_METRICS");
                jvmData.put("url", reqUrl);
                jvmData.put("method", reqMethod);
                if (operationName != null) {
                    jvmData.put("operation_name", operationName);
                }
                jvmData.put("heap_allocation_bytes", allocationInBytes);

                if (jvmConfig.isHeapAllocationThresholdDetected()) {
                    jvmData.put("threshold_bytes", jvmConfig.getHeapAllocationThresholdValueInBytes());
                    jvmData.put("threshold_exceeded",
                            allocationInBytes > jvmConfig.getHeapAllocationThresholdValueInBytes());
                }

                Log jvmLogger = LogFactory.getLog("org.quickperf.jvm");
                jvmLogger.info(objectMapper.writeValueAsString(jvmData));
            }

            if (sqlExecutions != null) {
                // 2. Slow Queries
                if (databaseConfig.isSqlExecutionTimeDetected()) {
                    LongDbRequestsListener longDbRequestsListener = SqlRecorderRegistry.INSTANCE
                            .getSqlRecorderOfType(LongDbRequestsListener.class);
                    SqlExecutions slowExecutions = longDbRequestsListener.getSqlExecutionsGreaterOrEqualToThreshold();

                    if (!slowExecutions.isEmpty()) {
                        Map<String, Object> slowQueryData = new LinkedHashMap<>();
                        slowQueryData.put("timestamp", timestamp);
                        slowQueryData.put("type", "SLOW_QUERY_DETECTED");
                        slowQueryData.put("url", reqUrl);
                        slowQueryData.put("method", reqMethod);
                        if (operationName != null) {
                            slowQueryData.put("operation_name", operationName);
                        }
                        slowQueryData.put("threshold_ms", databaseConfig.getSqlExecutionTimeThresholdInMilliseconds());

                        List<Map<String, Object>> queries = new ArrayList<>();
                        for (SqlExecution execution : slowExecutions) {
                            for (QueryInfo q : execution.getQueries()) {
                                Map<String, Object> qData = new LinkedHashMap<>();
                                qData.put("sql", q.getQuery());
                                qData.put("time_ms", execution.getElapsedTime());
                                // Extract simple call stack info if available, or just first line
                                List<String> stack = execution.getCallStack();
                                if (stack != null && !stack.isEmpty()) {
                                    qData.put("caller", stack.get(0));
                                }
                                queries.add(qData);
                            }
                        }
                        slowQueryData.put("queries", queries);

                        Log slowQueryLogger = LogFactory.getLog("org.quickperf.slowquery");
                        slowQueryLogger.warn(objectMapper.writeValueAsString(slowQueryData));
                    }
                }

                // 3. N+1 Detection
                if (databaseConfig.isNPlusOneSelectDetected()) {
                    SelectAnalysis selectAnalysis = SelectAnalysisExtractor.INSTANCE
                            .extractPerfMeasureFrom(sqlExecutions);
                    if (selectAnalysis.getSameSelectTypesWithDifferentParamValues().evaluate()) {
                        Map<String, Object> nPlusOneData = new LinkedHashMap<>();
                        nPlusOneData.put("timestamp", timestamp);
                        nPlusOneData.put("type", "N_PLUS_ONE_DETECTED");
                        nPlusOneData.put("url", reqUrl);
                        nPlusOneData.put("method", reqMethod);
                        if (operationName != null) {
                            nPlusOneData.put("operation_name", operationName);
                        }
                        nPlusOneData.put("count", selectAnalysis.getSelectNumber().getValue());
                        nPlusOneData.put("sample_query", selectAnalysis.getNPlusOneQuery());
                        nPlusOneData.put("impacted_tables", selectAnalysis.getNPlusOneImpactedTables());
                        nPlusOneData.put("call_stack", selectAnalysis.getNPlusOneCallStack());

                        Log nPlusOneLogger = LogFactory.getLog("org.quickperf.nplusone");
                        nPlusOneLogger.warn(objectMapper.writeValueAsString(nPlusOneData));
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to log QuickPerf JSON data", e);
        }

    }

}
