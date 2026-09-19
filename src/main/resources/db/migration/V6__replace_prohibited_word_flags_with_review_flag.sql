ALTER TABLE reviews
    ADD COLUMN prohibited_word_flag BIT(1) NOT NULL DEFAULT b'0' COMMENT '금칙어 플래그';

ALTER TABLE action_history
    ADD COLUMN review_id BIGINT NULL COMMENT '조치 대상 후기 ID';

ALTER TABLE action_history
    DROP FOREIGN KEY fk_action_history_word_flag;

ALTER TABLE action_history
    ADD CONSTRAINT fk_action_history_review FOREIGN KEY (review_id) REFERENCES reviews (id);

ALTER TABLE action_history
    DROP COLUMN prohibited_word_flag_id;

ALTER TABLE prohibited_word_flags
    DROP FOREIGN KEY fk_prohibited_word_flags_review,
    DROP FOREIGN KEY fk_prohibited_word_flags_word;

DROP TABLE prohibited_word_flags;

ALTER TABLE action_history
    MODIFY COLUMN reason VARCHAR(255) NOT NULL COMMENT '조치 사유. 여러 사유는 쉼표로 이어붙여 저장';
