package com.ranjit.jobportal.config;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the application database on startup if it does not exist yet.
 * Connects to the default {@code postgres} database to run CREATE DATABASE.
 */
@Slf4j
public final class DatabaseEnsurer {

    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("jdbc:postgresql://([^/]+)/([^?]+)");

    private DatabaseEnsurer() {
    }

    public static void ensureDatabaseExists(String jdbcUrl, String username, String password) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.find()) {
            log.warn("Could not parse JDBC URL for auto database creation: {}", jdbcUrl);
            return;
        }

        String hostPort = matcher.group(1);
        String databaseName = matcher.group(2);

        if ("postgres".equalsIgnoreCase(databaseName)) {
            return;
        }

        String adminUrl = "jdbc:postgresql://" + hostPort + "/postgres";

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(adminUrl, props);
             Statement statement = connection.createStatement()) {

            if (databaseExists(statement, databaseName)) {
                log.info("Database '{}' already exists", databaseName);
                return;
            }

            statement.executeUpdate("CREATE DATABASE \"" + databaseName.replace("\"", "") + "\"");
            log.info("Created database '{}'", databaseName);
        } catch (Exception ex) {
            log.error(
                    "Failed to create database '{}'. Create it manually in pgAdmin: CREATE DATABASE {};",
                    databaseName,
                    databaseName,
                    ex
            );
            throw new IllegalStateException(
                    "Database '" + databaseName + "' does not exist and could not be created automatically. "
                            + "Run in pgAdmin: CREATE DATABASE " + databaseName + ";",
                    ex
            );
        }
    }

    private static boolean databaseExists(Statement statement, String databaseName) throws Exception {
        String sql =
                "SELECT 1 FROM pg_database WHERE datname = '" + databaseName.replace("'", "''") + "'";
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }
}
