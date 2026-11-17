package com.mewrix.flowloom.engine.user.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mewrix.flowloom.engine.user.entity.User;

/** 用户记录的数据库访问接口。 */
@Mapper
public interface UserMapper {

    /** 按主键查询用户；不存在时返回 null。 */
    User findById(@Param("id") UUID id);

    /** 按用户名查询用户，忽略大小写；不存在时返回 null。 */
    User findByUsername(@Param("username") String username);

    /** 按邮箱查询用户，忽略大小写；不存在时返回 null。 */
    User findByEmail(@Param("email") String email);

    /**
     * 插入用户，由数据库生成主键和时间字段。
     *
     * @param user 待插入的用户信息
     * @return 受影响的行数；此方法不会将生成的主键写回实体
     */
    int insertUser(User user);

    /**
     * 将用户标记为邮箱已验证（仅当尚未验证时更新）。
     *
     * @param id         用户 ID
     * @param verifiedAt 验证完成时间（业务时区）
     * @return 受影响的行数；已验证或用户不存在时为 0
     */
    int markEmailVerified(@Param("id") UUID id, @Param("verifiedAt") LocalDateTime verifiedAt);
}
