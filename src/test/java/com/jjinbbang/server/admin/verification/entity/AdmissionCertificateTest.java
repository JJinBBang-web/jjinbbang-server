package com.jjinbbang.server.admin.verification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.admin.verification.type.AdmissionCertificateStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("합격증명서")
class AdmissionCertificateTest {

	@Test
	@DisplayName("승인한 관리자를 처리 이력에 기록한다")
	void approveRecordsAdmin() {
		AdmissionCertificate certificate = pendingCertificate();
		Admin admin = BeanUtils.instantiateClass(Admin.class);

		certificate.approve(admin);

		assertThat(certificate.getStatus()).isEqualTo(AdmissionCertificateStatus.APPROVE);
		assertThat(certificate.getAdmin()).isSameAs(admin);
		assertThat(certificate.getRejectReason()).isNull();
	}

	@Test
	@DisplayName("반려한 관리자와 반려 사유를 처리 이력에 기록한다")
	void rejectRecordsAdminAndReason() {
		AdmissionCertificate certificate = pendingCertificate();
		Admin admin = BeanUtils.instantiateClass(Admin.class);

		certificate.reject(admin, "식별 정보가 선명하지 않습니다.");

		assertThat(certificate.getStatus()).isEqualTo(AdmissionCertificateStatus.REJECT);
		assertThat(certificate.getAdmin()).isSameAs(admin);
		assertThat(certificate.getRejectReason()).isEqualTo("식별 정보가 선명하지 않습니다.");
	}

	private AdmissionCertificate pendingCertificate() {
		AdmissionCertificate certificate = BeanUtils.instantiateClass(AdmissionCertificate.class);
		ReflectionTestUtils.setField(certificate, "status", AdmissionCertificateStatus.PENDING);
		return certificate;
	}
}
