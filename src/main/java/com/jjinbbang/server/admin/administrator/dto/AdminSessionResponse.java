package com.jjinbbang.server.admin.administrator.dto;

import com.jjinbbang.server.admin.administrator.entity.Admin;

public record AdminSessionResponse(
	Long id,
	String email,
	String username,
	String displayName
) {

	public static AdminSessionResponse from(Admin admin) {
		return new AdminSessionResponse(
			admin.getId(),
			admin.getEmail(),
			admin.getUsername(),
			admin.getDisplayName()
		);
	}
}
