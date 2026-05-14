package com.jmarket.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.notification.dto.NotificationType;
import com.jmarket.notification.service.NotificationService;
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
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from notifications");
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
    void shouldListAndMarkNotificationsAsRead() throws Exception {
        String token = signUpAndGetToken("notify@test.com", "알림사용자", "01093000001", "notify");
        Long userId = findUserIdByEmail("notify@test.com");

        notificationService.create(
                userId,
                NotificationType.CHAT_MESSAGE,
                "새 메시지",
                "테스트 메시지 1",
                "/chat/rooms/1"
        );
        notificationService.create(
                userId,
                NotificationType.AUCTION_OUTBID,
                "상위 입찰",
                "테스트 메시지 2",
                "/auctions/1"
        );

        mockMvc.perform(get("/api/notifications/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/notifications/me/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        Long targetNotificationId = jdbcTemplate.queryForObject(
                "select id from notifications where recipient_user_id = ? order by id desc limit 1",
                Long.class,
                userId
        );

        mockMvc.perform(patch("/api/notifications/" + targetNotificationId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetNotificationId))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/notifications/me/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));

        mockMvc.perform(get("/api/notifications/me?unreadOnly=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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

    private Long findUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }
}
