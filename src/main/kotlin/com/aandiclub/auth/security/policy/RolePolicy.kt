package com.aandiclub.auth.security.policy

import com.aandiclub.auth.user.domain.UserRole
import org.springframework.stereotype.Component

@Component
class RolePolicy {
	fun canAccess(requiredRole: UserRole, currentRole: UserRole): Boolean =
		currentRole.level >= requiredRole.level
}
