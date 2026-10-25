ALTER TABLE reviews
    ADD COLUMN masked_content TEXT NULL COMMENT '마스킹된 후기 내용. NULL이면 마스킹되지 않은 상태';

ALTER TABLE reviews
    MODIFY COLUMN content TEXT NOT NULL COMMENT '후기 원문';
