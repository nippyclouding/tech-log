package com.nippyclouding.tech_log_back.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "Invalid request."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "Access denied."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD_404", "Post was not found."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_404", "존재하지 않는 카테고리입니다."),
    CATEGORY_DUPLICATED(HttpStatus.CONFLICT, "CATEGORY_409", "이미 존재하는 카테고리입니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "CATEGORY_409_IN_USE", "게시글에서 사용 중인 카테고리는 삭제할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "Comment was not found."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "IMAGE_400", "Invalid image file."),
    IMAGE_TOTAL_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_413", "Images per post must not exceed 20MB."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "Internal server error.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
