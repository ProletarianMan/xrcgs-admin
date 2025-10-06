package com.xrcgs.common.enums;

/**
 * 审批状态枚举，供巡查日志等业务统一引用。
 */
public enum ApprovalStatus {
    /** 未提交审批。 */
    UNSUBMITTED("未提交"),
    /** 已提交，正在审批中。 */
    IN_PROGRESS("审批中"),
    /** 审批退回，需要重新处理。 */
    REJECTED("退回"),
    /** 审批通过。 */
    APPROVED("通过"),
    /** 审批流程暂时搁置。 */
    ON_HOLD("搁置");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
