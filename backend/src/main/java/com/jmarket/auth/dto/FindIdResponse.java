package com.jmarket.auth.dto;

import java.util.List;

public record FindIdResponse(
        List<String> loginIds
) {
}
