package com.wutsi.application.shell.endpoint.settings.profile.screen

import com.wutsi.application.shell.endpoint.Page
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.InputType
import com.wutsi.platform.account.dto.Account
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings/profile/website")
class SettingsProfileWebsiteScreen : AbstractSettingsProfileAttributeScreen() {
    override fun getAttributeName() = "website"

    override fun getPageId() = Page.SETTINGS_PROFILE_WEBSITE

    override fun getInputWidget(account: Account): WidgetAware = Input(
        name = "value",
        value = account.website,
        type = InputType.Url,
        maxLength = 160
    )
}
