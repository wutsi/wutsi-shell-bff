package com.wutsi.application.shell.endpoint.contact.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.shell.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.contact.WutsiContactApi
import com.wutsi.platform.contact.dto.ContactSummary
import com.wutsi.platform.contact.dto.SearchContactResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ContactScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var contactApi: WutsiContactApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/contact"
    }

    @Test
    fun index() {
        val contacts = listOf(
            createContact(100),
            createContact(101)
        )
        doReturn(SearchContactResponse(contacts)).whenever(contactApi).searchContact(any())

        val accounts = listOf(
            createAccount(100, "Roger Milla"),
            createAccount(101, "Omam Mbiyick")
        )
        doReturn(SearchAccountResponse(accounts)).whenever(accountApi).searchAccount(any())

        assertEndpointEquals("/screens/contact/contact.json", url)
    }

    private fun createContact(contactId: Long) = ContactSummary(
        accountId = USER_ID,
        contactId = contactId
    )

    private fun createAccount(id: Long, displayName: String) = AccountSummary(
        id = id,
        displayName = displayName
    )
}
