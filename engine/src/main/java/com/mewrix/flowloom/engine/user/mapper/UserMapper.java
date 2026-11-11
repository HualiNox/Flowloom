package com.mewrix.flowloom.engine.user.mapper;

import com.mewrix.flowloom.engine.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户记录的数据库访问接口。 */
@Mapper
public interface UserMapper {

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
}
