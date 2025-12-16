package com.taitrinh.online_auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.taitrinh.online_auction.dto.recaptcha.RecaptchaResponse;
import com.taitrinh.online_auction.exception.InvalidRecaptchaException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecaptchaService {

    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.verify-url}")
    private String verifyUrl;

    /**
     * Verify reCAPTCHA token with Google's API
     * 
     * @param token The reCAPTCHA token from frontend
     * @throws InvalidRecaptchaException if verification fails
     */
    public void verifyRecaptcha(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidRecaptchaException("Token reCAPTCHA không hợp lệ");
        }

        try {
            // Prepare request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", secretKey);
            requestBody.add("response", token);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Call Google's verification API
            RecaptchaResponse response = restTemplate.postForObject(
                    verifyUrl,
                    request,
                    RecaptchaResponse.class);

            // Check response
            if (response == null) {
                log.error("reCAPTCHA verification failed: No response from Google API");
                throw new InvalidRecaptchaException("Xác minh reCAPTCHA thất bại");
            }

            if (!response.isSuccess()) {
                log.warn("reCAPTCHA verification failed. Error codes: {}", response.getErrorCodes());
                throw new InvalidRecaptchaException("Xác minh reCAPTCHA thất bại");
            }

            log.info("reCAPTCHA verification successful");

        } catch (RestClientException e) {
            log.error("Error calling reCAPTCHA API: {}", e.getMessage());
            throw new InvalidRecaptchaException("Xác minh reCAPTCHA thất bại do lỗi mạng");
        }
    }
}
