package com.ember;

import com.ember.repository.MenuItemRepository;
import com.ember.service.OrderService;
import com.ember.web.dto.CreateOrderRequest;
import com.ember.web.dto.OrderLineRequest;
import com.ember.web.dto.OrderResponse;
import com.ember.domain.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Flyway migration matches the JPA entities the way prod runs it:
 * against a real PostgreSQL, with {@code ddl-auto=validate}. If a column, type or
 * table drifts from {@code V1__init_schema.sql}, Hibernate's validator fails the
 * context and this test goes red. It also exercises the write path (orders +
 * order_line + order_line_addon inserts) against the migrated schema.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true) // skip (not fail) where docker-java can't reach a daemon
class FlywayMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Run exactly as prod: Flyway owns the schema, Hibernate only validates it.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MenuItemRepository menu;

    @Autowired
    private OrderService orders;

    @Test
    void migratedSchemaValidatesAndSeeds() {
        // Context loaded => Flyway migrated and Hibernate validated the mapping.
        assertThat(menu.count()).isEqualTo(18);
    }

    @Test
    void writePathPersistsAgainstMigratedSchema() {
        var request = new CreateOrderRequest(
                OrderType.DINE_IN,
                List.of(new OrderLineRequest(
                        "b1", 1, null, true, List.of("No onion", "Extra cheese"), "well done")));

        OrderResponse created = orders.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.ticketNumber()).isPositive();
        assertThat(created.lines().get(0).addons()).containsExactly("No onion", "Extra cheese");
        assertThat(created.subtotal()).isEqualByComparingTo("10.90"); // 6.50 + 3.50 meal + 0.90 cheese
        assertThat(created.total()).isEqualByComparingTo("11.83");    // 10.90 + 8.5% tax (0.93)
    }
}
