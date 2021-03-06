package com.wutsi.application.shell.endpoint.settings.account.dto

import javax.validation.constraints.NotEmpty

data class VerifySmsCodeRequest(
    @get:NotEmpty
    val code: String = ""
)
