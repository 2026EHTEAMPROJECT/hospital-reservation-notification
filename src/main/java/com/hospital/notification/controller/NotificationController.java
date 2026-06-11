package com.hospital.notification.controller;

import com.hospital.notification.client.UserClient;
import com.hospital.notification.dto.NotificationRecord;
import com.hospital.notification.service.LiveNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final LiveNotificationService liveNotificationService;
    private final UserClient userClient;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String patientId, @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(patientId, jwt);
        return liveNotificationService.subscribe(patientId);
    }

    @GetMapping
    public ResponseEntity<List<NotificationRecord>> getRecent(@RequestParam String patientId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(patientId, jwt);
        return ResponseEntity.ok(liveNotificationService.getRecentNotifications(patientId));
    }

    /**
     * IDOR 방지: 요청한 patientId 가 토큰 소유자 본인의 것인지 확인한다.
     * notification 은 Keycloak sub 만으로 로컬 patientId(user-service User.id)를 알 수 없으므로,
     * 요청자의 토큰을 user-service /me 로 릴레이해 본인 id 를 얻어 비교한다.
     * ADMIN 역할은 전체 접근 허용(관리자 신뢰).
     */
    private void verifyOwnership(String patientId, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (hasAdminRole(jwt)) {
            return;
        }
        Long callerId = userClient.getMyId(jwt.getTokenValue());
        if (callerId == null || !String.valueOf(callerId).equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 알림만 접근할 수 있습니다.");
        }
    }

    private boolean hasAdminRole(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> ra && ra.get("roles") instanceof Collection<?> roles) {
            return roles.contains("ADMIN");
        }
        return false;
    }
}
