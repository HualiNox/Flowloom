package com.mewrix.flowloom.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class FlowloomCoreApplication

fun main(args: Array<String>) {
    val launchOptions = LaunchOptions.parse(args)

    runApplication<FlowloomCoreApplication>(*args) {
        setDefaultProperties(launchOptions.toMap())
    }
}