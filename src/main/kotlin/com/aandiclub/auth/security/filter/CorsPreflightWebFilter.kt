package com.aandiclub.auth.security.filter

import com.aandiclub.auth.security.config.AppCorsProperties
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CorsPreflightWebFilter(
	private val corsProperties: AppCorsProperties,
) : WebFilter, Ordered {
	override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		if (request.method != HttpMethod.OPTIONS) {
			return chain.filter(exchange)
		}

		val origin = request.headers.origin ?: return chain.filter(exchange)
		val requestedMethod = request.headers.getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)?.uppercase()
			?: return chain.filter(exchange)

		val allowedOrigins = corsProperties.allowedOriginsList()
		val allowedMethods = corsProperties.allowedMethodsList().map { it.uppercase() }
		if (!allowedOrigins.contains(origin) || !allowedMethods.contains(requestedMethod)) {
			exchange.response.statusCode = HttpStatus.FORBIDDEN
			return exchange.response.setComplete()
		}

		val responseHeaders = exchange.response.headers
		responseHeaders.add(HttpHeaders.VARY, "Origin")
		responseHeaders.add(HttpHeaders.VARY, "Access-Control-Request-Method")
		responseHeaders.add(HttpHeaders.VARY, "Access-Control-Request-Headers")
		responseHeaders.accessControlAllowOrigin = origin
		responseHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods.joinToString(","))
		responseHeaders.add(
			HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
			corsProperties.allowedHeadersList().joinToString(","),
		)
		if (corsProperties.allowCredentials) {
			responseHeaders.accessControlAllowCredentials = true
		}
		responseHeaders.accessControlMaxAge = corsProperties.maxAgeSeconds

		exchange.response.statusCode = HttpStatus.OK
		return exchange.response.setComplete()
	}
}
