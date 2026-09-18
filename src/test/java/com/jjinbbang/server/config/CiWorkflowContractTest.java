package com.jjinbbang.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CiWorkflowContractTest {

	private static final Path WORKFLOW_PATH = Path.of(".github/workflows/ci.yml");
	private static final Pattern PINNED_ACTION = Pattern.compile(
		"\\s*- uses: [^\\s@]+@[0-9a-f]{40}(?:\\s+#.*)?");

	private static String workflow;

	@BeforeAll
	static void loadWorkflow() throws IOException {
		workflow = Files.readString(WORKFLOW_PATH);
	}

	@Test
	void developAndMainPushesValidateThenPublishImmutableArm64Images() {
		assertThat(workflow)
				.contains("push:\n    branches:\n      - develop\n      - main")
				.contains("run: ./gradlew clean test build --no-daemon")
				.contains("needs: test")
				.contains("platforms: linux/arm64")
				.contains("tags: ghcr.io/jjinbbang-web/jjinbbang-server:${{ github.sha }}");
	}

	@Test
	void publishedDigestIsValidatedAndInspectedForArm64() {
		assertThat(workflow)
				.contains("IMAGE_DIGEST: ${{ steps.build.outputs.digest }}")
				.contains("^sha256:[0-9a-f]{64}$")
				.contains("docker buildx imagetools inspect \"$IMAGE@$IMAGE_DIGEST\" --raw")
				.contains(".platform.os == \"linux\" and .platform.architecture == \"arm64\"");
	}

	@Test
	void repositoryDispatchRunsOnlyFromMainAndTargetsProd() {
		assertThat(workflow)
				.contains("dispatch:\n    if: github.event_name == 'push' && github.ref == 'refs/heads/main'")
				.contains("DEPLOY_ENVIRONMENT: prod")
				.doesNotContain("github.ref_name == 'main' && 'prod' || 'dev'");
	}

	@Test
	void missingGitOpsCredentialsSkipDispatchAndTransientFailuresRetry() {
		assertThat(workflow)
				.contains("GITOPS_APP_ID: ${{ secrets.GITOPS_APP_ID }}")
				.contains("GITOPS_APP_PRIVATE_KEY: ${{ secrets.GITOPS_APP_PRIVATE_KEY }}")
				.contains("GitOps dispatch skipped because GitHub App credentials are not configured")
				.contains("if: steps.gitops.outputs.available == 'true'")
				.contains("for attempt in 1 2 3; do")
				.contains("GitOps dispatch failed after 3 attempts");
	}

	@Test
	void actionsStayPinnedAndDispatchPayloadNamesStayStable() {
		List<String> actionUses = workflow.lines()
				.filter(line -> line.stripLeading().startsWith("- uses:"))
				.toList();

		assertThat(actionUses)
				.isNotEmpty()
				.allMatch(line -> PINNED_ACTION.matcher(line).matches());
		assertThat(workflow)
				.contains("event_type=admin-image-built")
				.contains("client_payload[component]")
				.contains("client_payload[environment]")
				.contains("client_payload[image]")
				.contains("client_payload[tag]")
				.contains("client_payload[source_repository]")
				.contains("client_payload[source_ref]")
				.contains("client_payload[source_sha]")
				.contains("client_payload[image_digest]");
	}
}
