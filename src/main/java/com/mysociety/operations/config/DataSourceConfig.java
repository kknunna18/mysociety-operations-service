package com.mysociety.operations.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {
    private final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment env) {
        String rawUrl = env.getProperty("DATABASE_URL");
        String username = env.getProperty("DATABASE_USERNAME");
        String password = env.getProperty("DATABASE_PASSWORD");

        if (rawUrl == null || rawUrl.isBlank()) {
            log.warn("DATABASE_URL not set — using in-memory fallback datasource for operations");
            return DataSourceBuilder.create()
                    .url("jdbc:h2:mem:operations;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;INIT=CREATE SCHEMA IF NOT EXISTS mysociety\\;SET SCHEMA mysociety")
                    .driverClassName("org.h2.Driver")
                    .username("sa")
                    .password("")
                    .build();
        }

        String url = rawUrl.trim();
        try {
            if (!url.startsWith("jdbc:")) {
                URI uri = new URI(url);
                String scheme = uri.getScheme();
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    if (parts.length > 0 && (username == null || username.isBlank())) username = parts[0];
                    if (parts.length > 1 && (password == null || password.isBlank())) password = parts[1];
                }

                if ("postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme)) {
                    url = "jdbc:postgresql://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "") + uri.getPath();
                    log.info("Converted DATABASE_URL to JDBC URL for Postgres: {}", url);
                    return DataSourceBuilder.create()
                            .driverClassName("org.postgresql.Driver")
                            .url(url)
                            .username(username)
                            .password(password)
                            .build();
                }

                if ("mysql".equalsIgnoreCase(scheme) || "mariadb".equalsIgnoreCase(scheme)) {
                    url = "jdbc:mysql://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "") + uri.getPath();
                    log.info("Converted DATABASE_URL to JDBC URL for MySQL: {}", url);
                    return DataSourceBuilder.create()
                            .driverClassName("com.mysql.cj.jdbc.Driver")
                            .url(url)
                            .username(username)
                            .password(password)
                            .build();
                }

                log.warn("DATABASE_URL does not start with jdbc and scheme {} is not explicitly supported. Trying as JDBC URL: {}", scheme, url);
            }

            return DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .build();
        } catch (Exception e) {
            log.error("Failed to configure DataSource from DATABASE_URL='{}'. Falling back to in-memory H2. Error: {}", rawUrl, e.toString());
            return DataSourceBuilder.create()
                    .url("jdbc:h2:mem:operations;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;INIT=CREATE SCHEMA IF NOT EXISTS mysociety\\;SET SCHEMA mysociety")
                    .driverClassName("org.h2.Driver")
                    .username("sa")
                    .password("")
                    .build();
        }
    }
}
