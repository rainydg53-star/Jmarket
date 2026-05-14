package com.jmarket.support.controller;

import com.jmarket.support.dto.SupportCategoryGroupResponse;
import com.jmarket.support.service.SupportInquiryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/categories")
public class SupportCategoryController {

    private final SupportInquiryService supportInquiryService;

    public SupportCategoryController(SupportInquiryService supportInquiryService) {
        this.supportInquiryService = supportInquiryService;
    }

    @GetMapping
    public List<SupportCategoryGroupResponse> getCategories() {
        return supportInquiryService.getCategories();
    }
}
