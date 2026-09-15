package com.udabank.authdemo.customer;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * This is the pentest's SQL-injection finding. It uses JdbcTemplate directly
 * instead of the JPA repository because the original author needed a
 * flexible partial-match search — a reasonable-sounding reason that led to
 * building the query by string concatenation instead of parameterizing it.
 *
 * TODO: fix searchByName(name) so the search term can never break out of
 * the query. Replace the concatenated SQL string with a parameterized
 * query: keep "?" as a placeholder in the SQL and pass the actual value as
 * a separate argument to JdbcTemplate.query(sql, rowMapper, args...) —
 * the driver then sends the value as data, never as part of the SQL text,
 * so there's nothing for an attacker's quotes or SQL keywords to "break out" into.
 */
@Service
public class CustomerSearchService {

    private final JdbcTemplate jdbcTemplate;

    public CustomerSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CustomerSearchResult> searchByName(String name) {
        // TODO: this string-concatenated SQL is the vulnerability — fix it
        String sql = "SELECT id, full_name, account_number "
                + "FROM customers WHERE full_name LIKE '%" + name + "%'";
        return jdbcTemplate.query(sql, CustomerSearchService::mapRow);
    }

    private static CustomerSearchResult mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CustomerSearchResult(rs.getLong("id"), rs.getString("full_name"), rs.getString("account_number"));
    }
}
