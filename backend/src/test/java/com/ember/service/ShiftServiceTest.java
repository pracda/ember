package com.ember.service;

import com.ember.domain.Staff;
import com.ember.domain.StaffRole;
import com.ember.repository.ShiftRepository;
import com.ember.repository.StaffRepository;
import com.ember.web.dto.ShiftRequest;
import com.ember.web.error.BadRequestException;
import com.ember.web.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ShiftServiceTest {

    @Autowired
    private ShiftRepository shifts;
    @Autowired
    private StaffRepository staff;

    private ShiftService service() {
        return new ShiftService(shifts, staff);
    }

    private Long seedStaff() {
        return staff.saveAndFlush(new Staff("amy", "Amy", StaffRole.CASHIER)).getId();
    }

    @Test
    void createsAndListsAShiftWithStaffName() {
        Long id = seedStaff();
        LocalDate today = LocalDate.now();
        service().create(new ShiftRequest(id, today, LocalTime.of(9, 0), LocalTime.of(17, 0), "opening"));

        var list = service().list(today, today);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).staffName()).isEqualTo("Amy");
        assertThat(list.get(0).note()).isEqualTo("opening");
    }

    @Test
    void rejectsEndBeforeStart() {
        Long id = seedStaff();
        assertThatThrownBy(() -> service().create(
                new ShiftRequest(id, LocalDate.now(), LocalTime.of(17, 0), LocalTime.of(9, 0), null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsUnknownStaff() {
        assertThatThrownBy(() -> service().create(
                new ShiftRequest(999L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), null)))
                .isInstanceOf(NotFoundException.class);
    }
}
