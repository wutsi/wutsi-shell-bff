package com.wutsi.application.shell.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

@ConfigurationProperties(prefix = "wutsi.toggles")
class Toggles {
    var sendSmsCode: Boolean = true
    var verifySmsCode: Boolean = true
    var buttonScan: Boolean = false
}

@Service
@EnableConfigurationProperties(Toggles::class)
class TogglesProvider(
    private val toggles: Toggles,
    private val userProvider: UserProvider
) {
    fun get() = toggles
}
