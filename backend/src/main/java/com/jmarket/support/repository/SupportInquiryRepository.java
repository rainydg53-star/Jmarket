package com.jmarket.support.repository;

import com.jmarket.support.domain.SupportInquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportInquiryRepository extends JpaRepository<SupportInquiry, Long> {

    List<SupportInquiry> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<SupportInquiry> findAllByOrderByCreatedAtDesc();
}
