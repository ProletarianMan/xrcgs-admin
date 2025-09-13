package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.iam.entity.SysMenu;
import com.xrcgs.iam.entity.SysRoleMenu;
import com.xrcgs.iam.mapper.SysMenuMapper;
import com.xrcgs.iam.mapper.SysRoleMenuMapper;
import com.xrcgs.iam.model.query.MenuQuery;
import com.xrcgs.iam.model.vo.MenuMetaVO;
import com.xrcgs.iam.model.vo.MenuTreeVO;
import com.xrcgs.iam.service.MenuService;
import com.xrcgs.common.cache.AuthCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final AuthCacheService cacheService;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysMenu menu) {
        validateParent(menu.getParentId());
        menuMapper.insert(menu);
        // 可选择在这里清理全量菜单树缓存（如果你对 ALL/roleId 做了缓存）
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenu menu) {
        validateParent(menu.getParentId());
        menuMapper.updateById(menu);
        // 可在此按需失效相关角色的菜单树缓存
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        long childCount = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (childCount > 0) throw new IllegalStateException("存在子菜单，无法删除");
        menuMapper.deleteById(id);
        roleMenuMapper.delete(
                Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id)
        );
        // 缓存失效可在角色分配/查询时做，这里不强制
    }

    @Override
    public List<MenuTreeVO> treeAllEnabled() {
        // 读取缓存（roleId=0 代表 ALL）
        try {
            String cached = cacheService.getCachedMenuTreeByRole(0L);
            if (cached != null) {
                return om.readValue(cached, new TypeReference<List<MenuTreeVO>>() {});
            }
        } catch (Exception ignore) {}

        List<SysMenu> list = menuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getStatus, 1).eq(SysMenu::getDelFlag, 0)
                        .orderByAsc(SysMenu::getRank, SysMenu::getId));
        List<MenuTreeVO> tree = buildTree(list);

        try {
            cacheService.cacheMenuTreeByRole(0L, om.writeValueAsString(tree));
        } catch (Exception ignore) {}
        return tree;
    }

    @Override
    public List<MenuTreeVO> treeByRole(Long roleId) {
        try {
            String cached = cacheService.getCachedMenuTreeByRole(roleId);
            if (cached != null) {
                return om.readValue(cached, new TypeReference<List<MenuTreeVO>>() {});
            }
        } catch (Exception ignore) {}

        List<SysMenu> list = menuMapper.selectByRoleId(roleId).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1 && m.getDelFlag() != null && m.getDelFlag() == 0)
                .sorted(Comparator.comparing(SysMenu::getRank).thenComparing(SysMenu::getId))
                .collect(Collectors.toList());
        List<MenuTreeVO> tree = buildTree(list);

        try {
            cacheService.cacheMenuTreeByRole(roleId, om.writeValueAsString(tree));
        } catch (Exception ignore) {}
        return tree;
    }

    @Override
    public List<SysMenu> list(MenuQuery q) {
        return menuMapper.selectListByQuery(q);
    }

    /* helpers */
    private void validateParent(Long parentId) {
        if (parentId == null || parentId == 0) return;
        SysMenu p = menuMapper.selectById(parentId);
        if (p == null || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new IllegalArgumentException("父节点不存在或已删除");
        }
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> flat) {
        Map<Long, MenuTreeVO> map = new LinkedHashMap<>();
        for (SysMenu m : flat) {
            MenuTreeVO vo = new MenuTreeVO();
            vo.setName(m.getRouterName());
            vo.setPath(m.getPath());
            vo.setComponent(m.getComponent());
            MenuMetaVO meta = new MenuMetaVO();
            meta.setTitle(m.getTitle());
            meta.setIcon(m.getIcon());
            meta.setRank(m.getRank());
            meta.setKeepAlive(m.getKeepAlive());
            meta.setShowParent(m.getShowParent());
            vo.setMeta(meta);
            map.put(m.getId(), vo);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (SysMenu m : flat) {
            MenuTreeVO vo = map.get(m.getId());
            Long pid = m.getParentId();
            if (pid == null || pid == 0 || !map.containsKey(pid)) {
                roots.add(vo);
            } else {
                map.get(pid).getChildren().add(vo);
            }
        }
        return roots;
    }
}
