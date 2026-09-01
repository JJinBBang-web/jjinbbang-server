package com.jjinbbang.server.admin.administrator.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.exception.AdminAuthenticationErrorCode;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;

@Service
public class AdminOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private static final String GROUPS_CLAIM = "groups";

	private final AdminRepository adminRepository;
	private final String requiredGroup;
	private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

	@Autowired
	public AdminOidcUserService(
		AdminRepository adminRepository,
		@Value("${app.auth.admin-group:jjinbbang-backoffice-admins}") String requiredGroup
	) {
		this(adminRepository, requiredGroup, new OidcUserService());
	}

	AdminOidcUserService(
		AdminRepository adminRepository,
		String requiredGroup,
		OAuth2UserService<OidcUserRequest, OidcUser> delegate
	) {
		this.adminRepository = adminRepository;
		this.requiredGroup = requiredGroup;
		this.delegate = delegate;
	}

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = delegate.loadUser(userRequest);
		requireAdminGroup(oidcUser);

		String issuer = oidcUser.getIssuer().toString();
		String subject = oidcUser.getSubject();
		LocalDateTime loginAt = LocalDateTime.now(ZoneOffset.UTC);

		Admin admin = adminRepository.findByOidcIssuerAndOidcSubject(issuer, subject)
			.orElseGet(() -> Admin.register(
				issuer,
				subject,
				oidcUser.getEmail(),
				oidcUser.getPreferredUsername(),
				resolveDisplayName(oidcUser),
				loginAt
			));

		if (admin.getStatus() == AdminStatus.DEACTIVATED) {
			throw authenticationFailure(AdminAuthenticationErrorCode.ADMIN_DEACTIVATED);
		}

		admin.synchronizeProfile(
			oidcUser.getEmail(),
			oidcUser.getPreferredUsername(),
			resolveDisplayName(oidcUser),
			loginAt
		);
		Admin savedAdmin = adminRepository.save(admin);

		Set<org.springframework.security.core.GrantedAuthority> authorities =
			new LinkedHashSet<>(oidcUser.getAuthorities());
		authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

		OidcUser authorizedUser = new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
			authorities,
			oidcUser.getIdToken(),
			oidcUser.getUserInfo(),
			"sub"
		);
		return new AdminOidcUser(authorizedUser, savedAdmin.getId());
	}

	private void requireAdminGroup(OidcUser oidcUser) {
		Object groups = oidcUser.getClaims().get(GROUPS_CLAIM);
		if (!(groups instanceof Collection<?> values) || !values.contains(requiredGroup)) {
			throw authenticationFailure(AdminAuthenticationErrorCode.ADMIN_GROUP_REQUIRED);
		}
	}

	private String resolveDisplayName(OidcUser oidcUser) {
		return oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername();
	}

	private OAuth2AuthenticationException authenticationFailure(AdminAuthenticationErrorCode errorCode) {
		return new OAuth2AuthenticationException(
			new OAuth2Error(errorCode.getCode()),
			errorCode.getMessage()
		);
	}
}
