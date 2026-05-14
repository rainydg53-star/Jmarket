package com.jmarket.auth.controller;

import com.jmarket.auth.dto.UserProfileResponse;
import com.jmarket.auth.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return userProfileService.getProfile(userId);
    }
}
