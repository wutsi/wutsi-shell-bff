package com.wutsi.application.shell.endpoint.profile.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.application.shell.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.catalog.WutsiCatalogApi
import com.wutsi.platform.catalog.dto.PictureSummary
import com.wutsi.platform.catalog.dto.ProductSummary
import com.wutsi.platform.catalog.dto.SearchProductResponse
import com.wutsi.platform.contact.WutsiContactApi
import com.wutsi.platform.contact.dto.SearchContactResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ProfileScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @MockBean
    private lateinit var contactApi: WutsiContactApi

    @MockBean
    private lateinit var togglesProvider: TogglesProvider

    @MockBean
    private lateinit var catalogApi: WutsiCatalogApi

    @Test
    fun personal() {
        // GIVEN
        doReturn(SearchContactResponse()).whenever(contactApi).searchContact(any())

        val account = Account(
            id = 5555,
            displayName = "Ray Sponsible",
            country = "CM",
            language = "en",
            status = "ACTIVE",
            phone = Phone(
                id = 1,
                number = "+1237666666666",
                country = "CM"
            ),
            business = false,
        )
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(555L)

        // WHEN
        val url = "http://localhost:$port/profile?id=555"
        val response = rest.postForEntity(url, null, Any::class.java)

        // THEN
        assertJsonEquals("/screens/profile/personal.json", response.body)
    }

    @Test
    fun businessStoreEnabled() {
        // GIVEN
        doReturn(true).whenever(togglesProvider).isBusinessAccountEnabled()
        doReturn(true).whenever(togglesProvider).isStoreEnabled()

        doReturn(SearchContactResponse()).whenever(contactApi).searchContact(any())

        val products = listOf(createProductSummary(1), createProductSummary(2))
        doReturn(SearchProductResponse(products)).whenever(catalogApi).searchProducts(any())

        val account = createAccount(5555, true)
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(555L)

        // WHEN
        val url = "http://localhost:$port/profile?id=555"
        val response = rest.postForEntity(url, null, Any::class.java)

        // THEN
        assertJsonEquals("/screens/profile/business-store-enabled.json", response.body)
    }

    @Test
    fun business() {
        // GIVEN
        doReturn(true).whenever(togglesProvider).isBusinessAccountEnabled()
        doReturn(SearchContactResponse()).whenever(contactApi).searchContact(any())

        val account = createAccount(5555, true)
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(555L)

        // WHEN
        val url = "http://localhost:$port/profile?id=555"
        val response = rest.postForEntity(url, null, Any::class.java)

        // THEN
        assertJsonEquals("/screens/profile/business.json", response.body)
    }

    private fun createAccount(id: Long, business: Boolean) = Account(
        id = id,
        displayName = "Ray Sponsible",
        country = "CM",
        language = "en",
        status = "ACTIVE",
        phone = Phone(
            id = 1,
            number = "+1237666666666",
            country = "CM"
        ),
        pictureUrl = "https://img.com/1.png",
        business = business,
        retail = true,
        biography = "This is my bio",
        categoryId = 1000L,
        website = "https://my.business.com/12432",
        whatsapp = "+23500000000"
    )

    protected fun createProductSummary(id: Long) = ProductSummary(
        id = id,
        title = "Sample product",
        summary = "Summary of product",
        price = 7000.0,
        comparablePrice = 10000.0,
        thumbnail = PictureSummary(
            id = 3,
            url = "https://www.imag.com/$id.png"
        )
    )
}
