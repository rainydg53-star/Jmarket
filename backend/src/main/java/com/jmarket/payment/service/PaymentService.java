package com.jmarket.payment.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.mileage.service.MileageService;
import com.jmarket.payment.domain.Payment;
import com.jmarket.payment.domain.PaymentStatus;
import com.jmarket.payment.dto.PaymentApproveRequest;
import com.jmarket.payment.dto.PaymentReadyRequest;
import com.jmarket.payment.dto.PaymentReadyResponse;
import com.jmarket.payment.dto.PaymentResponse;
import com.jmarket.payment.dto.PaymentStatusUpdateRequest;
import com.jmarket.payment.repository.PaymentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentService {

    private static final String PROVIDER_KAKAOPAY = "KAKAOPAY";

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MileageService mileageService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakaopay.base-url:https://open-api.kakaopay.com}")
    private String kakaoPayBaseUrl;

    @Value("${kakaopay.secret-key:}")
    private String kakaoPaySecretKey;

    @Value("${kakaopay.cid:TC0ONETIME}")
    private String kakaoPayCid;

    @Value("${kakaopay.approval-url:http://localhost:5173/mileage}")
    private String approvalUrl;

    @Value("${kakaopay.cancel-url:http://localhost:5173/mileage}")
    private String cancelUrl;

    @Value("${kakaopay.fail-url:http://localhost:5173/mileage}")
    private String failUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            MileageService mileageService
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.mileageService = mileageService;
    }

    @Transactional
    public PaymentReadyResponse requestKakaoPay(PaymentReadyRequest request, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        String orderId = generateOrderId();
        Payment payment = paymentRepository.save(new Payment(user, PROVIDER_KAKAOPAY, orderId, request.amount()));

        Map<String, Object> readyRequest = Map.of(
                "cid", kakaoPayCid,
                "partner_order_id", orderId,
                "partner_user_id", String.valueOf(user.getId()),
                "item_name", "Jmarket Mileage Charge",
                "quantity", 1,
                "total_amount", request.amount(),
                "tax_free_amount", 0,
                "approval_url", approvalUrl + "?orderId=" + orderId,
                "cancel_url", cancelUrl + "?orderId=" + orderId,
                "fail_url", failUrl + "?orderId=" + orderId
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoPayBaseUrl + "/online/v1/payment/ready",
                    new HttpEntity<>(readyRequest, kakaoHeaders()),
                    Map.class
            );

            Map body = response.getBody();
            String tid = getRequiredString(body, "tid");
            String redirectUrl = getRequiredString(body, "next_redirect_pc_url");

            payment.markReady(tid, redirectUrl);
            return new PaymentReadyResponse(orderId, redirectUrl);
        } catch (Exception ex) {
            payment.markFailed("KAKAOPAY_READY_FAILED");
            throw new JmarketException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Transactional
    public PaymentResponse approveKakaoPay(PaymentApproveRequest request, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        Payment payment = findMyPaymentByOrderId(user, request.orderId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new JmarketException(ErrorCode.PAYMENT_INVALID_STATUS);
        }

        Map<String, Object> approveRequest = Map.of(
                "cid", kakaoPayCid,
                "tid", payment.getTid(),
                "partner_order_id", payment.getOrderId(),
                "partner_user_id", String.valueOf(user.getId()),
                "pg_token", request.pgToken()
        );

        try {
            restTemplate.postForEntity(
                    kakaoPayBaseUrl + "/online/v1/payment/approve",
                    new HttpEntity<>(approveRequest, kakaoHeaders()),
                    Map.class
            );

            payment.markApproved();
            mileageService.chargeBySystem(payment.getUser().getId(), payment.getAmount(), "PAYMENT_APPROVED", payment.getId());
            return PaymentResponse.from(payment);
        } catch (Exception ex) {
            payment.markFailed("KAKAOPAY_APPROVE_FAILED");
            throw new JmarketException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Transactional
    public PaymentResponse markCanceled(PaymentStatusUpdateRequest request, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        Payment payment = findMyPaymentByOrderId(user, request.orderId());
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new JmarketException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
        payment.markCanceled(request.reason());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse markFailed(PaymentStatusUpdateRequest request, String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        Payment payment = findMyPaymentByOrderId(user, request.orderId());
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new JmarketException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
        payment.markFailed(request.reason());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        return paymentRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private HttpHeaders kakaoHeaders() {
        if (kakaoPaySecretKey == null || kakaoPaySecretKey.isBlank()) {
            throw new JmarketException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);
        return headers;
    }

    private String getRequiredString(Map body, String key) {
        if (body == null || body.get(key) == null) {
            throw new JmarketException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
        return String.valueOf(body.get(key));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private Payment findMyPaymentByOrderId(User user, String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new JmarketException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
        return payment;
    }

    private String generateOrderId() {
        return "KM-" + UUID.randomUUID().toString().replace("-", "");
    }
}
