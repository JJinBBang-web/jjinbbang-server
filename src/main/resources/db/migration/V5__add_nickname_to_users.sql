ALTER TABLE users
    ADD COLUMN nickname VARCHAR(20) NOT NULL DEFAULT '익명의 찐빵이'
        COMMENT '사용자 닉네임';
