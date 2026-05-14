package com.jmarket.product.controller;

import com.jmarket.product.dto.ProductCreateRequest;
import com.jmarket.product.dto.ProductDetailResponse;
import com.jmarket.product.dto.ProductFavoriteResponse;
import com.jmarket.product.dto.ProductImageUploadResponse;
import com.jmarket.product.dto.ProductQuestionAnswerRequest;
import com.jmarket.product.dto.ProductQuestionCreateRequest;
import com.jmarket.product.dto.ProductQuestionResponse;
import com.jmarket.product.dto.ProductSummaryResponse;
import com.jmarket.product.dto.ProductUpdateRequest;
import com.jmarket.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductDetailResponse create(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.create(request, email);
    }

    @PostMapping("/images")
    public List<ProductImageUploadResponse> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        return productService.uploadImages(files);
    }

    @GetMapping
    public List<ProductSummaryResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        return productService.getAll(keyword, category, sort);
    }

    @GetMapping("/me/favorites")
    public List<ProductSummaryResponse> getMyFavorites(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.getMyFavorites(email);
    }

    @GetMapping("/me/recent")
    public List<ProductSummaryResponse> getMyRecentViews(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.getMyRecentViews(email);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getById(
            @PathVariable Long productId,
            @AuthenticationPrincipal(expression = "username") String email,
            HttpServletRequest request
    ) {
        return productService.getById(productId, email, buildViewerKey(email, request));
    }

    @PutMapping("/{productId}")
    public ProductDetailResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.update(productId, request, email);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long productId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        productService.delete(productId, email);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/favorite")
    public ProductFavoriteResponse toggleFavorite(
            @PathVariable Long productId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.toggleFavorite(productId, email);
    }

    @GetMapping("/{productId}/questions")
    public List<ProductQuestionResponse> getQuestions(
            @PathVariable Long productId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.getQuestions(productId, email);
    }

    @PostMapping("/{productId}/questions")
    public ProductQuestionResponse createQuestion(
            @PathVariable Long productId,
            @Valid @RequestBody ProductQuestionCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.createQuestion(productId, request, email);
    }

    @PatchMapping("/{productId}/questions/{questionId}/answer")
    public ProductQuestionResponse answerQuestion(
            @PathVariable Long productId,
            @PathVariable Long questionId,
            @Valid @RequestBody ProductQuestionAnswerRequest request,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return productService.answerQuestion(productId, questionId, request, email);
    }

    private String buildViewerKey(String email, HttpServletRequest request) {
        if (email != null && !email.isBlank()) {
            return "user:" + email;
        }
        return "anon:" + request.getRemoteAddr() + ":" + request.getHeader("User-Agent");
    }
}
