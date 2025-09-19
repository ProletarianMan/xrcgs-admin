package com.xrcgs.iam.service;

import com.xrcgs.iam.model.dto.DeptUpsertDTO;
import com.xrcgs.iam.model.vo.DeptTreeVO;
import com.xrcgs.iam.model.vo.DeptVO;

import java.util.List;

public interface DeptService {

    Long create(DeptUpsertDTO dto);

    void update(Long id, DeptUpsertDTO dto);

    void delete(Long id);

    DeptVO detail(Long id);

    List<DeptTreeVO> tree(Integer status);
}
