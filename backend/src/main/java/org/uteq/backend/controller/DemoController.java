package org.uteq.backend.controller;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final JdbcTemplate jdbcTemplate;

    public DemoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/demo/whoami")
    public String whoami() {
        return jdbcTemplate.queryForObject("SELECT current_user", String.class);
    }
}