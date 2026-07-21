package com.ember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 6 auth acceptance: reads are public, mutations need the right staff role,
 * and bad credentials are rejected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    private static final String ORDER = """
            {"type":"DINE_IN","lines":[{"itemId":"b1","quantity":1}]}""";

    @Autowired
    private TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    private String login(String username, String password) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<Map> resp = rest.postForEntity(
                "/api/auth/login", new HttpEntity<>(body, headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resp.getBody().get("token");
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String path, String token) {
        var headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void menuIsPublic() {
        assertThat(rest.getForEntity("/api/menu", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createOrderRequiresAuthentication() {
        assertThat(post("/api/orders", ORDER, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createOrderForbiddenForCook() {
        String cook = login("cook", "cook123");
        assertThat(post("/api/orders", ORDER, cook).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cashierCanCreateOrder() {
        String cashier = login("cashier", "cashier123");
        assertThat(post("/api/orders", ORDER, cashier).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void menuWriteForbiddenForNonManager() {
        String cashier = login("cashier", "cashier123");
        // A cashier cannot touch menu admin (MANAGER only) — denied before reaching any handler.
        assertThat(post("/api/menu", "{}", cashier).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/menu", "{}", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reportsAreManagerOnly() {
        assertThat(get("/api/reports/day-summary", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/reports/day-summary", login("cashier", "cashier123")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/reports/day-summary", login("manager", "manager123")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void badCredentialsAreRejected() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>("{\"username\":\"cashier\",\"password\":\"wrong\"}", headers),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
