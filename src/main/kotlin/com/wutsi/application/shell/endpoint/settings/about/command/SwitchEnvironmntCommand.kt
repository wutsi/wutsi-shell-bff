package com.wutsi.application.shell.endpoint.settings.about.command

import com.wutsi.application.shell.endpoint.AbstractCommand
import com.wutsi.application.shell.exception.AccountAlreadyLinkedException
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/switch-environment")
class SwitchEnvironmntCommand : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam environment: String
    ): ResponseEntity<Action> {
        val headers = HttpHeaders()
        headers.add("x-environment", environment)
        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                Action(
                    type = ActionType.Route,
                    url = "route:/"
                )
            )
    }

    @ExceptionHandler(AccountAlreadyLinkedException::class)
    fun onAccountAlreadyLinkedException(ex: AccountAlreadyLinkedException): Action =
        createErrorAction(ex, "page.verify-account-mobile.error.already-linked")
}
