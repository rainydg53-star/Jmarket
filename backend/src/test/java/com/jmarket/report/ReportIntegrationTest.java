package com.jmarket.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auction.repository.BidRepository;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.product.domain.Product;
import com.jmarket.product.repository.ProductRepository;
import com.jmarket.report.repository.ReportRepository;
import com.jmarket.support.repository.SupportInquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SupportInquiryRepository supportInquiryRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

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

        reportRepository.deleteAll();
        supportInquiryRepository.deleteAll();
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void reportCreateAndAdminResolveFlow() throws Exception {
        String userLoginId = "01091000001";
        String adminLoginId = "01091000009";
        String userToken = signUpAndGetToken(userLoginId, "reportuser");
        String adminToken = signUpAndGetToken(adminLoginId, "reportadmin");
        makeAdmin(adminLoginId);

        Long reporterId = jdbcTemplate.queryForObject(
                "select id from users where email = ?",
                Long.class,
                userLoginId + "@test.com"
        );
        Product product = productRepository.save(new Product(
                userRepository.findById(reporterId).orElseThrow(),
                "테스트상품",
                "신고대상상품",
                1000L
        ));

        String createBody = """
                {
                  "targetType":"PRODUCT",
                  "targetId":%d,
                  "reason":"사기 의심",
                  "detail":"사진과 설명이 달라 보입니다."
                }
                """.formatted(product.getId());

        String created = mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reportId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/reports/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId));

        String resolveBody = """
                {
                  "status":"RESOLVED",
                  "resolutionAction":"WARNING",
                  "resolutionMemo":"경고 조치 완료"
                }
                """;

        mockMvc.perform(patch("/api/admin/reports/" + reportId + "/resolve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionAction").value("WARNING"));
    }

    private String signUpAndGetToken(String loginId, String nickname) throws Exception {
        String email = loginId + "@test.com";
        String emailVerificationToken = emailVerificationToken(email);
        String body = """
                {
                  "email":"%s",
                  "name":"%s",
                  "phoneNumber":"%s",
                  "password":"password1234",
                  "passwordConfirm":"password1234",
                  "nickname":"%s",
                  "emailVerificationToken":"%s"
                }
                """.formatted(email, nickname, loginId, nickname, emailVerificationToken);

        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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

    private void makeAdmin(String loginId) {
        jdbcTemplate.update("update users set role = ? where email = ?", "ADMIN", loginId + "@test.com");
    }
}
