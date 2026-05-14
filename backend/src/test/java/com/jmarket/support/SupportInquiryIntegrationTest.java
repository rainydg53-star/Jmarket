package com.jmarket.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmarket.auction.repository.AuctionRepository;
import com.jmarket.auction.repository.BidRepository;
import com.jmarket.auth.repository.UserRepository;
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
class SupportInquiryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportInquiryRepository supportInquiryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ProductRepository productRepository;

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
    void shouldExposeSupportCategoriesInKorean() throws Exception {
        String token = signUpAndGetToken("01090000001", "회원1");

        mockMvc.perform(get("/api/support/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].majorCategory").value("거래취소/종료"))
                .andExpect(jsonPath("$[0].minorCategories[0]").value("취소요청"))
                .andExpect(jsonPath("$[1].majorCategory").value("거래사고"))
                .andExpect(jsonPath("$[2].majorCategory").value("이용관련"));
    }

    @Test
    void shouldCreateAndReadInquiryUsingKoreanCategoryValues() throws Exception {
        String token = signUpAndGetToken("01090000002", "회원2");

        String createBody = """
                {
                  "majorCategory": "이용관련",
                  "minorCategory": "로그인문의",
                  "title": "로그인이 안됩니다",
                  "content": "앱에서 로그인 후 다시 로그인 페이지로 이동합니다."
                }
                """;

        String created = mockMvc.perform(post("/api/support/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.majorCategory").value("이용관련"))
                .andExpect(jsonPath("$.minorCategory").value("로그인문의"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdJson = objectMapper.readTree(created);
        Long inquiryId = createdJson.get("id").asLong();

        mockMvc.perform(get("/api/support/inquiries/" + inquiryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.majorCategory").value("이용관련"))
                .andExpect(jsonPath("$.minorCategory").value("로그인문의"))
                .andExpect(jsonPath("$.title").value("로그인이 안됩니다"));
    }

    @Test
    void shouldRejectMismatchedMajorMinorCategory() throws Exception {
        String token = signUpAndGetToken("01090000003", "회원3");

        String invalidBody = """
                {
                  "majorCategory": "거래사고",
                  "minorCategory": "로그인문의",
                  "title": "잘못된 조합",
                  "content": "분류 조합 검증"
                }
                """;

        mockMvc.perform(post("/api/support/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Q002"));
    }

    @Test
    void adminShouldAnswerAndCloseInquiry() throws Exception {
        String memberToken = signUpAndGetToken("01090000004", "회원4");
        String adminLoginId = "01090000009";
        String adminToken = signUpAndGetToken(adminLoginId, "관리자1");
        makeAdmin(adminLoginId);

        String createBody = """
                {
                  "majorCategory": "거래취소/종료",
                  "minorCategory": "취소요청",
                  "title": "거래 취소 요청",
                  "content": "취소 절차 확인 부탁드립니다."
                }
                """;

        String created = mockMvc.perform(post("/api/support/inquiries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long inquiryId = objectMapper.readTree(created).get("id").asLong();

        String answerBody = """
                {
                  "answerContent": "취소 요청 접수 완료되었습니다."
                }
                """;

        mockMvc.perform(patch("/api/admin/support/inquiries/" + inquiryId + "/answer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answerContent").value("취소 요청 접수 완료되었습니다."));

        String closeBody = """
                {
                  "status": "CLOSED"
                }
                """;

        mockMvc.perform(patch("/api/admin/support/inquiries/" + inquiryId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
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
