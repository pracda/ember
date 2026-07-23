package com.ember.repository;

import com.ember.domain.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    /** The staff member's currently-open punch, if they are clocked in. */
    Optional<TimeEntry> findByStaffIdAndClockOutIsNull(Long staffId);

    /** Entries that started within a range — the labor report. */
    List<TimeEntry> findByClockInGreaterThanEqualAndClockInLessThan(Instant start, Instant end);
}
