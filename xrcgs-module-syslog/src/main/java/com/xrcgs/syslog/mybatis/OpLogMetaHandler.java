package com.xrcgs.syslog.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mybatis在日志中自动填充创建日期createdAt
 */
@Component
public class OpLogMetaHandler implements MetaObjectHandler {

    /** 插入数据 自动获取 **/
    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
    }

    /**  更新数据实现方法，暂时先不操作  **/
    @Override
    public void updateFill(MetaObject metaObject) { }
}
