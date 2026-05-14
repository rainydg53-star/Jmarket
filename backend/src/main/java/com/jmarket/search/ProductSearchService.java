package com.jmarket.search;

import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductListingType;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "jmarket.search.engine", havingValue = "elasticsearch")
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ElasticsearchOperations elasticsearchOperations;

    public ProductSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void index(Product product) {
        try {
            elasticsearchOperations.save(ProductSearchDocument.from(product));
        } catch (RuntimeException ex) {
            log.warn("Product Elasticsearch indexing failed. productId={}", product.getId(), ex);
        }
    }

    public void delete(Long productId) {
        try {
            elasticsearchOperations.delete(String.valueOf(productId), ProductSearchDocument.class);
        } catch (RuntimeException ex) {
            log.warn("Product Elasticsearch delete failed. productId={}", productId, ex);
        }
    }

    public List<Long> searchProductIds(ProductListingType listingType, String keyword, String category, String sort) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            String normalizedKeyword = keyword.trim();
            Criteria criteria = new Criteria("title").contains(normalizedKeyword)
                    .or(new Criteria("description").contains(normalizedKeyword))
                    .or(new Criteria("sellerNickname").contains(normalizedKeyword));
            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setPageable(PageRequest.of(0, 100));

            return elasticsearchOperations
                    .search(query, ProductSearchDocument.class)
                    .stream()
                    .map(SearchHit::getContent)
                    .filter(document -> listingType.name().equals(document.getListingType()))
                    .filter(document -> category == null || category.isBlank() || category.equals(document.getCategory()))
                    .sorted(comparator(sort))
                    .map(ProductSearchDocument::getId)
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Product Elasticsearch search failed. keyword={}", keyword, ex);
            return List.of();
        }
    }

    private Comparator<ProductSearchDocument> comparator(String sort) {
        String normalizedSort = sort == null ? "LATEST" : sort.trim().toUpperCase();
        return switch (normalizedSort) {
            case "PRICE_ASC" -> Comparator
                    .comparing(ProductSearchDocument::getPrice, Comparator.nullsLast(Long::compareTo))
                    .thenComparing(ProductSearchDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case "PRICE_DESC" -> Comparator
                    .comparing(ProductSearchDocument::getPrice, Comparator.nullsLast(Long::compareTo))
                    .reversed();
            case "POPULAR" -> Comparator
                    .comparing(ProductSearchDocument::getFavoriteCount, Comparator.nullsLast(Long::compareTo))
                    .thenComparing(ProductSearchDocument::getViewCount, Comparator.nullsLast(Long::compareTo))
                    .thenComparing(ProductSearchDocument::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
            default -> Comparator
                    .comparing(ProductSearchDocument::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
        };
    }
}
