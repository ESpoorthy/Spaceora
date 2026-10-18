package com.spaceora.reservation.controller;

import com.spaceora.availability.entity.AvailabilitySchedule;
import com.spaceora.availability.repository.AvailabilityScheduleRepository;
import com.spaceora.reservation.repository.ReservationRepository;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReservationConcurrencyIntegrationTest {

    private static final String TEST_PASSWORD = "SecurePass1!";
    private static final LocalDate BOOKING_DATE = LocalDate.of(2099, 6, 17);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AvailabilityScheduleRepository scheduleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String ownerEmail;
    private String customerEmail;
    private String customerToken;
    private Long spaceId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        ownerEmail = "race_owner_" + suffix + "@example.com";
        customerEmail = "race_customer_" + suffix + "@example.com";

        register(ownerEmail, "OWNER");
        register(customerEmail, "CUSTOMER");
        customerToken = login(customerEmail);

        HttpHeaders ownerHeaders = jsonHeaders(login(ownerEmail));
        Map<String, Object> space = Map.of(
                "name", "Concurrent booking test space",
                "description", "Temporary integration-test space",
                "type", "MEETING_ROOM",
                "city", "London",
                "address", "1 Test Street",
                "hourlyRate", 15,
                "capacity", 4
        );
        ResponseEntity<Map<String, Object>> spaceResponse = exchange(
                "/api/spaces", HttpMethod.POST, ownerHeaders, space);
        assertThat(spaceResponse.getStatusCode()).isEqualTo(CREATED);
        spaceId = ((Number) spaceResponse.getBody().get("id")).longValue();

        List<AvailabilitySchedule> schedules = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            schedules.add(AvailabilitySchedule.builder()
                    .spaceId(spaceId)
                    .dayOfWeek(day)
                    .openTime(LocalTime.MIDNIGHT)
                    .closeTime(LocalTime.of(23, 59))
                    .build());
        }
        scheduleRepository.saveAll(schedules);
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (spaceId != null) {
                reservationRepository.findBySpaceId(spaceId,
                        org.springframework.data.domain.PageRequest.of(0, 100))
                        .forEach(reservationRepository::delete);
                scheduleRepository.deleteBySpaceId(spaceId);
                spaceRepository.deleteById(spaceId);
            }
            if (ownerEmail != null) {
                userRepository.findByEmail(ownerEmail)
                        .ifPresent(user -> userRepository.deleteById(user.getId()));
            }
            if (customerEmail != null) {
                userRepository.findByEmail(customerEmail)
                        .ifPresent(user -> userRepository.deleteById(user.getId()));
            }
        });
    }

    @Test
    void concurrentOverlappingRequests_onlyOneReservationIsCreated() throws Exception {
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<Map<String, Object>>>> requests = new ArrayList<>();
        try {
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent request start timed out");
                    }
                    Map<String, Object> request = Map.of(
                            "spaceId", spaceId,
                            "startDatetime", BOOKING_DATE + "T10:00:00Z",
                            "endDatetime", BOOKING_DATE + "T11:00:00Z",
                            "notes", "Concurrent overlap regression test"
                    );
                    return exchange("/api/reservations", HttpMethod.POST,
                            jsonHeaders(customerToken), request);
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ResponseEntity<Map<String, Object>>> responses = new ArrayList<>();
            for (Future<ResponseEntity<Map<String, Object>>> request : requests) {
                responses.add(request.get(20, TimeUnit.SECONDS));
            }

            assertThat(responses).filteredOn(response -> response.getStatusCode().equals(CREATED)).hasSize(1);
            assertThat(responses).filteredOn(response -> response.getStatusCode().equals(CONFLICT))
                    .hasSize(requestCount - 1);
            assertThat(reservationRepository.findBySpaceId(spaceId,
                    org.springframework.data.domain.PageRequest.of(0, 100)).getContent())
                    .hasSize(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void register(String email, String role) {
        Map<String, Object> request = Map.of(
                "firstName", "Race",
                "lastName", "Test",
                "email", email,
                "password", TEST_PASSWORD,
                "role", role
        );
        ResponseEntity<Map<String, Object>> response = exchange(
                "/api/auth/register", HttpMethod.POST, jsonHeaders(null), request);
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
    }

    private String login(String email) {
        ResponseEntity<Map<String, Object>> response = exchange(
                "/api/auth/login", HttpMethod.POST, jsonHeaders(null),
                Map.of("email", email, "password", TEST_PASSWORD));
        assertThat(response.getStatusCode()).isEqualTo(OK);
        return (String) response.getBody().get("token");
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<Map<String, Object>> exchange(
            String path, HttpMethod method, HttpHeaders headers, Map<String, Object> body) {
        return restTemplate.exchange(
                URI.create("http://localhost:" + port + path),
                method,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {});
    }
}
