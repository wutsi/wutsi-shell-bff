package com.wutsi.application.shell.endpoint.scan.screen

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.shell.endpoint.AbstractQuery
import com.wutsi.application.shell.endpoint.Page
import com.wutsi.application.shell.endpoint.Theme
import com.wutsi.application.shell.endpoint.scan.dto.ScanRequest
import com.wutsi.application.shell.exception.toErrorResponse
import com.wutsi.application.shell.service.TenantProvider
import com.wutsi.application.shell.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Center
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.QrImage
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.platform.qr.WutsiQrApi
import com.wutsi.platform.qr.dto.DecodeQRCodeRequest
import com.wutsi.platform.qr.dto.Entity
import com.wutsi.platform.qr.error.ErrorURN
import feign.FeignException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/scan/viewer")
class ScanViewerScreen(
    private val urlBuilder: URLBuilder,
    private val qrApi: WutsiQrApi,
    private val tenantProvider: TenantProvider,
    private val mapper: ObjectMapper,

    @Value("\${wutsi.application.cash-url}") private val cashUrl: String,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestBody request: ScanRequest): Widget {
        logger.add("code", request.code)
        logger.add("format", request.format)

        // Parse the qr-code
        var error: String? = null
        var nextUrl: String? = null
        var entity: Entity? = null
        try {
            entity = qrApi.decode(
                DecodeQRCodeRequest(
                    token = request.code
                )
            ).entity
            nextUrl = nextUrl(entity)
        } catch (ex: FeignException) {
            val response = ex.toErrorResponse(mapper)
            error = if (response?.error?.code == ErrorURN.EXPIRED.urn)
                getText("prompt.error.expired-qr-code")
            else if (response?.error?.code == ErrorURN.MALFORMED_TOKEN.urn)
                getText("prompt.error.malformed-qr-code")
            else
                getText("prompt.error.unexpected-error")
        }

        // Viewer
        return Screen(
            id = Page.SCAN_VIEWER,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.scan-viewer.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Center(
                        child = QrImage(
                            data = request.code,
                            size = 230.0,
                            padding = 10.0,
                            embeddedImageSize = 64.0,
                            embeddedImageUrl = if (includeEmbeddedImage(entity))
                                tenantProvider.get().logos.find { it.type == "PICTORIAL" }?.url
                            else
                                null
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = if (error == null)
                            Icon(Theme.ICON_CHECK, color = Theme.COLOR_SUCCESS, size = 64.0)
                        else
                            Icon(Theme.ICON_ERROR, color = Theme.COLOR_DANGER, size = 64.0)
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Text(
                            error?.let { it } ?: getText("page.scan-viewer.valid"),
                            size = Theme.TEXT_SIZE_LARGE
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = nextButton(nextUrl, entity)
                    )
                )
            )
        ).toWidget()
    }

    private fun includeEmbeddedImage(entity: Entity?): Boolean =
        entity?.type == "payment-request" || entity?.type == "account" || entity?.type == "web"

    private fun nextUrl(entity: Entity?): String? =
        if (entity?.type == "payment-request")
            urlBuilder.build(cashUrl, "pay/confirm?payment-request-id=${entity.id}")
        else if (entity?.type == "account")
            urlBuilder.build("profile?id=${entity.id}")
        else if (entity?.type == "url")
            entity.id
        else
            null

    private fun nextButton(nextUrl: String?, entity: Entity?): WidgetAware =
        if (nextUrl == null)
            Button(
                caption = getText("page.scan-viewer.button.close"),
                type = ButtonType.Text,
                action = Action(
                    type = ActionType.Route,
                    url = "route:/~"
                )
            )
        else
            Button(
                caption = when (entity?.type?.lowercase()) {
                    "account" -> getText("page.scan-viewer.button.continue-account")
                    "payment-request" -> getText("page.scan-viewer.button.continue-payment")
                    "url" -> getText("page.scan-viewer.button.continue-url")
                    else -> getText("page.scan-viewer.button.continue")
                },
                action = Action(
                    type = if (entity?.type?.lowercase() == "url") ActionType.Navigate else ActionType.Route,
                    url = nextUrl
                )
            )
}
