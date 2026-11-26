package com.mewrix.flowloom.core.bootstrap.state

/**
 * 随实例配置持久化的初始化状态。新实例从 [UNINITIALIZED] 开始，
 * 创建初始管理员账户不会推进此状态。
 */
enum class SetupState {
    UNINITIALIZED,
    INITIALIZED,
}
