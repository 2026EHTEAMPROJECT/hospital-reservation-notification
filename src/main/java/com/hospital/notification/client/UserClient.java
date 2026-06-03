package com.hospital.notification.client;

import com.hospital.notification.dto.client.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${clients.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    public UserResponse getUser(Long id) {

        return restTemplate.getForObject(
                userServiceUrl + "/api/users/" + id,
                UserResponse.class
        );
    }
}