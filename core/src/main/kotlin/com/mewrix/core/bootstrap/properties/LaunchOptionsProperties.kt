package com.mewrix.core.bootstrap.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "flowloom.launch")
data class LaunchOptionsProperties(
    val home: String
)
