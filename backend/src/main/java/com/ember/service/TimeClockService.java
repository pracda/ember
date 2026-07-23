package com.ember.service;

import com.ember.domain.Staff;
import com.ember.domain.TimeEntry;
import com.ember.repository.StaffRepository;
import com.ember.repository.TimeEntryRepository;
import com.ember.web.dto.TimeEntryResponse;
import com.ember.web.error.BadRequestException;
import com.ember.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff time-clock: one open punch per member at a time. */
@Service
public class TimeClockService {

    private final TimeEntryRepository entries;
    private final StaffRepository staff;

    public TimeClockService(TimeEntryRepository entries, StaffRepository staff) {
        this.entries = entries;
        this.staff = staff;
    }

    @Transactional
    public TimeEntryResponse clockIn(String username) {
        Long staffId = staffId(username);
        entries.findByStaffIdAndClockOutIsNull(staffId).ifPresent(open -> {
            throw new BadRequestException("Already clocked in");
        });
        return toResponse(entries.save(new TimeEntry(staffId)));
    }

    @Transactional
    public TimeEntryResponse clockOut(String username) {
        TimeEntry open = entries.findByStaffIdAndClockOutIsNull(staffId(username))
                .orElseThrow(() -> new BadRequestException("Not clocked in"));
        open.clockOut();
        return toResponse(open);
    }

    /** The caller's open punch, or null if they aren't clocked in. */
    @Transactional(readOnly = true)
    public TimeEntryResponse current(String username) {
        return entries.findByStaffIdAndClockOutIsNull(staffId(username))
                .map(TimeClockService::toResponse)
                .orElse(null);
    }

    private Long staffId(String username) {
        return staff.findByUsername(username).map(Staff::getId)
                .orElseThrow(() -> new NotFoundException("Unknown staff: " + username));
    }

    private static TimeEntryResponse toResponse(TimeEntry e) {
        return new TimeEntryResponse(e.getId(), e.getStaffId(), e.getClockIn(), e.getClockOut());
    }
}
