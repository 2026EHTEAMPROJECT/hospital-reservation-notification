package com.hospital.notification.client;

import com.hospital.notification.dto.client.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    /**
     * 요청자의 JWT 를 user-service /api/users/me 로 릴레이해 본인 User.id(=patientId)를 얻는다.
     * notification 은 Keycloak sub 만으로는 로컬 patientId 를 알 수 없어, 본인 검증(IDOR 방지)에 사용한다.
     */
    public Long getMyId(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                userServiceUrl + "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponse.class
        );
        UserResponse body = response.getBody();
        return body == null ? null : body.id();
    }
}