package com.jmarket.support.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.support.domain.SupportInquiry;
import com.jmarket.support.domain.SupportInquiryStatus;
import com.jmarket.support.dto.SupportInquiryAnswerRequest;
import com.jmarket.support.dto.SupportCategoryGroupResponse;
import com.jmarket.support.dto.SupportInquiryCreateRequest;
import com.jmarket.support.dto.SupportInquiryDetailResponse;
import com.jmarket.support.dto.SupportInquiryStatusUpdateRequest;
import com.jmarket.support.dto.SupportInquirySummaryResponse;
import com.jmarket.support.repository.SupportInquiryRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportInquiryService {

    private final SupportInquiryRepository supportInquiryRepository;
    private final UserRepository userRepository;

    public SupportInquiryService(SupportInquiryRepository supportInquiryRepository, UserRepository userRepository) {
        this.supportInquiryRepository = supportInquiryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SupportInquiryDetailResponse create(SupportInquiryCreateRequest request, String currentUserEmail) {
        User member = findUserByEmail(currentUserEmail);
        validateCategory(request);

        SupportInquiry inquiry = new SupportInquiry(
                member,
                request.majorCategory(),
                request.minorCategory(),
                request.title().trim(),
                request.content().trim()
        );
        return SupportInquiryDetailResponse.from(supportInquiryRepository.save(inquiry));
    }

    @Transactional(readOnly = true)
    public List<SupportInquirySummaryResponse> getMyInquiries(String currentUserEmail) {
        User member = findUserByEmail(currentUserEmail);
        return supportInquiryRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .map(SupportInquirySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportInquirySummaryResponse> getAllForAdmin(String currentUserEmail) {
        User admin = findUserByEmail(currentUserEmail);
        validateAdmin(admin);

        return supportInquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SupportInquirySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportCategoryGroupResponse> getCategories() {
        return Stream.of(com.jmarket.support.domain.SupportMajorCategory.values())
                .map(major -> new SupportCategoryGroupResponse(
                        major.getLabel(),
                        Stream.of(com.jmarket.support.domain.SupportMinorCategory.values())
                                .filter(minor -> minor.getMajorCategory() == major)
                                .map(com.jmarket.support.domain.SupportMinorCategory::getLabel)
                                .toList()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportInquiryDetailResponse getById(Long inquiryId, String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        SupportInquiry inquiry = findInquiryById(inquiryId);

        if (!isAdmin(currentUser) && !inquiry.getMember().getId().equals(currentUser.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
        return SupportInquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse answerByAdmin(
            Long inquiryId,
            SupportInquiryAnswerRequest request,
            String currentUserEmail
    ) {
        User admin = findUserByEmail(currentUserEmail);
        validateAdmin(admin);
        SupportInquiry inquiry = findInquiryById(inquiryId);

        if (inquiry.getStatus() == SupportInquiryStatus.CLOSED) {
            throw new JmarketException(ErrorCode.SUPPORT_INVALID_STATUS);
        }

        inquiry.answer(admin, request.answerContent().trim());
        return SupportInquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse updateStatusByAdmin(
            Long inquiryId,
            SupportInquiryStatusUpdateRequest request,
            String currentUserEmail
    ) {
        User admin = findUserByEmail(currentUserEmail);
        validateAdmin(admin);
        SupportInquiry inquiry = findInquiryById(inquiryId);

        validateStatusChange(inquiry, request.status());
        inquiry.changeStatus(request.status());
        return SupportInquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse updateStatusByMember(
            Long inquiryId,
            SupportInquiryStatusUpdateRequest request,
            String currentUserEmail
    ) {
        User member = findUserByEmail(currentUserEmail);
        SupportInquiry inquiry = findInquiryById(inquiryId);

        if (!inquiry.getMember().getId().equals(member.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }

        if (request.status() != SupportInquiryStatus.CLOSED) {
            throw new JmarketException(ErrorCode.SUPPORT_INVALID_STATUS);
        }

        validateStatusChange(inquiry, request.status());
        inquiry.changeStatus(request.status());
        return SupportInquiryDetailResponse.from(inquiry);
    }

    private void validateCategory(SupportInquiryCreateRequest request) {
        if (request.minorCategory().getMajorCategory() != request.majorCategory()) {
            throw new JmarketException(ErrorCode.SUPPORT_INVALID_CATEGORY);
        }
    }

    private void validateStatusChange(SupportInquiry inquiry, SupportInquiryStatus targetStatus) {
        if (inquiry.getStatus() == SupportInquiryStatus.CLOSED && targetStatus != SupportInquiryStatus.CLOSED) {
            throw new JmarketException(ErrorCode.SUPPORT_INVALID_STATUS);
        }

        if (targetStatus == SupportInquiryStatus.ANSWERED
                && (inquiry.getAnswerContent() == null || inquiry.getAnswerContent().isBlank())) {
            throw new JmarketException(ErrorCode.SUPPORT_INVALID_STATUS);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private SupportInquiry findInquiryById(Long inquiryId) {
        return supportInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new JmarketException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
    }

    private void validateAdmin(User user) {
        if (!isAdmin(user)) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole().canAccessAdmin();
    }
}
