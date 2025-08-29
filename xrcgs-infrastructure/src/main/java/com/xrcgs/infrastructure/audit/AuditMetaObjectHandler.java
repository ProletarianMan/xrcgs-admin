package com.xrcgs.infrastructure.audit;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统一审计字段自动填充：
 * - createdAt: LocalDateTime，insert 时填
 * - createdBy: Long（从 Security 获取），insert 时填（拿不到则不填）
 *
 * 说明：
 * - 这是“唯一”的 createdAt/createdBy 填充器；其他模块请勿重复设置。
 */
@Component
@Order(100) // 先于多数通用填充器执行；如有需要可调整顺序
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final UserIdProvider userIdProvider;

    public AuditMetaObjectHandler(UserIdProvider userIdProvider) {
        this.userIdProvider = userIdProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        // 若实体未显式赋值，则自动填充
        strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);

        Long uid = userIdProvider.getCurrentUserId();
        if (uid != null) {
            strictInsertFill(metaObject, "createdBy", () -> uid, Long.class);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 如需 updatedAt/updatedBy，将来在此扩展；目前不做
    }
}
