package com.jmarket.review.controller;

import com.jmarket.review.dto.ReviewCreateRequest;
import com.jmarket.review.dto.ReviewResponse;
import com.jmarket.review.dto.ReviewSummaryResponse;
import com.jmarket.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/reviews")
    public ReviewResponse create(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return reviewService.create(request, email);
    }

    @GetMapping("/api/users/{userId}/reviews")
    public List<ReviewResponse> getReviews(@PathVariable Long userId) {
        return reviewService.getReviews(userId);
    }

    @GetMapping("/api/users/{userId}/reviews/summary")
    public ReviewSummaryResponse getSummary(@PathVariable Long userId) {
        return reviewService.getSummary(userId);
    }
}
