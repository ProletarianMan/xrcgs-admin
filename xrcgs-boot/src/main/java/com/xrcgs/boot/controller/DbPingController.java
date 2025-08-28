package com.xrcgs.boot.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据库心跳测试
 */
@RestController
public class DbPingController {

    private final JdbcTemplate jdbc;

    public DbPingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/db/ping")
    public Map<String, Object> ping() {
        Integer one = jdbc.queryForObject("select 1", Integer.class);
        return Map.of("code", 0, "msg", one != null && one == 1 ? "DB OK" : "DB FAIL");
    }
}
