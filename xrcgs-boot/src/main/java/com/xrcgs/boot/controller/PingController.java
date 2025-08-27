package com.xrcgs.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 程序心跳检测
 */
@RestController
public class PingController {

    @GetMapping("/api/test/ping")
    public Map<String, Object> ping() {
        return Map.of("code", 0, "msg", "OK");
    }
}
