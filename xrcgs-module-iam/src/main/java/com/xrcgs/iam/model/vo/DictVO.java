package com.xrcgs.iam.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 原生属性
 */
@Data
public class DictVO {
    private String type; // typeCode
    @Data
    public static class Item {
        private String label;
        private String value;
        private Integer sort;
        private String ext;
    }
    private List<Item> items;
}
