package com.wutsi.application.shell.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

@ConfigurationProperties(prefix = "wutsi.toggles")
class Toggles {
    var sendSmsCode: Boolean = true
    var verifySmsCode: Boolean = true
    var payment: Boolean = true
    var scan: Boolean = true
    var account: Boolean = true
    var business: Boolean = true
    var testerUserIds: List<Long> = emptyList()
}

@Service
@EnableConfigurationProperties(Toggles::class)
class TogglesProvider(
    private val toggles: Toggles,
    private val userProvider: UserProvider
) {
    fun isPaymentEnabled(): Boolean = toggles.payment || isCurrentUserIsTester()

    fun isScanEnabled(): Boolean = toggles.scan || isCurrentUserIsTester()

    fun isSendSmsEnabled(): Boolean = toggles.sendSmsCode

    fun isVerifySmsCodeEnabled(): Boolean = toggles.verifySmsCode

    fun isAccountEnabled(): Boolean = toggles.account

    fun isBusinessAccountEnabled(): Boolean = toggles.business

    private fun isCurrentUserIsTester(): Boolean =
        toggles.testerUserIds.contains(userProvider.id())
}
