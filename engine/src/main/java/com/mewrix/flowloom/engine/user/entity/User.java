package com.mewrix.flowloom.engine.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** 对应 users 表的用户实体。 */
@Getter
@Setter
@TableName("users")
public class User {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    private String displayName;
    private String username;
    private String email;
    private String passwordHash;

    private LocalDateTime createdAt;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime updatedAt;
}
