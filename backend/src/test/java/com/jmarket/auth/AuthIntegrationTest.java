package com.jmarket.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

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
    void signUpLoginAndMeFlow() throws Exception {
        String emailVerificationToken = emailVerificationToken("test@example.com");
        String signUpBody = """
                {
                  "email":"test@example.com",
                  "name":"테스트",
                  "phoneNumber":"01012341234",
                  "password":"password1234",
                  "passwordConfirm":"password1234",
                  "nickname":"tester",
                  "emailVerificationToken":"%s"
                }
                """.formatted(emailVerificationToken);

        String signUpResponse = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.loginId").value("test@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode signUpJson = objectMapper.readTree(signUpResponse);
        String signUpToken = signUpJson.get("accessToken").asText();
        assertThat(signUpToken).isNotBlank();

        String savedPasswordHash = userRepository.findByEmail("test@example.com")
                .orElseThrow()
                .getPasswordHash();
        assertThat(savedPasswordHash).isNotEqualTo("password1234");

        String loginBody = """
                {
                  "loginId":"test@example.com",
                  "password":"password1234"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.nickname").value("tester"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String loginToken = loginJson.get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("test@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("01012341234"))
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void changeMyPasswordShouldRequireCurrentPasswordAndStoreEncodedPassword() throws Exception {
        String token = signUpAndGetToken("change-password@example.com", "changeUser");

        mockMvc.perform(patch("/api/auth/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"wrong-password",
                                  "newPassword":"newPassword1234",
                                  "newPasswordConfirm":"newPassword1234"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/auth/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"password1234",
                                  "newPassword":"newPassword1234",
                                  "newPasswordConfirm":"newPassword1234"
                                }
                                """))
                .andExpect(status().isOk());

        String savedPasswordHash = userRepository.findByEmail("change-password@example.com")
                .orElseThrow()
                .getPasswordHash();
        assertThat(savedPasswordHash).isNotEqualTo("newPassword1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId":"change-password@example.com",
                                  "password":"password1234"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId":"change-password@example.com",
                                  "password":"newPassword1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void resetPasswordShouldRequireEmailVerificationToken() throws Exception {
        signUpAndGetToken("reset-password@example.com", "resetUser");

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"reset-password@example.com",
                                  "newPassword":"resetPassword1234",
                                  "newPasswordConfirm":"resetPassword1234",
                                  "emailVerificationToken":"invalid-token"
                                }
                                """))
                .andExpect(status().isBadRequest());

        String emailVerificationToken = passwordResetVerificationToken("reset-password@example.com");
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"reset-password@example.com",
                                  "newPassword":"resetPassword1234",
                                  "newPasswordConfirm":"resetPassword1234",
                                  "emailVerificationToken":"%s"
                                }
                                """.formatted(emailVerificationToken)))
                .andExpect(status().isOk());

        String savedPasswordHash = userRepository.findByEmail("reset-password@example.com")
                .orElseThrow()
                .getPasswordHash();
        assertThat(savedPasswordHash).isNotEqualTo("resetPassword1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId":"reset-password@example.com",
                                  "password":"resetPassword1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void meWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("S001"));
    }

    @Test
    void adminApiShouldRejectRegularUser() throws Exception {
        String token = signUpAndGetToken("user@example.com", "regularUser");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("S002"));
    }

    @Test
    void adminApiShouldAllowAdminUser() throws Exception {
        String token = signUpAndGetToken("admin@example.com", "adminUser");
        jdbcTemplate.update("update users set role = 'ADMIN' where email = ?", "admin@example.com");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String signUpAndGetToken(String email, String nickname) throws Exception {
        String emailVerificationToken = emailVerificationToken(email);
        String signUpBody = """
                {
                  "email":"%s",
                  "name":"AdminTest",
                  "phoneNumber":"01012341234",
                  "password":"password1234",
                  "passwordConfirm":"password1234",
                  "nickname":"%s",
                  "emailVerificationToken":"%s"
                }
                """.formatted(email, nickname, emailVerificationToken);

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

    private String passwordResetVerificationToken(String email) throws Exception {
        String sendResponse = mockMvc.perform(post("/api/auth/password/verify-email")
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

        String confirmResponse = mockMvc.perform(post("/api/auth/password/confirm-email")
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
}
