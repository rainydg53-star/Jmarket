package com.jmarket.admin.controller;

import com.jmarket.admin.dto.AdminCategoryResponse;
import com.jmarket.admin.service.AdminService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final AdminService adminService;

    public CategoryController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminCategoryResponse> getActiveCategories() {
        return adminService.getActiveCategories();
    }
}
