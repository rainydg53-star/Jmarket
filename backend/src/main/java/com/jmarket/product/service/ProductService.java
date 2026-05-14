package com.jmarket.product.service;

import com.jmarket.admin.domain.AdminCategory;
import com.jmarket.admin.domain.UserRestrictionType;
import com.jmarket.admin.repository.AdminCategoryRepository;
import com.jmarket.admin.service.UserRestrictionService;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductCategory;
import com.jmarket.product.domain.ProductFavorite;
import com.jmarket.product.domain.ProductImage;
import com.jmarket.product.domain.ProductQuestion;
import com.jmarket.product.domain.ProductView;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.dto.ProductCreateRequest;
import com.jmarket.product.dto.ProductDetailResponse;
import com.jmarket.product.dto.ProductFavoriteResponse;
import com.jmarket.product.dto.ProductImageRequest;
import com.jmarket.product.dto.ProductImageResponse;
import com.jmarket.product.dto.ProductImageUploadResponse;
import com.jmarket.product.dto.ProductQuestionAnswerRequest;
import com.jmarket.product.dto.ProductQuestionCreateRequest;
import com.jmarket.product.dto.ProductQuestionResponse;
import com.jmarket.product.dto.ProductSummaryResponse;
import com.jmarket.product.dto.ProductUpdateRequest;
import com.jmarket.product.repository.ProductFavoriteRepository;
import com.jmarket.product.repository.ProductImageRepository;
import com.jmarket.product.repository.ProductQuestionRepository;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.product.repository.ProductViewRepository;
import com.jmarket.review.repository.UserReviewRepository;
import com.jmarket.search.ProductSearchService;
import com.jmarket.trade.domain.TradeStatus;
import com.jmarket.trade.repository.TradeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    private static final Set<TradeStatus> ACTIVE_TRADE_STATUSES = EnumSet.of(TradeStatus.REQUESTED, TradeStatus.ACCEPTED);
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_UPLOAD_COUNT = 10;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final ProductViewRepository productViewRepository;
    private final AdminCategoryRepository categoryRepository;
    private final UserRestrictionService restrictionService;
    private final AuctionRepository auctionRepository;
    private final TradeRepository tradeRepository;
    private final UserReviewRepository userReviewRepository;
    private final ProductSearchService productSearchService;

    public ProductService(
            ProductRepository productRepository,
            UserRepository userRepository,
            ProductImageRepository productImageRepository,
            ProductFavoriteRepository productFavoriteRepository,
            ProductQuestionRepository productQuestionRepository,
            ProductViewRepository productViewRepository,
            AdminCategoryRepository categoryRepository,
            UserRestrictionService restrictionService,
            AuctionRepository auctionRepository,
            TradeRepository tradeRepository,
            UserReviewRepository userReviewRepository,
            ObjectProvider<ProductSearchService> productSearchServiceProvider
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productImageRepository = productImageRepository;
        this.productFavoriteRepository = productFavoriteRepository;
        this.productQuestionRepository = productQuestionRepository;
        this.productViewRepository = productViewRepository;
        this.categoryRepository = categoryRepository;
        this.restrictionService = restrictionService;
        this.auctionRepository = auctionRepository;
        this.tradeRepository = tradeRepository;
        this.userReviewRepository = userReviewRepository;
        this.productSearchService = productSearchServiceProvider.getIfAvailable();
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public ProductDetailResponse create(ProductCreateRequest request, String currentUserEmail) {
        User seller = findUserByEmail(currentUserEmail);
        restrictionService.validateAllowed(seller.getId(), UserRestrictionType.PRODUCT_CREATE);
        Product product = new Product(
                seller,
                request.title(),
                request.description(),
                resolveActiveCategoryCode(request.category()),
                request.price(),
                ProductListingType.DIRECT
        );
        Product savedProduct = productRepository.save(product);
        saveImages(savedProduct, request.images());
        indexProduct(savedProduct);
        return toDetailResponse(savedProduct, seller);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "productList",
            key = "T(java.util.Objects).toString(#keyword, '') + '|' + T(java.util.Objects).toString(#category, '') + '|' + T(java.util.Objects).toString(#sort, '')"
    )
    public List<ProductSummaryResponse> getAll(String keyword, String category, String sort) {
        String selectedCategory = category == null || category.isBlank() ? null : normalizeCategoryCode(category);

        List<Product> products = searchWithElasticsearch(keyword, selectedCategory, sort);
        if (products.isEmpty()) {
            products = productRepository.searchProducts(ProductListingType.DIRECT, keyword, selectedCategory, sort);
        }

        return products.stream()
                .map(product -> ProductSummaryResponse.from(
                        product,
                        getThumbnailUrl(product.getId()),
                        resolveCategoryLabel(product.getCategoryCode()),
                        resolveTradeStatus(product),
                        resolveTradeStatusLabel(product),
                        resolveSellerReviewCount(product),
                        resolveSellerAverageRating(product),
                        resolveSellerMannerTemperature(product)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getMyFavorites(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        return productFavoriteRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(ProductFavorite::getProduct)
                .filter(product -> product.getListingType() == ProductListingType.DIRECT)
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getMyRecentViews(String currentUserEmail) {
        findUserByEmail(currentUserEmail);
        return productViewRepository.findTop20ByViewerKeyOrderByViewedAtDesc("user:" + currentUserEmail).stream()
                .map(ProductView::getProduct)
                .filter(product -> product.getListingType() == ProductListingType.DIRECT)
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public ProductDetailResponse getById(Long productId, String currentUserEmail, String viewerKey) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        recordView(product, viewerKey);
        return toDetailResponse(product, currentUser);
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public ProductDetailResponse update(
            Long productId,
            ProductUpdateRequest request,
            String currentUserEmail
    ) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateSeller(product, currentUser);

        product.update(request.title(), request.description(), resolveActiveCategoryCode(request.category()), request.price());
        productImageRepository.deleteAllByProductId(product.getId());
        saveImages(product, request.images());
        indexProduct(product);
        return toDetailResponse(product, currentUser);
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public void delete(Long productId, String currentUserEmail) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateSeller(product, currentUser);
        validateDeletableProduct(productId);
        productImageRepository.deleteAllByProductId(productId);
        productQuestionRepository.deleteAllByProductId(productId);
        productFavoriteRepository.deleteAllByProductId(productId);
        productViewRepository.deleteAllByProductId(productId);
        productRepository.delete(product);
        deleteSearchIndex(productId);
    }

    private void validateDeletableProduct(Long productId) {
        if (tradeRepository.existsByProductId(productId) || auctionRepository.existsByProductId(productId)) {
            throw new JmarketException(ErrorCode.PRODUCT_DELETE_NOT_ALLOWED);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "productList", allEntries = true)
    public ProductFavoriteResponse toggleFavorite(Long productId, String currentUserEmail) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);

        return productFavoriteRepository.findByProductIdAndUserId(productId, currentUser.getId())
                .map(favorite -> {
                    productFavoriteRepository.delete(favorite);
                    product.decreaseFavoriteCount();
                    return new ProductFavoriteResponse(false, product.getFavoriteCount());
                })
                .orElseGet(() -> {
                    productFavoriteRepository.save(new ProductFavorite(product, currentUser));
                    product.increaseFavoriteCount();
                    return new ProductFavoriteResponse(true, product.getFavoriteCount());
                });
    }

    public List<ProductImageUploadResponse> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_IMAGE_UPLOAD_COUNT) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0\uB294 \uCD5C\uB300 10\uAC1C\uAE4C\uC9C0 \uC5C5\uB85C\uB4DC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
        }
        try {
            Path uploadDir = resolveUploadRoot().resolve("products").normalize();
            Files.createDirectories(uploadDir);
            List<ProductImageUploadResponse> responses = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                String extension = validateImageFile(file);
                String storedName = UUID.randomUUID() + "." + extension;
                Path target = uploadDir.resolve(storedName).normalize();
                if (!target.startsWith(uploadDir)) {
                    throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0 \uC800\uC7A5 \uACBD\uB85C\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
                }
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                responses.add(new ProductImageUploadResponse("/uploads/products/" + storedName));
            }
            return responses;
        } catch (JmarketException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
        }
    }

    private String validateImageFile(MultipartFile file) throws IOException {
        if (file.getSize() <= 0) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uBE48 \uC774\uBBF8\uC9C0\uB294 \uC5C5\uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0\uB294 \uD30C\uC77C\uB2F9 5MB \uC774\uD558\uB9CC \uC5C5\uB85C\uB4DC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "jpg, png, gif, webp \uC774\uBBF8\uC9C0\uB9CC \uC5C5\uB85C\uB4DC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String expectedExtension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (expectedExtension == null) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0 Content-Type\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
        }
        if (!extensionsMatch(extension, expectedExtension)) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0 \uD655\uC7A5\uC790\uC640 Content-Type\uC774 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
        }
        if (!hasValidImageSignature(file, contentType)) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "\uC774\uBBF8\uC9C0 \uD30C\uC77C \uD615\uC2DD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.");
        }
        return expectedExtension;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean extensionsMatch(String extension, String expectedExtension) {
        if ("jpg".equals(expectedExtension)) {
            return "jpg".equals(extension) || "jpeg".equals(extension);
        }
        return expectedExtension.equals(extension);
    }

    private boolean hasValidImageSignature(MultipartFile file, String contentType) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        }
        if (read < 4) {
            return false;
        }
        byte[] actual = Arrays.copyOf(header, read);
        return switch (contentType) {
            case "image/jpeg" -> actual.length >= 3
                    && (actual[0] & 0xFF) == 0xFF
                    && (actual[1] & 0xFF) == 0xD8
                    && (actual[2] & 0xFF) == 0xFF;
            case "image/png" -> actual.length >= 8
                    && (actual[0] & 0xFF) == 0x89
                    && actual[1] == 0x50
                    && actual[2] == 0x4E
                    && actual[3] == 0x47
                    && actual[4] == 0x0D
                    && actual[5] == 0x0A
                    && actual[6] == 0x1A
                    && actual[7] == 0x0A;
            case "image/gif" -> actual.length >= 6
                    && actual[0] == 0x47
                    && actual[1] == 0x49
                    && actual[2] == 0x46
                    && actual[3] == 0x38
                    && (actual[4] == 0x37 || actual[4] == 0x39)
                    && actual[5] == 0x61;
            case "image/webp" -> actual.length >= 12
                    && actual[0] == 0x52
                    && actual[1] == 0x49
                    && actual[2] == 0x46
                    && actual[3] == 0x46
                    && actual[8] == 0x57
                    && actual[9] == 0x45
                    && actual[10] == 0x42
                    && actual[11] == 0x50;
            default -> false;
        };
    }

    @Transactional(readOnly = true)
    public List<ProductQuestionResponse> getQuestions(Long productId, String currentUserEmail) {
        findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        return productQuestionRepository.findAllByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(question -> ProductQuestionResponse.from(question, currentUser))
                .toList();
    }

    @Transactional
    public ProductQuestionResponse createQuestion(
            Long productId,
            ProductQuestionCreateRequest request,
            String currentUserEmail
    ) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        ProductQuestion question = productQuestionRepository.save(new ProductQuestion(
                product,
                currentUser,
                request.question().trim(),
                request.secret()
        ));
        return ProductQuestionResponse.from(question, currentUser);
    }

    @Transactional
    public ProductQuestionResponse answerQuestion(
            Long productId,
            Long questionId,
            ProductQuestionAnswerRequest request,
            String currentUserEmail
    ) {
        Product product = findProductById(productId);
        User currentUser = findUserByEmail(currentUserEmail);
        validateSeller(product, currentUser);
        ProductQuestion question = productQuestionRepository.findById(questionId)
                .filter(item -> item.getProduct().getId().equals(productId))
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND));

        question.answer(request.answer().trim());
        return ProductQuestionResponse.from(question, currentUser);
    }

    private ProductDetailResponse toDetailResponse(Product product, User currentUser) {
        List<ProductImageResponse> images = productImageRepository.findAllByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(ProductImageResponse::from)
                .toList();
        boolean favorited = productFavoriteRepository.existsByProductIdAndUserId(product.getId(), currentUser.getId());
        return ProductDetailResponse.from(
                product,
                images,
                favorited,
                resolveCategoryLabel(product.getCategoryCode()),
                resolveTradeStatus(product),
                resolveTradeStatusLabel(product),
                resolveSellerReviewCount(product),
                resolveSellerAverageRating(product),
                resolveSellerMannerTemperature(product)
        );
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.from(
                product,
                getThumbnailUrl(product.getId()),
                resolveCategoryLabel(product.getCategoryCode()),
                resolveTradeStatus(product),
                resolveTradeStatusLabel(product),
                resolveSellerReviewCount(product),
                resolveSellerAverageRating(product),
                resolveSellerMannerTemperature(product)
        );
    }

    private void saveImages(Product product, List<ProductImageRequest> images) {
        List<ProductImageRequest> safeImages = images == null ? List.of() : images;
        if (safeImages.isEmpty()) {
            return;
        }

        boolean hasThumbnail = safeImages.stream().anyMatch(ProductImageRequest::thumbnail);
        boolean thumbnailAssigned = false;
        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < safeImages.size(); i++) {
            ProductImageRequest image = safeImages.get(i);
            boolean thumbnail = (image.thumbnail() && !thumbnailAssigned) || (!hasThumbnail && i == 0);
            if (thumbnail) {
                thumbnailAssigned = true;
            }
            productImages.add(new ProductImage(
                    product,
                    image.imageUrl().trim(),
                    thumbnail,
                    i
            ));
        }
        productImageRepository.saveAll(productImages);
    }

    private String getThumbnailUrl(Long productId) {
        return productImageRepository.findAllByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(ProductImage::isThumbnail)
                .findFirst()
                .or(() -> productImageRepository.findAllByProductIdOrderBySortOrderAsc(productId).stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private Comparator<Product> productComparator(String sort) {
        String normalizedSort = sort == null ? "LATEST" : sort.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedSort) {
            case "PRICE_ASC" -> Comparator.comparing(Product::getPrice)
                    .thenComparing(Product::getCreatedAt, Comparator.reverseOrder());
            case "PRICE_DESC" -> Comparator.comparing(Product::getPrice).reversed();
            case "POPULAR" -> Comparator.comparing(Product::getFavoriteCount)
                    .thenComparing(Product::getViewCount)
                    .thenComparing(Product::getCreatedAt)
                    .reversed();
            default -> Comparator.comparing(Product::getCreatedAt).reversed();
        };
    }

    private List<Product> searchWithElasticsearch(String keyword, String category, String sort) {
        if (productSearchService == null || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<Long> ids = productSearchService.searchProductIds(ProductListingType.DIRECT, keyword, category, sort);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllById(ids);
        return ids.stream()
                .flatMap(id -> products.stream().filter(product -> product.getId().equals(id)).limit(1))
                .toList();
    }

    private void indexProduct(Product product) {
        if (productSearchService != null) {
            productSearchService.index(product);
        }
    }

    private void deleteSearchIndex(Long productId) {
        if (productSearchService != null) {
            productSearchService.delete(productId);
        }
    }

    private void recordView(Product product, String viewerKey) {
        if (viewerKey == null || viewerKey.isBlank()) {
            return;
        }
        if (productViewRepository.existsByProductIdAndViewerKey(product.getId(), viewerKey)) {
            return;
        }
        productViewRepository.save(new ProductView(product, viewerKey));
        product.increaseViewCount();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }

    private Path resolveUploadRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (workingDirectory.getFileName() != null && "backend".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.resolve("uploads").normalize();
        }
        return workingDirectory.resolve(Path.of("backend", "uploads")).normalize();
    }

    private Product findProductById(Long productId) {
        return productRepository.findByIdAndListingType(productId, ProductListingType.DIRECT)
                .orElseThrow(() -> new JmarketException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private String resolveActiveCategoryCode(String rawCategory) {
        String code = normalizeCategoryCode(rawCategory);
        AdminCategory category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new JmarketException(ErrorCode.INVALID_INPUT));
        if (!category.isActive()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
        return category.getCode();
    }

    private String normalizeCategoryCode(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return ProductCategory.ETC.name();
        }
        String code = rawCategory.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        if (categoryRepository.existsByCode(code)) {
            return code;
        }
        return ProductCategory.from(rawCategory).name();
    }

    private String resolveCategoryLabel(String categoryCode) {
        String code = normalizeCategoryCode(categoryCode);
        return categoryRepository.findByCode(code)
                .map(AdminCategory::getName)
                .orElseGet(() -> ProductCategory.from(code).label());
    }

    private String resolveTradeStatus(Product product) {
        if (product.isSold()) {
            return "COMPLETED";
        }
        return tradeRepository.findFirstByProductIdAndStatusInOrderByRequestedAtDesc(product.getId(), ACTIVE_TRADE_STATUSES)
                .map(trade -> "RESERVED")
                .orElse("ON_SALE");
    }

    private String resolveTradeStatusLabel(Product product) {
        return switch (resolveTradeStatus(product)) {
            case "COMPLETED" -> "거래완료";
            case "RESERVED" -> "예약중";
            default -> "판매중";
        };
    }

    private long resolveSellerReviewCount(Product product) {
        return userReviewRepository.countByTargetUserId(product.getSeller().getId());
    }

    private double resolveSellerAverageRating(Product product) {
        Double average = userReviewRepository.averageRatingByTargetUserId(product.getSeller().getId());
        if (average == null) {
            return 0.0d;
        }
        return Math.round(average * 10.0d) / 10.0d;
    }

    private double resolveSellerMannerTemperature(Product product) {
        double average = resolveSellerAverageRating(product);
        double temperature = average == 0.0d ? 36.5d : 36.5d + ((average - 3.0d) * 3.0d);
        return Math.round(temperature * 10.0d) / 10.0d;
    }

    private void validateSeller(Product product, User currentUser) {
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new JmarketException(ErrorCode.FORBIDDEN);
        }
    }
}
