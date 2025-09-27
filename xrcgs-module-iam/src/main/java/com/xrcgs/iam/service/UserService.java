package com.xrcgs.iam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.UserVO;

import java.util.List;

public interface UserService {

    Page<UserVO> page(UserPageQuery q, long pageNo, long pageSize);

    UserVO detail(Long id);

    Long create(UserUpsertDTO dto);

    void update(Long id, UserUpsertDTO dto);

    void delete(Long id);

    void updateEnabled(Long id, boolean enabled);

    void resetPassword(Long id, String rawPassword);

    void assignRoles(Long id, List<Long> roleIds);

    List<UserVO> listByNicknameSuffix(String nickname);
}

