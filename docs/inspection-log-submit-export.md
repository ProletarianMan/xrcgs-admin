# 巡查日志提交并生成文件接口

## 新接口

- `POST /api/road-safety/inspection/logs/submit-export`
- 请求体：前端全量 payload（含 `team`、`shiftSchedule`、`details` 扩展字段）
- 响应：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 二进制下载

## 与旧接口关系

- 旧接口 `POST /api/road-safety/inspection/logs/export` 保持兼容，不做破坏性变更。
- 新接口用于“提交并生成文件”（入库 + 导出）。

## 映射规则

- 接收层 DTO：`InspectionLogSubmitExportRequest`
- 导出层 DTO：`CanonicalInspectionExportModel`
- 映射器：`InspectionLogSubmitExportMapper`

### 明细类型兼容

- `OVERSIZE_CHECK -> OVERSIZE`
- `OVERLIMIT_HANDLING -> OVERLOAD`
- `INFRINGEMENT -> VIOLATION`

### 巡查、处理情况正文规则

- 每个分类正文仅使用 `details[].summaryText`
- `summaryText` 为空时，按策略 B 输出 `无`
- 除 `summaryText` 外的明细字段（`time/location/station/plateNos/responsibles/...`）不写入分类正文，只入库

## 入库字段

`road_inspection_record` 新增：

- `form_payload_json`：完整原始提交体
- `details_payload_json`：原始 `details` 快照（可选审计）
- `summary_payload_json`：标准化分类摘要快照（可选审计）

同时继续复用原有索引字段：`record_date`、`squad_code`、`approval_status`、`export_file_name` 等。

## 请求示例（节选）

```json
{
  "date": "2026-03-06",
  "team": "WUJIAQU",
  "shiftType": "BOTH",
  "routes": ["G30-五家渠段"],
  "vehicle": "新A12345",
  "weather": "SUNNY",
  "details": [
    {
      "category": "ACCIDENT_RESCUE",
      "type": "ACCIDENT",
      "summaryText": "10:30 上行K150+200发生事故，已处置"
    }
  ],
  "handover": {
    "inspectors": ["zhangsan"]
  },
  "fileName": "巡查日志",
  "draft": false
}
```

