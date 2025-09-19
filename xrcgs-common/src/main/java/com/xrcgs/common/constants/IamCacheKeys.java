package com.xrcgs.common.constants;

/**
 * 角色模块---redis缓存常量键（Key）
 */
public interface IamCacheKeys {

    // 用户权限集合（含角色、权限码、通配符），7-4 会在登录时写入/读取
    String AUTH_PERM_USER = "auth:perm:"; // + {userId}

    // 菜单树缓存（可按角色维度缓存）
    String MENU_TREE_ROLE = "menu:tree:"; // + {roleId} or "ALL"

    // 字典缓存：按 typeCode
    String DICT_TYPE = "dict:"; // + {typeCode}

    // 部门树版本号，用于通知前端刷新组织架构缓存
    String DEPT_TREE_VERSION = "iam:dept:treeVersion";

    // 部门数据范围缓存前缀，+ {deptId}
    String DEPT_SCOPE = "iam:dept:scope:";

}
