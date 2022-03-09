package com.wutsi.application.shell.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.shared.service.FeignAcceptLanguageInterceptor
import com.wutsi.ecommerce.cart.WutsiCartApi
import com.wutsi.ecommerce.cart.WutsiCartApiBuilder
import com.wutsi.platform.core.security.feign.FeignAuthorizationRequestInterceptor
import com.wutsi.platform.core.tracing.feign.FeignTracingRequestInterceptor
import com.wutsi.platform.core.util.feign.Custom5XXErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
class CartApiConfiguration(
    private val authorizationRequestInterceptor: FeignAuthorizationRequestInterceptor,
    private val tracingRequestInterceptor: FeignTracingRequestInterceptor,
    private val acceptLanguageInterceptor: FeignAcceptLanguageInterceptor,
    private val mapper: ObjectMapper,
    private val env: Environment
) {
    @Bean
    fun cartApi(): WutsiCartApi =
        WutsiCartApiBuilder().build(
            env = environment(),
            mapper = mapper,
            interceptors = listOf(
                tracingRequestInterceptor,
                authorizationRequestInterceptor,
                acceptLanguageInterceptor
            ),
            errorDecoder = Custom5XXErrorDecoder()
        )

    private fun environment(): com.wutsi.ecommerce.cart.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            com.wutsi.ecommerce.cart.Environment.PRODUCTION
        else
            com.wutsi.ecommerce.cart.Environment.SANDBOX
}
