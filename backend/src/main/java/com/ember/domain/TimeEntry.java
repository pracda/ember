package com.ember.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A time-clock punch: one open entry per staff member until they clock out. */
@Entity
@Table(name = "time_entry")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "clock_in", nullable = false)
    private Instant clockIn = Instant.now();

    @Column(name = "clock_out")
    private Instant clockOut;

    protected TimeEntry() { }

    public TimeEntry(Long staffId) {
        this.staffId = staffId;
    }

    public void clockOut() {
        this.clockOut = Instant.now();
    }

    public Long getId() { return id; }
    public Long getStaffId() { return staffId; }
    public Instant getClockIn() { return clockIn; }
    public Instant getClockOut() { return clockOut; }
}
