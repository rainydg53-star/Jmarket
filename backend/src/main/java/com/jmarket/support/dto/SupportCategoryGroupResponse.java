package com.jmarket.support.dto;

import java.util.List;

public record SupportCategoryGroupResponse(
        String majorCategory,
        List<String> minorCategories
) {
}
