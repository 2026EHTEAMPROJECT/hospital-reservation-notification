package com.hospital.notification.client;

import com.hospital.notification.dto.client.DoctorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class DoctorClient {

    private final RestTemplate restTemplate;

    public DoctorResponse getDoctor(Long id) {

        return restTemplate.getForObject(
                "http://localhost:8081/api/doctors/" + id,
                DoctorResponse.class
        );
    }
}