package com.aandiclub.auth.common.service

import org.springframework.stereotype.Service

@Service
class PingService {
	fun ping(): String = "pong"
}
