package com.xrcgs.iam.enums;

/**
 * 菜单权限，用在 sys_menu 表的 type 字段，区分菜单节点的用途
 * DIR
 * 👉 目录（前端左侧大类，只做分组，不直接点开页面）。
 * 例：系统管理
 * MENU
 * 👉 菜单（真正有页面的路由）。
 * 例：用户管理
 * BUTTON
 * 👉 按钮（页面里的功能点，需要权限控制）。
 * 例：新增用户按钮、删除用户按钮
 * API
 * 👉 接口权限（后端接口层面的控制点）。
 * 例：iam:user:list、file:doc:convert
 */
public enum MenuType {
    DIR, MENU, BUTTON, API
}
