ALTER TABLE review_keywords
    RENAME COLUMN id TO review_id,
    RENAME COLUMN id2 TO keyword_id;

ALTER TABLE dormitory_facilities
    RENAME COLUMN id TO dorm_review_id;

ALTER TABLE prohibited_word_flags
    RENAME COLUMN prohibited_words_id TO prohibited_word_id;

ALTER TABLE contents_likes
    RENAME COLUMN contents_id TO content_id;
