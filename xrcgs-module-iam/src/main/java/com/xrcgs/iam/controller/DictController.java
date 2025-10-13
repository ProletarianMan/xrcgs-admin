package com.xrcgs.iam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.entity.SysDictType;
import com.xrcgs.iam.model.query.DictItemPageQuery;
import com.xrcgs.iam.model.query.DictTypePageQuery;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
// ↓↓↓ 按你项目实际包名修改
import com.xrcgs.common.core.R;

import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典控制器
 * 类型：iam:dict:type:list|create|update|delete
 * 项：iam:dict:item:create|update|delete
 * 查询：iam:dict:get
 */
@Validated
@RestController
@RequestMapping("/api/iam/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /* ---------- 字典类型 ---------- */

    /**
     * 字典类型清单：支持 code/name 关键字模糊、状态与创建时间区间过滤。
     * 若前端希望分页，可在查询层自行封装；此接口返回完整列表。
     */
    @GetMapping("/type")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:list')")
    public R<List<SysDictType>> listTypes(@Valid DictTypePageQuery q) {
        return R.ok(dictService.listTypes(q));
    }

    /** 字典项分页：按 typeCode + label 模糊 */
    @GetMapping("/item/page")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:list')")
    public com.xrcgs.common.core.R<Page<SysDictItem>> itemPage(
            @Valid DictItemPageQuery q,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(dictService.pageItems(q, pageNo, pageSize));
    }

    @PostMapping("/type")
    @OpLog("新增字典类型")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:create')")
    public R<Long> createType(@Valid @RequestBody SysDictType type) {
        Long id = dictService.createType(type);
        return R.ok(id);
    }

    @PutMapping("/type/{id}")
    @OpLog("修改字典类型")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:update')")
    public R<Boolean> updateType(@PathVariable @NotNull Long id,
                                 @Valid @RequestBody SysDictType type) {
        type.setId(id);
        dictService.updateType(type);
        return R.ok(true);
    }

    @DeleteMapping("/type/{id}")
    @OpLog("删除字典类型")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:delete')")
    public R<Boolean> removeType(@PathVariable @NotNull Long id) {
        dictService.removeType(id);
        return R.ok(true);
    }

    /* ---------- 字典项 ---------- */

    @PostMapping("/item")
    @OpLog("新增字典项")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:create')")
    public R<Long> createItem(@Valid @RequestBody SysDictItem item) {
        Long id = dictService.createItem(item);
        return R.ok(id);
    }

    @PutMapping("/item/{id}")
    @OpLog("修改字典项")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:update')")
    public R<Boolean> updateItem(@PathVariable @NotNull Long id,
                                 @Valid @RequestBody SysDictItem item) {
        item.setId(id);
        dictService.updateItem(item);
        return R.ok(true);
    }

    @DeleteMapping("/item/{id}")
    @OpLog("删除字典项")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:delete')")
    public R<Boolean> removeItem(@PathVariable @NotNull Long id) {
        dictService.removeItem(id);
        return R.ok(true);
    }

    @PostMapping("/sync/cache")
    @OpLog("手动同步字典缓存")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:create')")
    public R<Boolean> syncCache() {
        dictService.syncAllDictCache();
        return R.ok(true);
    }

    /* ---------- 查询：按 typeCode ---------- */

    @GetMapping("/{typeCode}")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dict:get')")
    public R<DictVO> getByType(@PathVariable @NotBlank String typeCode) {
        return R.ok(dictService.getByType(typeCode));
    }
}
