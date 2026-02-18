package com.aandiclub.auth.admin.service

import com.aandiclub.auth.admin.web.dto.AdminUserSummary
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.CreateAdminUserResponse
import reactor.core.publisher.Mono

interface AdminService {
	fun getUsers(): Mono<List<AdminUserSummary>>
	fun createUser(request: CreateAdminUserRequest): Mono<CreateAdminUserResponse>
}
