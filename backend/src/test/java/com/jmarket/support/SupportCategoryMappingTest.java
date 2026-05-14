package com.jmarket.support;

import com.jmarket.support.domain.SupportMajorCategory;
import com.jmarket.support.domain.SupportMajorCategoryConverter;
import com.jmarket.support.domain.SupportMinorCategory;
import com.jmarket.support.domain.SupportMinorCategoryConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportCategoryMappingTest {

    @Test
    void shouldConvertMajorCategoryByLabelAndCode() {
        assertThat(SupportMajorCategory.fromValue("거래사고")).isEqualTo(SupportMajorCategory.TRADE_ACCIDENT);
        assertThat(SupportMajorCategory.fromValue("TRADE_ACCIDENT")).isEqualTo(SupportMajorCategory.TRADE_ACCIDENT);
        assertThat(new SupportMajorCategoryConverter().convertToDatabaseColumn(SupportMajorCategory.USAGE))
                .isEqualTo("이용관련");
    }

    @Test
    void shouldConvertMinorCategoryByLabelAndCode() {
        assertThat(SupportMinorCategory.fromValue("취소요청")).isEqualTo(SupportMinorCategory.CANCEL_REQUEST);
        assertThat(SupportMinorCategory.fromValue("CANCEL_REQUEST")).isEqualTo(SupportMinorCategory.CANCEL_REQUEST);
        assertThat(new SupportMinorCategoryConverter().convertToDatabaseColumn(SupportMinorCategory.LOGIN_ISSUE))
                .isEqualTo("로그인문의");
    }
}
