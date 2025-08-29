package com.xrcgs.file.service;

public interface FileConvertService {

    /**
     * 针对可转换的文档（doc/x/docx/xls/xlsx/ppt/pptx）发起异步转 PDF。
     * @return 提交是否成功（不代表立即完成）
     */
    boolean convertToPdfAsync(Long fileId);
}
