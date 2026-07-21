package com.ember.service;

import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.repository.StaffRepository;
import com.ember.web.dto.StaffRequest;
import com.ember.web.dto.StaffResponse;
import com.ember.web.dto.StaffUpdateRequest;
import com.ember.web.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class StaffServiceTest {

    @Autowired
    private StaffRepository repo;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private StaffService service() {
        return new StaffService(repo, encoder);
    }

    private StaffResponse manager() {
        return service().create(new StaffRequest("boss", "Boss", StaffRole.MANAGER, null, "secret123"));
    }

    @Test
    void createHashesCredentialsAndFlagsThem() {
        StaffResponse r = service().create(new StaffRequest("amy", "Amy", StaffRole.CASHIER, "1234", null));
        assertThat(r.hasPin()).isTrue();
        assertThat(r.hasPassword()).isFalse();
        Staff saved = repo.findByUsername("amy").orElseThrow();
        assertThat(saved.getPinHash()).isNotEqualTo("1234");
        assertThat(encoder.matches("1234", saved.getPinHash())).isTrue();
    }

    @Test
    void rejectsDuplicateUsername() {
        service().create(new StaffRequest("amy", "Amy", StaffRole.CASHIER, "1234", null));
        assertThatThrownBy(() -> service().create(new StaffRequest("amy", "Other", StaffRole.COOK, "5678", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requiresAtLeastOneCredential() {
        assertThatThrownBy(() -> service().create(new StaffRequest("amy", "Amy", StaffRole.CASHIER, null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void managerCannotDemoteThemselves() {
        StaffResponse me = manager();
        assertThatThrownBy(() ->
                service().update(me.id(), new StaffUpdateRequest("Boss", StaffRole.CASHIER, true), "boss"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void managerCannotDeactivateThemselves() {
        StaffResponse me = manager();
        assertThatThrownBy(() ->
                service().update(me.id(), new StaffUpdateRequest("Boss", StaffRole.MANAGER, false), "boss"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void managerCannotDeleteThemselves() {
        StaffResponse me = manager();
        assertThatThrownBy(() -> service().delete(me.id(), "boss")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void canUpdateOtherStaff() {
        manager();
        StaffResponse amy = service().create(new StaffRequest("amy", "Amy", StaffRole.CASHIER, "1234", null));
        StaffResponse updated = service().update(amy.id(), new StaffUpdateRequest("Amy P.", StaffRole.COOK, false), "boss");
        assertThat(updated.role()).isEqualTo(StaffRole.COOK);
        assertThat(updated.active()).isFalse();
        assertThat(updated.displayName()).isEqualTo("Amy P.");
    }
}
