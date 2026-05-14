package com.jmarket.auction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuctionMileageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from reports");
        jdbcTemplate.update("delete from support_inquiries");
        jdbcTemplate.update("delete from bids");
        jdbcTemplate.update("delete from auctions");
        jdbcTemplate.update("delete from chat_messages");
        jdbcTemplate.update("delete from chat_rooms");
        jdbcTemplate.update("delete from mileage_ledger");
        jdbcTemplate.update("delete from mileage_accounts");
        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from trades");
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void outbidShouldReleasePreviousReservationAndCloseShouldSettleWinnerMileage() throws Exception {
        String sellerToken = signUpAndGetToken("seller@test.com", "판매자", "01092000001", "seller");
        String bidder1Token = signUpAndGetToken("bidder1@test.com", "입찰자1", "01092000002", "bidder1");
        String bidder2Token = signUpAndGetToken("bidder2@test.com", "입찰자2", "01092000003", "bidder2");

        chargeMileage(bidder1Token, 50000L);
        chargeMileage(bidder2Token, 50000L);

        Long auctionId = createAuction(sellerToken);

        placeBid(bidder1Token, auctionId, 1100L);
        assertBalanceAndReserved("bidder1@test.com", 50000L, 1100L);

        placeBidWithAutoAmount(bidder2Token, auctionId);
        assertBalanceAndReserved("bidder1@test.com", 50000L, 0L);
        assertBalanceAndReserved("bidder2@test.com", 50000L, 1210L);

        jdbcTemplate.update("update auctions set end_at = ? where id = ?", Instant.now().minusSeconds(1), auctionId);
        mockMvc.perform(patch("/api/auctions/" + auctionId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken))
                .andExpect(status().isOk());

        assertBalanceAndReserved("bidder2@test.com", 48790L, 0L);
        assertBalanceAndReserved("seller@test.com", 1210L, 0L);
    }

    @Test
    void concurrentSameAmountBidsShouldAllowOnlyOneWinnerBid() throws Exception {
        String sellerToken = signUpAndGetToken("seller2@test.com", "판매자2", "01092000011", "seller2");
        String bidder1Token = signUpAndGetToken("con-bidder1@test.com", "동시입찰1", "01092000012", "conbidder1");
        String bidder2Token = signUpAndGetToken("con-bidder2@test.com", "동시입찰2", "01092000013", "conbidder2");

        chargeMileage(bidder1Token, 50000L);
        chargeMileage(bidder2Token, 50000L);

        Long auctionId = createAuction(sellerToken);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        Future<Integer> bidder1Result = executor.submit(() -> submitBidAndGetStatus(bidder1Token, auctionId, 1100L, startSignal));
        Future<Integer> bidder2Result = executor.submit(() -> submitBidAndGetStatus(bidder2Token, auctionId, 1100L, startSignal));

        startSignal.countDown();

        Integer status1 = bidder1Result.get(10, TimeUnit.SECONDS);
        Integer status2 = bidder2Result.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        long successCount = (status1 == 200 ? 1 : 0) + (status2 == 200 ? 1 : 0);
        long failCount = (status1 == 400 ? 1 : 0) + (status2 == 400 ? 1 : 0);

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from bids where auction_id = ?", Long.class, auctionId))
                .isEqualTo(1L);

        assertSingleActiveReservationBetween("con-bidder1@test.com", "con-bidder2@test.com", 1100L);
    }

    private String signUpAndGetToken(String email, String name, String phone, String nickname) throws Exception {
        String emailVerificationToken = emailVerificationToken(email);
        String signUpBody = """
                {
                  "email":"%s",
                  "name":"%s",
                  "phoneNumber":"%s",
                  "password":"password1234",
                  "passwordConfirm":"password1234",
                  "nickname":"%s",
                  "emailVerificationToken":"%s"
                }
                """.formatted(email, name, phone, nickname, emailVerificationToken);

        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String emailVerificationToken(String email) throws Exception {
        String sendResponse = mockMvc.perform(post("/api/auth/email-verification/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String code = objectMapper.readTree(sendResponse).get("devCode").asText();

        String confirmResponse = mockMvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "code":"%s"
                                }
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(confirmResponse).get("emailVerificationToken").asText();
    }

    private void chargeMileage(String token, Long amount) throws Exception {
        String body = """
                {
                  "amount": %d
                }
                """.formatted(amount);

        mockMvc.perform(post("/api/mileage/charge")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private Long createAuction(String token) throws Exception {
        Instant startAt = Instant.now().minusSeconds(60);
        Instant endAt = Instant.now().plusSeconds(3600);
        String body = """
                {
                  "title": "경매상품",
                  "description": "경매 테스트 상품",
                  "startPrice": 1000,
                  "startAt": "%s",
                  "endAt": "%s"
                }
                """.formatted(startAt.toString(), endAt.toString());

        String response = mockMvc.perform(post("/api/auctions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void placeBid(String token, Long auctionId, Long amount) throws Exception {
        String body = """
                {
                  "amount": %d
                }
                """.formatted(amount);

        mockMvc.perform(post("/api/auctions/" + auctionId + "/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private void placeBidWithAutoAmount(String token, Long auctionId) throws Exception {
        mockMvc.perform(post("/api/auctions/" + auctionId + "/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":null}"))
                .andExpect(status().isOk());
    }

    private int submitBidAndGetStatus(
            String token,
            Long auctionId,
            Long amount,
            CountDownLatch startSignal
    ) throws Exception {
        startSignal.await(10, TimeUnit.SECONDS);
        String body = """
                {
                  "amount": %d
                }
                """.formatted(amount);

        return mockMvc.perform(post("/api/auctions/" + auctionId + "/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void assertSingleActiveReservationBetween(String email1, String email2, Long expectedReserved) {
        Long reserved1 = jdbcTemplate.queryForObject(
                """
                select ma.reserved_balance
                from mileage_accounts ma
                join users u on u.id = ma.user_id
                where u.email = ?
                """,
                Long.class,
                email1
        );
        Long reserved2 = jdbcTemplate.queryForObject(
                """
                select ma.reserved_balance
                from mileage_accounts ma
                join users u on u.id = ma.user_id
                where u.email = ?
                """,
                Long.class,
                email2
        );

        assertThat(reserved1 + reserved2).isEqualTo(expectedReserved);
        assertThat(reserved1.equals(expectedReserved) || reserved2.equals(expectedReserved)).isTrue();
    }

    private void assertBalanceAndReserved(String email, Long expectedBalance, Long expectedReserved) {
        JsonNode node = jdbcTemplate.queryForObject(
                """
                select ma.balance, ma.reserved_balance
                from mileage_accounts ma
                join users u on u.id = ma.user_id
                where u.email = ?
                """,
                (rs, rowNum) -> {
                    try {
                        return objectMapper.readTree("""
                                {"balance": %d, "reserved": %d}
                                """.formatted(rs.getLong("balance"), rs.getLong("reserved_balance")));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                },
                email
        );

        assertThat(node.get("balance").asLong()).isEqualTo(expectedBalance);
        assertThat(node.get("reserved").asLong()).isEqualTo(expectedReserved);
    }
}
