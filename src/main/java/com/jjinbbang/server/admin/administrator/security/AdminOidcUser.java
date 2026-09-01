package com.jjinbbang.server.admin.administrator.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class AdminOidcUser implements OidcUser, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final OidcUser delegate;
	private final Long adminId;

	public AdminOidcUser(OidcUser delegate, Long adminId) {
		this.delegate = delegate;
		this.adminId = adminId;
	}

	public Long getAdminId() {
		return adminId;
	}

	@Override
	public Map<String, Object> getClaims() {
		return delegate.getClaims();
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return delegate.getUserInfo();
	}

	@Override
	public OidcIdToken getIdToken() {
		return delegate.getIdToken();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return delegate.getAuthorities();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}
}
