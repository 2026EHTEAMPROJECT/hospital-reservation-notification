package com.hospital.notification.client;

import com.hospital.notification.dto.client.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    public UserResponse getUser(Long id) {

        return restTemplate.getForObject(
                "http://localhost:8081/api/users/" + id,
                UserResponse.class
        );
    }
}