ALTER TABLE admission_certificates
    ADD COLUMN reject_reason VARCHAR(255) NULL COMMENT '합격증명서 반려 사유',
    ADD CONSTRAINT chk_admission_certificates_reject_reason_status
        CHECK (reject_reason IS NULL OR status = 'REJECT');
