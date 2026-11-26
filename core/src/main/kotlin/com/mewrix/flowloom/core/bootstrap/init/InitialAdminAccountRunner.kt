package com.mewrix.flowloom.core.bootstrap.init

import com.mewrix.flowloom.core.user.UserService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 在 Spring Boot 的启动回调中确保管理员账户存在。
 *
 * 只有这次启动新建了 admin 账户时，才会在终端打印提示：用户名，以及保存初始密码的文件路径。
 * 提示里不包含密码本身，避免密码进入日志。admin 账户已存在时什么都不打印。
 */
@Component
class InitialAdminAccountRunner(
    private val userService: UserService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val createdAccount = userService.createAdminAccountIfMissing() ?: return

        val border = "*".repeat(80)
        println(
            """

            $border
            $border
            $border

            Flowloom is being initialized for the first time.
            An administrator account has been created for you:

                Username: ${createdAccount.username}

            The generated password is stored in this file (plain text):

                ${createdAccount.passwordFile}

            $border
            $border
            $border

            """.trimIndent()
        )
    }
}
