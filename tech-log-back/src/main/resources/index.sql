-- INDEX
CREATE INDEX "IDX_IMAGES_BOARD_ID_IMAGE_ORDER"
    ON "IMAGES" ("board_id", "image_order");

CREATE INDEX "IDX_COMMENTS_BOARD_ID_IS_DELETED_UPDATED_AT"
    ON "COMMENTS" ("board_id", "is_deleted", "updated_at");

CREATE INDEX "IDX_BOARD_CATEGORY_CATEGORY_ID"
    ON "BOARD_CATEGORY" ("category_id");

CREATE INDEX "IDX_ACCESS_LOGS_UPDATED_AT"
    ON "ACCESS_LOGS" ("updated_at");

CREATE INDEX "IDX_LOGIN_LOGS_UPDATED_AT"
    ON "LOGIN_LOGS" ("updated_at");

CREATE INDEX "IDX_ADMINS_ACTIVE_USERNAME"
    ON "ADMINS" ("is_active", "username");

CREATE INDEX "IDX_BOARDS_UPDATED_AT"
    ON "BOARDS" ("updated_at" DESC);

-- '썸네일 이미지는 Board 1개당 하나' 비즈니스 로직을 DB 에서도 확인
CREATE UNIQUE INDEX "UK_IMAGES_ONE_THUMBNAIL_PER_BOARD"
    ON "IMAGES" ("board_id")
    WHERE "is_thumbnail" = true;
