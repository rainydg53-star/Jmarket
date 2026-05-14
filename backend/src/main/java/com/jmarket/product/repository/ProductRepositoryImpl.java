package com.jmarket.product.repository;

import com.jmarket.product.domain.Product;
import com.jmarket.product.domain.ProductListingType;
import com.jmarket.product.domain.QProduct;
import com.jmarket.auth.domain.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ProductRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Product> searchProducts(ProductListingType listingType, String keyword, String category, String sort) {
        QProduct product = QProduct.product;
        QUser seller = QUser.user;
        BooleanBuilder where = new BooleanBuilder()
                .and(product.listingType.eq(listingType));

        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            where.and(product.title.containsIgnoreCase(normalizedKeyword)
                    .or(product.description.containsIgnoreCase(normalizedKeyword))
                    .or(seller.nickname.containsIgnoreCase(normalizedKeyword)));
        }
        if (category != null && !category.isBlank()) {
            where.and(product.category.eq(category));
        }

        return queryFactory
                .selectFrom(product)
                .join(product.seller, seller).fetchJoin()
                .where(where)
                .orderBy(orderBy(product, sort))
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(QProduct product, String sort) {
        String normalizedSort = sort == null ? "LATEST" : sort.trim().toUpperCase();
        return switch (normalizedSort) {
            case "PRICE_ASC" -> new OrderSpecifier<?>[] {product.price.asc(), product.createdAt.desc()};
            case "PRICE_DESC" -> new OrderSpecifier<?>[] {product.price.desc(), product.createdAt.desc()};
            case "POPULAR" -> new OrderSpecifier<?>[] {
                    product.favoriteCount.desc(),
                    product.viewCount.desc(),
                    product.createdAt.desc()
            };
            default -> new OrderSpecifier<?>[] {product.createdAt.desc()};
        };
    }
}
