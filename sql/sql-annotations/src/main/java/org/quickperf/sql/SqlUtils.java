package org.quickperf.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlUtils {

    // Simple regex to find words after FROM and JOIN.
    // It captures the table name and optionally the alias, but we focus on the
    // table name.
    // Case insensitive, looks for FROM or JOIN followed by whitespace and a word.
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?:\\bFROM\\b|\\bJOIN\\b)\\s+([\\w\"`]+)",
            Pattern.CASE_INSENSITIVE);

    private SqlUtils() {
    }

    public static List<String> extractTableNames(String sql) {
        if (sql == null || sql.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1);
            // Clean up identifying quotes if present
            tableName = tableName.replaceAll("[\"`]", "");
            if (!tables.contains(tableName)) {
                tables.add(tableName);
            }
        }
        return tables;
    }
}
