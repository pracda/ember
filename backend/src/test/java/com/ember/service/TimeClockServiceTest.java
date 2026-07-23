package com.ember.service;

import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.repository.StaffRepository;
import com.ember.repository.TimeEntryRepository;
import com.ember.web.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TimeClockServiceTest {

    @Autowired
    private TimeEntryRepository entries;
    @Autowired
    private StaffRepository staff;

    private TimeClockService service() {
        return new TimeClockService(entries, staff);
    }

    @BeforeEach
    void seedStaff() {
        staff.saveAndFlush(new Staff("amy", "Amy", StaffRole.CASHIER));
    }

    @Test
    void clockInThenOut() {
        assertThat(service().clockIn("amy").clockOut()).isNull();
        assertThat(service().current("amy")).isNotNull();
        assertThat(service().clockOut("amy").clockOut()).isNotNull();
        assertThat(service().current("amy")).isNull();
    }

    @Test
    void cannotClockInTwice() {
        service().clockIn("amy");
        assertThatThrownBy(() -> service().clockIn("amy")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void cannotClockOutWhenNotClockedIn() {
        assertThatThrownBy(() -> service().clockOut("amy")).isInstanceOf(BadRequestException.class);
    }
}
