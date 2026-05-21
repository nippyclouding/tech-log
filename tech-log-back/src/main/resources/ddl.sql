
--DDL
CREATE TABLE "BOARDS" (
                          "board_id" BIGSERIAL,
                          "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          "title" VARCHAR(255) NOT NULL,
                          "content" TEXT NOT NULL,
                          "views" BIGINT DEFAULT 0 NOT NULL,

                          CONSTRAINT "PK_BOARDS" PRIMARY KEY ("board_id")
);

CREATE TABLE "COMMENTS" (
                            "comment_id" BIGSERIAL,
                            "board_id" BIGINT NOT NULL,
                            "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            "is_deleted" BOOLEAN DEFAULT FALSE NOT NULL,
                            "content" VARCHAR(500) NOT NULL,

                            "github_id" BIGINT NOT NULL,
                            "github_name" VARCHAR(100) NOT NULL,
                            "github_avatar_url" TEXT,

                            "access_ip" VARCHAR(45) NOT NULL,

                            CONSTRAINT "PK_COMMENTS" PRIMARY KEY ("comment_id"),
                            CONSTRAINT "FK_BOARDS_TO_COMMENTS"
                                FOREIGN KEY ("board_id")
                                    REFERENCES "BOARDS" ("board_id")
                                    ON DELETE CASCADE
);

CREATE TABLE "CATEGORIES" (
                              "category_id" BIGSERIAL,
                              "category_name" VARCHAR(100) NOT NULL,

                              CONSTRAINT "PK_CATEGORIES" PRIMARY KEY ("category_id"),
                              CONSTRAINT "UK_CATEGORIES_NAME" UNIQUE ("category_name")
);

CREATE TABLE "BOARD_CATEGORY" (
                                  "board_category_id" BIGSERIAL,
                                  "board_id" BIGINT NOT NULL,
                                  "category_id" BIGINT NOT NULL,

                                  CONSTRAINT "PK_BOARD_CATEGORY" PRIMARY KEY ("board_category_id"),
                                  CONSTRAINT "FK_BOARDS_TO_BOARD_CATEGORY"
                                      FOREIGN KEY ("board_id")
                                          REFERENCES "BOARDS" ("board_id")
                                          ON DELETE CASCADE,
                                  CONSTRAINT "FK_CATEGORIES_TO_BOARD_CATEGORY"
                                      FOREIGN KEY ("category_id")
                                          REFERENCES "CATEGORIES" ("category_id"),
                                  CONSTRAINT "UK_BOARD_CATEGORY" UNIQUE ("board_id", "category_id")
);

CREATE TABLE "IMAGES" (
                          "image_id" BIGSERIAL,
                          "board_id" BIGINT NOT NULL,
                          "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

                          "storage_type" VARCHAR(20) NOT NULL,
                          "file_key" TEXT NOT NULL,
                          "original_name" VARCHAR(255) NOT NULL,
                          "stored_name" VARCHAR(255) NOT NULL,
                          "content_type" VARCHAR(100) NOT NULL,
                          "file_size" BIGINT NOT NULL,
                          "image_order" INT DEFAULT 0 NOT NULL,
                          "is_thumbnail" BOOLEAN DEFAULT FALSE NOT NULL,

                          CONSTRAINT "PK_IMAGES" PRIMARY KEY ("image_id"),
                          CONSTRAINT "FK_BOARDS_TO_IMAGES"
                              FOREIGN KEY ("board_id")
                                  REFERENCES "BOARDS" ("board_id")
                                  ON DELETE CASCADE,
                          CONSTRAINT "CK_IMAGES_STORAGE_TYPE"
                              CHECK ("storage_type" IN ('LOCAL', 'S3'))
);

CREATE TABLE "ACCESS_LOGS" (
                               "access_log_id" BIGSERIAL,
                               "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               "access_ip" VARCHAR(45) NOT NULL,
                               "request_uri" VARCHAR(500) NOT NULL,
                               "method" VARCHAR(10) NOT NULL,
                               "status_code" INT NOT NULL,

                               CONSTRAINT "PK_ACCESS_LOGS" PRIMARY KEY ("access_log_id"),
                               CONSTRAINT "CK_ACCESS_LOGS_METHOD"
                                   CHECK ("method" IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD'))
);

CREATE TABLE "LOGIN_LOGS" (
                               "login_log_id" BIGSERIAL,
                               "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               "provider" VARCHAR(30) NOT NULL,
                               "login_id" VARCHAR(100) NOT NULL,
                               "access_ip" VARCHAR(45) NOT NULL,

                               CONSTRAINT "PK_LOGIN_LOGS" PRIMARY KEY ("login_log_id")
);
