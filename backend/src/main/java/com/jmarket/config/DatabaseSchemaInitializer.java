package com.jmarket.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        ensureAuctionHiddenColumn();
    }

    private void ensureAuctionHiddenColumn() {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'auctions'
                  and column_name = 'hidden'
                """,
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("alter table auctions add column hidden tinyint(1) not null default 0");
        }
    }
}
