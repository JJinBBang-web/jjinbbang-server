package com.jjinbbang.server.admin.administrator.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.administrator.dto.AdminSessionResponse;
import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.exception.AdminAuthenticationErrorCode;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

	private final AdminRepository adminRepository;

	@Transactional(readOnly = true)
	public AdminSessionResponse getSession(Long adminId) {
		Admin admin = adminRepository.findById(adminId)
			.orElseThrow(AdminAuthenticationErrorCode.AUTHENTICATION_REQUIRED::exception);
		return AdminSessionResponse.from(admin);
	}
}
