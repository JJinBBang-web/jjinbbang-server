package com.jjinbbang.server.admin.administrator.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.administrator.repository.AdminRepository;
import com.jjinbbang.server.admin.administrator.type.AdminStatus;

@DisplayName("관리자 OIDC 사용자 동기화")
class AdminOidcUserServiceTest {

	private static final String REQUIRED_GROUP = "jjinbbang-backoffice-admins";
	private static final String ISSUER = "https://auth.jjinbbang.kr/application/o/jjinbbang-admin/";
	private static final String SUBJECT = "authentik-user-id";

	@Test
	@DisplayName("허용 그룹의 첫 로그인은 관리자 계정을 만들고 ROLE_ADMIN을 부여한다")
	void firstLoginRegistersAdmin() throws Exception {
		AdminRepository repository = mock(AdminRepository.class);
		OidcUser upstreamUser = oidcUser(List.of(REQUIRED_GROUP));
		OidcUserRequest request = mock(OidcUserRequest.class);
		AdminOidcUserService service = new AdminOidcUserService(repository, REQUIRED_GROUP, ignored -> upstreamUser);

		when(repository.findByOidcIssuerAndOidcSubject(ISSUER, SUBJECT)).thenReturn(Optional.empty());
		when(repository.save(any(Admin.class))).thenAnswer(invocation -> {
			Admin admin = invocation.getArgument(0);
			ReflectionTestUtils.setField(admin, "id", 7L);
			return admin;
		});

		AdminOidcUser result = (AdminOidcUser) service.loadUser(request);

		assertThat(result.getAdminId()).isEqualTo(7L);
		assertThat(result.getAuthorities())
			.extracting("authority")
			.contains("ROLE_ADMIN");
		try (ObjectOutputStream output = new ObjectOutputStream(new ByteArrayOutputStream())) {
			output.writeObject(result);
		}
		verify(repository).save(any(Admin.class));
	}

	@Test
	@DisplayName("허용 그룹이 없으면 DB 계정을 만들지 않고 로그인을 거부한다")
	void missingGroupIsRejected() {
		AdminRepository repository = mock(AdminRepository.class);
		OidcUser upstreamUser = oidcUser(List.of("other-group"));
		AdminOidcUserService service = new AdminOidcUserService(
			repository,
			REQUIRED_GROUP,
			ignored -> upstreamUser
		);

		assertThatThrownBy(() -> service.loadUser(mock(OidcUserRequest.class)))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("관리자 그룹");
		verify(repository, never()).save(any(Admin.class));
	}

	@Test
	@DisplayName("비활성화된 로컬 관리자는 Authentik 인증이 성공해도 로그인을 거부한다")
	void deactivatedAdminIsRejected() {
		AdminRepository repository = mock(AdminRepository.class);
		Admin admin = Admin.register(
			ISSUER,
			SUBJECT,
			"admin@example.com",
			"admin",
			"관리자",
			LocalDateTime.now()
		);
		ReflectionTestUtils.setField(admin, "status", AdminStatus.DEACTIVATED);
		when(repository.findByOidcIssuerAndOidcSubject(ISSUER, SUBJECT)).thenReturn(Optional.of(admin));

		AdminOidcUserService service = new AdminOidcUserService(
			repository,
			REQUIRED_GROUP,
			ignored -> oidcUser(List.of(REQUIRED_GROUP))
		);

		assertThatThrownBy(() -> service.loadUser(mock(OidcUserRequest.class)))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("비활성화");
		verify(repository, never()).save(any(Admin.class));
	}

	private OidcUser oidcUser(List<String> groups) {
		OidcUser user = mock(OidcUser.class);
		Instant issuedAt = Instant.now();
		OidcIdToken idToken = new OidcIdToken(
			"token",
			issuedAt,
			issuedAt.plusSeconds(300),
			Map.of(
				"iss", ISSUER,
				"sub", SUBJECT,
				"groups", groups,
				"email", "admin@example.com",
				"preferred_username", "admin",
				"name", "찐빵 관리자"
			)
		);

		when(user.getIssuer()).thenReturn(issuerUrl());
		when(user.getSubject()).thenReturn(SUBJECT);
		when(user.getEmail()).thenReturn("admin@example.com");
		when(user.getPreferredUsername()).thenReturn("admin");
		when(user.getFullName()).thenReturn("찐빵 관리자");
		when(user.getClaims()).thenReturn(Map.of("groups", groups));
		when(user.getAuthorities()).thenReturn(List.of());
		when(user.getIdToken()).thenReturn(idToken);
		when(user.getUserInfo()).thenReturn(null);
		return user;
	}

	private java.net.URL issuerUrl() {
		try {
			return java.net.URI.create(ISSUER).toURL();
		} catch (java.net.MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
}
