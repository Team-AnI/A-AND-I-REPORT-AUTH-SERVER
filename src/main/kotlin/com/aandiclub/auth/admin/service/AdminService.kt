package com.aandiclub.auth.admin.service

import com.aandiclub.auth.admin.web.dto.AdminUserSummary
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.CreateAdminUserResponse
import com.aandiclub.auth.admin.web.dto.ResetPasswordResponse
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminService {
	fun getUsers(): Mono<List<AdminUserSummary>>
	fun createUser(request: CreateAdminUserRequest): Mono<CreateAdminUserResponse>
	fun resetPassword(userId: UUID): Mono<ResetPasswordResponse>
	fun deleteUser(targetUserId: UUID, actorUserId: UUID): Mono<Void>
}
