package com.jmarket.auction.repository;

import com.jmarket.auction.domain.Auction;
import com.jmarket.auction.domain.AuctionStatus;
import com.jmarket.auction.domain.QAuction;
import com.jmarket.auth.domain.QUser;
import com.jmarket.product.domain.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AuctionRepositoryImpl implements AuctionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public AuctionRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Auction> searchVisibleAuctions(String keyword, String category, String sort, Instant closedCutoff) {
        QAuction auction = QAuction.auction;
        QProduct product = QProduct.product;
        QUser seller = QUser.user;
        BooleanBuilder visible = new BooleanBuilder()
                .and(auction.hidden.isFalse())
                .and(auction.status.eq(AuctionStatus.OPEN)
                        .or(auction.status.eq(AuctionStatus.CLOSED)
                                .and(auction.closedAt.goe(closedCutoff))));

        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            visible.and(product.title.containsIgnoreCase(normalizedKeyword)
                    .or(product.description.containsIgnoreCase(normalizedKeyword))
                    .or(seller.nickname.containsIgnoreCase(normalizedKeyword)));
        }
        if (category != null && !category.isBlank()) {
            visible.and(product.category.eq(category));
        }

        return queryFactory
                .selectFrom(auction)
                .join(auction.product, product).fetchJoin()
                .join(auction.seller, seller).fetchJoin()
                .where(visible)
                .orderBy(orderBy(auction, sort))
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(QAuction auction, String sort) {
        String normalizedSort = sort == null ? "ENDING_SOON" : sort.trim().toUpperCase();
        return switch (normalizedSort) {
            case "LATEST" -> new OrderSpecifier<?>[] {auction.createdAt.desc()};
            case "PRICE_ASC" -> new OrderSpecifier<?>[] {auction.startPrice.asc(), auction.endAt.asc()};
            case "PRICE_DESC" -> new OrderSpecifier<?>[] {auction.startPrice.desc(), auction.endAt.asc()};
            default -> new OrderSpecifier<?>[] {
                    auction.status.asc(),
                    auction.endAt.asc(),
                    auction.closedAt.desc()
            };
        };
    }
}
