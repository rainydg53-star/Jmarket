package com.jmarket.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "요청 값이 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A002", "이미 사용 중인 닉네임입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A003", "사용자를 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "이메일 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "A005", "소셜 로그인 처리에 실패했습니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "A006", "정지된 계정입니다."),
    USER_RESTRICTED(HttpStatus.FORBIDDEN, "A007", "현재 이용이 제한된 기능입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    PRODUCT_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "상품 문의를 찾을 수 없습니다."),
    PRODUCT_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "P003", "거래 또는 경매 이력이 있는 상품은 삭제할 수 없습니다."),
    TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "거래를 찾을 수 없습니다."),
    TRADE_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "T002", "이미 진행 중인 거래가 있습니다."),
    TRADE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "T003", "현재 거래 상태에서는 요청을 처리할 수 없습니다."),
    TRADE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T004", "본인 상품에는 거래 요청할 수 없습니다."),
    TRADE_INVALID_MILEAGE_REQUEST(HttpStatus.BAD_REQUEST, "T005", "마일리지 거래 요청 값이 올바르지 않습니다."),
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 진행 중인 경매가 있습니다."),
    AUCTION_INVALID_TIME(HttpStatus.BAD_REQUEST, "U003", "경매 시작/종료 시간이 올바르지 않습니다."),
    AUCTION_NOT_OPEN(HttpStatus.BAD_REQUEST, "U004", "진행 중인 경매가 아닙니다."),
    AUCTION_BID_TOO_LOW(HttpStatus.BAD_REQUEST, "U005", "현재 입찰 규칙보다 낮은 금액입니다."),
    AUCTION_SELF_BID_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U006", "본인 상품에는 입찰할 수 없습니다."),
    AUCTION_NOT_ENDED(HttpStatus.BAD_REQUEST, "U007", "경매 종료 시간이 지나야 마감할 수 있습니다."),
    AUCTION_ALREADY_TOP_BIDDER(HttpStatus.BAD_REQUEST, "U008", "이미 최고 입찰자입니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "채팅방을 찾을 수 없습니다."),
    CHAT_FORBIDDEN_PARTICIPANT(HttpStatus.FORBIDDEN, "H002", "채팅방 참여자가 아닙니다."),
    CHAT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "H003", "채팅 요청 값이 올바르지 않습니다."),
    MILEAGE_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "M001", "마일리지 금액이 올바르지 않습니다."),
    MILEAGE_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "M002", "사용 가능한 마일리지가 부족합니다."),
    MILEAGE_INSUFFICIENT_RESERVED(HttpStatus.BAD_REQUEST, "M003", "예약된 마일리지가 부족합니다."),
    MILEAGE_WITHDRAWAL_NOT_FOUND(HttpStatus.NOT_FOUND, "M004", "출금 요청을 찾을 수 없습니다."),
    MILEAGE_WITHDRAWAL_INVALID_STATUS(HttpStatus.BAD_REQUEST, "M005", "현재 출금 상태에서는 처리할 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Y001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Y002", "현재 결제 상태에서는 요청을 처리할 수 없습니다."),
    PAYMENT_PROVIDER_ERROR(HttpStatus.BAD_REQUEST, "Y003", "결제사 요청 처리 중 오류가 발생했습니다."),
    SUPPORT_INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "상담 문의를 찾을 수 없습니다."),
    SUPPORT_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "Q002", "상담 분류 값이 올바르지 않습니다."),
    SUPPORT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Q003", "현재 상담 상태에서는 요청을 처리할 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "신고를 찾을 수 없습니다."),
    REPORT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "R002", "신고 요청 값이 올바르지 않습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "R003", "신고 대상을 찾을 수 없습니다."),
    REPORT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "R004", "신고 상태 변경 요청이 올바르지 않습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "V001", "이미 리뷰를 작성했습니다."),
    REVIEW_INVALID_TARGET(HttpStatus.BAD_REQUEST, "V002", "리뷰 작성 대상이 올바르지 않습니다."),
    REVIEW_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "V003", "리뷰 대상 거래를 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),
    ADMIN_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "관리 카테고리를 찾을 수 없습니다."),
    ADMIN_CATEGORY_DUPLICATED(HttpStatus.CONFLICT, "D002", "이미 등록된 카테고리 코드입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "S001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "S002", "접근 권한이 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "S003", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S999", "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
