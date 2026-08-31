package com.jjinbbang.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class AdminEnvironmentConfigurationTest {

	private static final String OIDC_ISSUER_PROPERTY =
		"spring.security.oauth2.client.provider.authentik.issuer-uri";

	@Test
	void devAndProdProfilesExposeAuthentikIssuerAtOAuth2ProviderLevel() throws IOException {
		assertThat(property("application-dev.yml", OIDC_ISSUER_PROPERTY))
				.isEqualTo("${AUTHENTIK_ISSUER_URI}");
		assertThat(property("application-prod.yml", OIDC_ISSUER_PROPERTY))
				.isEqualTo("${AUTHENTIK_ISSUER_URI}");
	}

	private String property(String fileName, String key) throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader()
				.load(fileName, new ClassPathResource(fileName));
		return sources.stream()
				.map(source -> source.getProperty(key))
				.filter(value -> value != null)
				.map(Object::toString)
				.findFirst()
				.orElse(null);
	}
}
