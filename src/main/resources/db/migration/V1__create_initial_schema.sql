CREATE TABLE universities (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '대학교 ID',
    name VARCHAR(100) NOT NULL COMMENT '대학교 이름',
    logo VARCHAR(2048) NOT NULL COMMENT '대학교 로고 URL',
    domain VARCHAR(50) NULL COMMENT '학교 이메일 도메인',
    CONSTRAINT pk_universities PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admins (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '관리자 ID',
    oidc_issuer VARCHAR(255) NOT NULL COMMENT 'Authentik OIDC 발급자',
    oidc_subject VARCHAR(255) NOT NULL COMMENT 'Authentik 관리자 고유 식별자',
    email VARCHAR(255) NULL COMMENT '관리자 이메일',
    username VARCHAR(100) NULL COMMENT '관리자 계정명',
    display_name VARCHAR(100) NULL COMMENT '관리자 표시 이름',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, DEACTIVATED',
    last_login_at DATETIME(6) NULL COMMENT '마지막 로그인 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    CONSTRAINT pk_admins PRIMARY KEY (id),
    CONSTRAINT uk_admins_oidc_identity UNIQUE (oidc_issuer, oidc_subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE campuses (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '캠퍼스 ID',
    university_id BIGINT NOT NULL COMMENT '대학교 ID',
    name VARCHAR(100) NOT NULL COMMENT '캠퍼스 이름',
    address VARCHAR(255) NOT NULL COMMENT '캠퍼스 주소',
    latitude DOUBLE NOT NULL COMMENT '캠퍼스 위도',
    longitude DOUBLE NOT NULL COMMENT '캠퍼스 경도',
    image VARCHAR(2048) NULL COMMENT '캠퍼스 이미지 URL',
    CONSTRAINT pk_campuses PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
    university_id BIGINT NOT NULL COMMENT '대학교 ID',
    provider VARCHAR(20) NOT NULL COMMENT 'KAKAO, GOOGLE, NAVER',
    provider_id VARCHAR(100) NOT NULL COMMENT '소셜 로그인 ID',
    student_number VARCHAR(50) NULL COMMENT '학번',
    university_email VARCHAR(255) NULL COMMENT '학교 이메일',
    admission_certificate VARCHAR(2048) NULL COMMENT '합격증명서 URL',
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT 'NEW_STUDENT_VERIFIED, EMAIL_VERIFIED, UNVERIFIED, PENDING',
    certificate_upload_date DATETIME(6) NULL COMMENT '증명서 업로드 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '가입 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    deleted_at DATETIME(6) NULL COMMENT '탈퇴 일시',
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_provider_identity UNIQUE (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agencies (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공인중개사 ID',
    agency_serial BIGINT NULL COMMENT '공인중개사 일련번호',
    name VARCHAR(255) NOT NULL COMMENT '공인중개사 이름',
    address VARCHAR(255) NOT NULL COMMENT '공인중개사 주소',
    latitude DOUBLE NOT NULL COMMENT '공인중개사 위도',
    longitude DOUBLE NOT NULL COMMENT '공인중개사 경도',
    rating DOUBLE NOT NULL DEFAULT 0.0 COMMENT '평균 평점',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    review_count INT NOT NULL DEFAULT 0 COMMENT '후기 수',
    image_count INT NOT NULL DEFAULT 0 COMMENT '이미지 수',
    CONSTRAINT pk_agencies PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE buildings (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '건물 ID',
    campus_id BIGINT NOT NULL COMMENT '캠퍼스 ID',
    name VARCHAR(255) NOT NULL COMMENT '건물명',
    type VARCHAR(30) NOT NULL COMMENT '건물 유형',
    address VARCHAR(255) NOT NULL COMMENT '도로명 주소',
    latitude DOUBLE NOT NULL COMMENT '위도',
    longitude DOUBLE NOT NULL COMMENT '경도',
    area DOUBLE NULL COMMENT '면적(㎡)',
    rating DOUBLE NOT NULL DEFAULT 0.0 COMMENT '평균 평점',
    review_count INT NOT NULL DEFAULT 0 COMMENT '후기 수',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    image_count INT NOT NULL DEFAULT 0 COMMENT '이미지 수',
    CONSTRAINT pk_buildings PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE facilities (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '편의시설 ID',
    name VARCHAR(100) NOT NULL COMMENT '편의시설 이름',
    CONSTRAINT pk_facilities PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE contents (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '찐빵 콘텐츠 ID',
    thumbnail_image VARCHAR(2048) NOT NULL COMMENT '대표 이미지 URL',
    category VARCHAR(255) NOT NULL COMMENT '부동산, 자취꿀팁, 대학생활, 이사관련',
    title VARCHAR(255) NOT NULL COMMENT '제목',
    content TEXT NOT NULL COMMENT '내용',
    share_count INT NOT NULL DEFAULT 0 COMMENT '공유 수',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    view_count INT NOT NULL DEFAULT 0 COMMENT '조회 수',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    deleted_at DATETIME(6) NULL COMMENT '삭제 일시',
    CONSTRAINT pk_contents PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '후기 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    building_id BIGINT NULL COMMENT '건물 ID',
    agency_id BIGINT NULL COMMENT '공인중개사 ID',
    status VARCHAR(20) NOT NULL COMMENT 'PUBLIC, PRIVATE',
    type VARCHAR(30) NOT NULL COMMENT 'GENERAL, DORM, AGENCY',
    rating INT NOT NULL DEFAULT 0 COMMENT '별점',
    thumbnail_image VARCHAR(2048) NULL COMMENT '썸네일 이미지 URL',
    content TEXT NOT NULL COMMENT '후기 내용',
    like_count INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    deleted_at DATETIME(6) NULL COMMENT '삭제 일시',
    CONSTRAINT pk_reviews PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE general_reviews (
    id BIGINT NOT NULL COMMENT '후기 ID',
    floor VARCHAR(255) NOT NULL COMMENT '층',
    area DOUBLE NOT NULL COMMENT '면적',
    contract_type VARCHAR(20) NOT NULL COMMENT '월세, 전세',
    deposit INT NULL COMMENT '보증금',
    price INT NULL COMMENT '월세 또는 전세 금액',
    maintenance_cost INT NULL COMMENT '관리비',
    CONSTRAINT pk_general_reviews PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dorm_reviews (
    id BIGINT NOT NULL COMMENT '후기 ID',
    floor VARCHAR(10) NOT NULL COMMENT 'LOW, MID, HIGH',
    capacity INT NOT NULL COMMENT '방 인원',
    dorm_fee INT NOT NULL COMMENT '기숙사비',
    current_region VARCHAR(255) NULL COMMENT '거주 지역',
    current_grade DOUBLE NULL COMMENT '현재 학점',
    CONSTRAINT pk_dorm_reviews PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agency_reviews (
    id BIGINT NOT NULL COMMENT '후기 ID',
    CONSTRAINT pk_agency_reviews PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE keywords (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '키워드 ID',
    keyword VARCHAR(100) NOT NULL COMMENT '키워드',
    CONSTRAINT pk_keywords PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admission_certificates (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '합격증명서 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    admin_id BIGINT NULL COMMENT '승인 또는 반려한 관리자 ID',
    url VARCHAR(2048) NOT NULL COMMENT '합격증명서 이미지 URL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVE, REJECT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '업로드 일시',
    CONSTRAINT pk_admission_certificates PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '리뷰 신고 ID',
    user_id BIGINT NULL COMMENT '신고 사용자 ID',
    review_id BIGINT NOT NULL COMMENT '신고 후기 ID',
    reason VARCHAR(50) NOT NULL COMMENT '신고 사유',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '신고 일시',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVE, REJECT',
    CONSTRAINT pk_reports PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE prohibited_words (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '금칙어 ID',
    admin_id BIGINT NULL COMMENT '금칙어 등록 관리자 ID',
    word VARCHAR(255) NOT NULL COMMENT '금칙어',
    is_enabled BIT(1) NOT NULL DEFAULT b'1' COMMENT '활성 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    deleted_at DATETIME(6) NULL COMMENT '삭제 일시',
    CONSTRAINT pk_prohibited_words PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE prohibited_word_flags (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '금칙어 플래그 ID',
    review_id BIGINT NOT NULL COMMENT '후기 ID',
    prohibited_words_id BIGINT NOT NULL COMMENT '금칙어 ID',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    CONSTRAINT pk_prohibited_word_flags PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE action_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '조치 이력 ID',
    prohibited_word_flag_id BIGINT NULL COMMENT '금칙어 플래그 ID',
    report_id BIGINT NULL COMMENT '리뷰 신고 ID',
    admin_id BIGINT NOT NULL COMMENT '실제 조치를 수행한 관리자 ID',
    reason VARCHAR(50) NOT NULL COMMENT '조치 사유',
    detail_reason TEXT NULL COMMENT '구체적인 조치 사유',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '조치 일시',
    CONSTRAINT pk_action_history PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE images (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
    review_id BIGINT NOT NULL COMMENT '후기 ID',
    url VARCHAR(2048) NOT NULL COMMENT '스토리지 URL',
    sort_order INT NOT NULL DEFAULT 1 COMMENT '정렬 순서',
    CONSTRAINT pk_images PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dormitory_facilities (
    id BIGINT NOT NULL COMMENT '기숙사 후기 ID',
    facility_id BIGINT NOT NULL COMMENT '편의시설 ID',
    available BIT(1) NOT NULL COMMENT '존재 여부',
    usage_type VARCHAR(20) NULL COMMENT 'PRIVATE, PUBLIC',
    CONSTRAINT pk_dormitory_facilities PRIMARY KEY (id, facility_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE review_keywords (
    id BIGINT NOT NULL COMMENT '후기 ID',
    id2 BIGINT NOT NULL COMMENT '키워드 ID',
    CONSTRAINT pk_review_keywords PRIMARY KEY (id, id2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE review_likes (
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    review_id BIGINT NOT NULL COMMENT '후기 ID',
    CONSTRAINT pk_review_likes PRIMARY KEY (user_id, review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE building_likes (
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    building_id BIGINT NOT NULL COMMENT '건물 ID',
    CONSTRAINT pk_building_likes PRIMARY KEY (user_id, building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agency_likes (
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    agency_id BIGINT NOT NULL COMMENT '공인중개사 ID',
    CONSTRAINT pk_agency_likes PRIMARY KEY (user_id, agency_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE contents_likes (
    contents_id BIGINT NOT NULL COMMENT '찐빵 콘텐츠 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    CONSTRAINT pk_contents_likes PRIMARY KEY (contents_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE campuses
    ADD CONSTRAINT fk_campuses_university FOREIGN KEY (university_id) REFERENCES universities (id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_university FOREIGN KEY (university_id) REFERENCES universities (id);

ALTER TABLE buildings
    ADD CONSTRAINT fk_buildings_campus FOREIGN KEY (campus_id) REFERENCES campuses (id);

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_reviews_building FOREIGN KEY (building_id) REFERENCES buildings (id),
    ADD CONSTRAINT fk_reviews_agency FOREIGN KEY (agency_id) REFERENCES agencies (id);

ALTER TABLE general_reviews
    ADD CONSTRAINT fk_general_reviews_review FOREIGN KEY (id) REFERENCES reviews (id);

ALTER TABLE dorm_reviews
    ADD CONSTRAINT fk_dorm_reviews_review FOREIGN KEY (id) REFERENCES reviews (id);

ALTER TABLE agency_reviews
    ADD CONSTRAINT fk_agency_reviews_review FOREIGN KEY (id) REFERENCES reviews (id);

ALTER TABLE admission_certificates
    ADD CONSTRAINT fk_admission_certificates_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_admission_certificates_admin FOREIGN KEY (admin_id) REFERENCES admins (id);

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_reports_review FOREIGN KEY (review_id) REFERENCES reviews (id);

ALTER TABLE prohibited_words
    ADD CONSTRAINT fk_prohibited_words_admin FOREIGN KEY (admin_id) REFERENCES admins (id);

ALTER TABLE prohibited_word_flags
    ADD CONSTRAINT fk_prohibited_word_flags_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    ADD CONSTRAINT fk_prohibited_word_flags_word FOREIGN KEY (prohibited_words_id) REFERENCES prohibited_words (id);

ALTER TABLE action_history
    ADD CONSTRAINT fk_action_history_word_flag FOREIGN KEY (prohibited_word_flag_id) REFERENCES prohibited_word_flags (id),
    ADD CONSTRAINT fk_action_history_report FOREIGN KEY (report_id) REFERENCES reports (id),
    ADD CONSTRAINT fk_action_history_admin FOREIGN KEY (admin_id) REFERENCES admins (id);

ALTER TABLE images
    ADD CONSTRAINT fk_images_review FOREIGN KEY (review_id) REFERENCES reviews (id);

ALTER TABLE dormitory_facilities
    ADD CONSTRAINT fk_dormitory_facilities_review FOREIGN KEY (id) REFERENCES dorm_reviews (id),
    ADD CONSTRAINT fk_dormitory_facilities_facility FOREIGN KEY (facility_id) REFERENCES facilities (id);

ALTER TABLE review_keywords
    ADD CONSTRAINT fk_review_keywords_review FOREIGN KEY (id) REFERENCES reviews (id),
    ADD CONSTRAINT fk_review_keywords_keyword FOREIGN KEY (id2) REFERENCES keywords (id);

ALTER TABLE review_likes
    ADD CONSTRAINT fk_review_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_review_likes_review FOREIGN KEY (review_id) REFERENCES reviews (id);

ALTER TABLE building_likes
    ADD CONSTRAINT fk_building_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_building_likes_building FOREIGN KEY (building_id) REFERENCES buildings (id);

ALTER TABLE agency_likes
    ADD CONSTRAINT fk_agency_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT fk_agency_likes_agency FOREIGN KEY (agency_id) REFERENCES agencies (id);

ALTER TABLE contents_likes
    ADD CONSTRAINT fk_contents_likes_content FOREIGN KEY (contents_id) REFERENCES contents (id),
    ADD CONSTRAINT fk_contents_likes_user FOREIGN KEY (user_id) REFERENCES users (id);
