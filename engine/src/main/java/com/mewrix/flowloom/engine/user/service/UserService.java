package com.mewrix.flowloom.engine.user.service;

import com.mewrix.flowloom.engine.common.response.BusinessCode;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import com.mewrix.flowloom.engine.user.dto.response.UserRegisterResponse;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/** 处理用户注册及相关业务校验。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 用户展示名限制
    private static final Pattern INVALID_DISPLAY_NAME_PATTERN = Pattern.compile("[\\p{Cc}]");

    // 用户邮箱验证正则匹配模式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // 用户名格式限制
    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 32;
    private static final Pattern INVALID_USERNAME_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Pattern INVALID_USERNAME_START_PATTERN = Pattern.compile("^[^a-zA-Z]");

    // 密码格式限制
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 128;

    /**
     * 校验注册信息、哈希密码并创建用户。
     *
     * @param displayName 用户展示名
     * @param username    用户名
     * @param email       邮箱地址
     * @param password    明文密码，仅用于生成密码哈希
     * @return 注册结果；当前成功结果不包含数据
     */
    public BusinessResult<UserRegisterResponse> register(
            String displayName,
            String username,
            String email,
            String password) {

        // 校验用户信息是否合法
        BusinessResult<Void> validaResult;
        validaResult = validateUsername(username);
        if (!validaResult.isSuccess()) {
            return BusinessResult.fail(validaResult.code());
        }

        validaResult = validateEmail(email);
        if (!validaResult.isSuccess()) {
            return BusinessResult.fail(validaResult.code());
        }

        if (!isValidDisplayName(displayName)) {
            return BusinessResult.fail(BusinessCode.DISPLAY_NAME_INVALID);
        }

        BusinessResult<Void> passwordValidationResult = validatePassword(password);
        if (!passwordValidationResult.isSuccess()) {
            return BusinessResult.fail(passwordValidationResult.code());
        }

        // 使用`Argon2id`加密密码
        String passwordHash = passwordEncoder.encode(password);

        // 主键和时间字段由数据库生成，插入结果表示受影响的行数
        User user = new User();
        user.setDisplayName(displayName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);

        try {
            if (userMapper.insertUser(user) <= 0) {
                return BusinessResult.fail(BusinessCode.USER_CREATION_FAILED);
            }
        } catch (DataIntegrityViolationException e) {
            // 唯一性预检查与插入之间可能有并发请求，最终由数据库约束保证唯一性
            log.error("Data integrity violation while creating user: {}", e.getMessage());
            return BusinessResult.fail(BusinessCode.USER_CREATION_FAILED);
        }

        return BusinessResult.success();
    }

    /** 校验用户名格式及其是否已存在。 */
    private BusinessResult<Void> validateUsername(String username) {
        if (username.length() < USERNAME_MIN_LENGTH) {
            return BusinessResult.fail(BusinessCode.USERNAME_TOO_SHORT);
        }

        if (username.length() > USERNAME_MAX_LENGTH) {
            return BusinessResult.fail(BusinessCode.USERNAME_TOO_LONG);
        }

        if (INVALID_USERNAME_PATTERN.matcher(username).find()) {
            return BusinessResult.fail(BusinessCode.USERNAME_HAS_ILLEGAL_CHARACTERS);
        }

        if (INVALID_USERNAME_START_PATTERN.matcher(username).find()) {
            return BusinessResult.fail(BusinessCode.USERNAME_MUST_START_WITH_LETTER);
        }

        if (userMapper.findByUsername(username) != null) {
            return BusinessResult.fail(BusinessCode.USERNAME_ALREADY_EXISTS);
        }

        return BusinessResult.success();
    }

    /** 校验邮箱格式及其是否已存在。 */
    private BusinessResult<Void> validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).find()) {
            return BusinessResult.fail(BusinessCode.EMAIL_INVALID);
        }

        if (userMapper.findByEmail(email) != null) {
            return BusinessResult.fail(BusinessCode.EMAIL_ALREADY_EXISTS);
        }

        return BusinessResult.success();
    }

    /** 判断展示名是否不包含控制字符。 */
    private static boolean isValidDisplayName(String displayName) {
        return !INVALID_DISPLAY_NAME_PATTERN.matcher(displayName).find();
    }

    /** 校验密码长度。 */
    private BusinessResult<Void> validatePassword(String password) {
        if (password.length() < PASSWORD_MIN_LENGTH) {
            return BusinessResult.fail(BusinessCode.PASSWORD_TOO_SHORT);
        }

        if (password.length() > PASSWORD_MAX_LENGTH) {
            return BusinessResult.fail(BusinessCode.PASSWORD_TOO_LONG);
        }

        return BusinessResult.success();
    }
}
