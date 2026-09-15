package com.udabank.authdemo.customer;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerSearchService {

    private final JdbcTemplate jdbcTemplate;

    public CustomerSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CustomerSearchResult> searchByName(String name) {
        String sql = "SELECT id, full_name, account_number "
                + "FROM customers WHERE full_name LIKE ?";
        return jdbcTemplate.query(sql, CustomerSearchService::mapRow, "%" + name + "%");
    }

    private static CustomerSearchResult mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CustomerSearchResult(rs.getLong("id"), rs.getString("full_name"), rs.getString("account_number"));
    }
}
