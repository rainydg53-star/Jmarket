package com.jmarket.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cache.type=simple")
@AutoConfigureMockMvc
class ProductImageUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadImageShouldAcceptValidImageSignature() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "sample.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x00
                }
        );

        mockMvc.perform(multipart("/api/products/images")
                        .file(image)
                        .with(user("image-user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value(org.hamcrest.Matchers.endsWith(".png")));
    }

    @Test
    void uploadImageShouldRejectDisguisedFile() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "sample.png",
                "image/png",
                "not really an image".getBytes()
        );

        mockMvc.perform(multipart("/api/products/images")
                        .file(image)
                        .with(user("image-user@example.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImageShouldRejectUnsupportedExtension() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "sample.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/products/images")
                        .file(image)
                        .with(user("image-user@example.com")))
                .andExpect(status().isBadRequest());
    }
}
