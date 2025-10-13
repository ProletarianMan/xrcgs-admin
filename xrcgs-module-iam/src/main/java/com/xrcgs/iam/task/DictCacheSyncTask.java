package com.xrcgs.iam.task;

import com.xrcgs.iam.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 字典缓存定时同步任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DictCacheSyncTask {

    private final DictService dictService;

    /**
     * 每天凌晨 1 点同步一次字典缓存，保障 Redis 中的数据与数据库保持一致。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncDictCache() {
        long start = System.currentTimeMillis();
        try {
            dictService.syncAllDictCache();
            log.info("定时同步字典缓存完成, cost={}ms", System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("定时同步字典缓存失败", ex);
        }
    }
}
