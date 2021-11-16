package com.wutsi.application.home.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.home.dto.SendSmsCodeRequest
import com.wutsi.application.home.dto.VerifySmsCodeRequest
import com.wutsi.application.home.entity.SmsCodeEntity
import com.wutsi.application.home.exception.AccountAlreadyLinkedException
import com.wutsi.application.home.exception.InvalidPhoneNumberException
import com.wutsi.application.home.exception.SmsCodeMismatchException
import com.wutsi.application.home.exception.toErrorResponse
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AddPaymentMethodRequest
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.tracing.DeviceIdProvider
import com.wutsi.platform.payment.PaymentMethodProvider
import com.wutsi.platform.payment.PaymentMethodType
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendVerificationRequest
import com.wutsi.platform.tenant.dto.MobileCarrier
import com.wutsi.platform.tenant.dto.Tenant
import feign.FeignException
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.web.servlet.LocaleResolver
import javax.servlet.http.HttpServletRequest

@Service
class AccountService(
    private val tenantProvider: TenantProvider,
    private val smsApi: WutsiSmsApi,
    private val accountApi: WutsiAccountApi,
    private val cacheManager: CacheManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val httpServletRequest: HttpServletRequest,
    private val localeResolver: LocaleResolver,
    private val togglesProvider: TogglesProvider,
    private val userProvider: UserProvider,
    private val logger: KVLogger,
    private val objectMapper: ObjectMapper,

    @Value("\${wutsi.platform.cache.name}") private val cacheName: String,
) {
    companion object {
        const val ERROR_ACCOUNT_OWNERSHIP = "urn:error:wutsi:account:payment-method-ownership"
    }

    fun sendVerificationCode(request: SendSmsCodeRequest) {
        logger.add("phone_number", request.phoneNumber)

        val tenant = tenantProvider.get()
        val carrier = findCarrier(request.phoneNumber, tenant)
            ?: throw InvalidPhoneNumberException()

        val verificationId = sendVerificationCode(request.phoneNumber)
        logger.add("verification_id", verificationId)
        storeVerificationNumber(request.phoneNumber, verificationId, carrier.code)
    }

    fun resentVerificationCode() {
        val state = getSmsCodeEntity()
        log(state)

        val verificationId = sendVerificationCode(state.phoneNumber)
        logger.add("verification_id", verificationId)
        storeVerificationNumber(state.phoneNumber, verificationId, state.carrier)
    }

    fun verifyCode(request: VerifySmsCodeRequest) {
        val state = getSmsCodeEntity()
        log(state)
        logger.add("verification_code", request.code)

        if (togglesProvider.get().verifySmsCode) {
            try {
                smsApi.validateVerification(
                    id = state.verificationId,
                    code = request.code
                )
            } catch (ex: Exception) {
                throw SmsCodeMismatchException(ex)
            }
        }
    }

    fun linkAccount(type: PaymentMethodType) {
        try {
            val state = getSmsCodeEntity()
            log(state)

            val principal = userProvider.principal()
            val response = accountApi.addPaymentMethod(
                principal.id.toLong(),
                request = AddPaymentMethodRequest(
                    ownerName = principal.name,
                    phoneNumber = state.phoneNumber,
                    type = type.name,
                    provider = toPaymentProvider(state.carrier)!!.name
                )
            )
            logger.add("payment_method_token", response.token)
        } catch (ex: FeignException) {
            val code = ex.toErrorResponse(objectMapper)?.error?.code ?: throw ex
            if (code == ERROR_ACCOUNT_OWNERSHIP)
                throw AccountAlreadyLinkedException(ex)
            else
                throw ex
        }
    }

    fun getPaymentMethods(tenant: Tenant): List<PaymentMethodSummary> {
        val userId = userProvider.id()
        return accountApi.listPaymentMethods(userId).paymentMethods
            .filter { findMobileCarrier(tenant, it) != null }
    }

    fun getLogoUrl(tenant: Tenant, paymentMethod: PaymentMethodSummary): String? {
        val carrier = findMobileCarrier(tenant, paymentMethod)
        if (carrier != null) {
            return tenantProvider.logo(carrier)
        }
        return null
    }

    private fun log(state: SmsCodeEntity) {
        logger.add("phone_carrier", state.carrier)
        logger.add("phone_number", state.phoneNumber)
        logger.add("verification_id", state.verificationId)
    }

    private fun findMobileCarrier(tenant: Tenant, paymentMethod: PaymentMethodSummary): MobileCarrier? =
        tenant.mobileCarriers.find { it.code.equals(paymentMethod.provider, true) }

    fun getSmsCodeEntity(): SmsCodeEntity =
        cacheManager.getCache(cacheName).get(cacheKey(), SmsCodeEntity::class.java)

    private fun storeVerificationNumber(phoneNumber: String, verificationId: Long, carrier: String) {
        cacheManager.getCache(cacheName).put(
            cacheKey(),
            SmsCodeEntity(
                phoneNumber = phoneNumber,
                carrier = carrier,
                verificationId = verificationId
            )
        )
    }

    private fun toPaymentProvider(carrier: String): PaymentMethodProvider? =
        PaymentMethodProvider.values().find { it.name.equals(carrier, ignoreCase = true) }

    private fun sendVerificationCode(phoneNumber: String): Long {
        if (!togglesProvider.get().sendSmsCode)
            return -1

        return smsApi.sendVerification(
            SendVerificationRequest(
                phoneNumber = phoneNumber,
                language = localeResolver.resolveLocale(httpServletRequest).language
            )
        ).id
    }

    private fun findCarrier(phoneNumber: String, tenant: Tenant): MobileCarrier? {
        val carriers = tenantProvider.mobileCarriers(tenant)
        return carriers.find { hasPrefix(phoneNumber, it) }
    }

    private fun hasPrefix(phoneNumber: String, carrier: MobileCarrier): Boolean =
        carrier.phonePrefixes.flatMap { it.prefixes }
            .find { phoneNumber.startsWith(it) } != null

    private fun cacheKey(): String =
        "verification-code-" + deviceIdProvider.get(httpServletRequest)
}
