package com.ember.service;

import com.ember.domain.Shift;
import com.ember.domain.Staff;
import com.ember.repository.ShiftRepository;
import com.ember.repository.StaffRepository;
import com.ember.web.dto.ShiftRequest;
import com.ember.web.dto.ShiftResponse;
import com.ember.web.error.BadRequestException;
import com.ember.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manager roster management. */
@Service
public class ShiftService {

    private final ShiftRepository shifts;
    private final StaffRepository staff;

    public ShiftService(ShiftRepository shifts, StaffRepository staff) {
        this.shifts = shifts;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> list(LocalDate from, LocalDate to) {
        Map<Long, String> names = staffNames();
        return shifts.findByWorkDateBetweenOrderByWorkDateAscStartTimeAsc(from, to).stream()
                .map(s -> toResponse(s, names))
                .toList();
    }

    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        validate(request);
        Shift shift = new Shift(request.staffId(), request.workDate(), request.startTime(), request.endTime());
        shift.setNote(request.note());
        return toResponse(shifts.save(shift), staffNames());
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        validate(request);
        Shift shift = shifts.findById(id).orElseThrow(() -> new NotFoundException("Shift not found: " + id));
        shift.setStaffId(request.staffId());
        shift.setWorkDate(request.workDate());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setNote(request.note());
        return toResponse(shifts.save(shift), staffNames());
    }

    @Transactional
    public void delete(Long id) {
        if (!shifts.existsById(id)) {
            throw new NotFoundException("Shift not found: " + id);
        }
        shifts.deleteById(id);
    }

    private void validate(ShiftRequest request) {
        if (!staff.existsById(request.staffId())) {
            throw new NotFoundException("Unknown staff: " + request.staffId());
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private Map<Long, String> staffNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        for (Staff s : staff.findAll()) {
            names.put(s.getId(), s.getDisplayName());
        }
        return names;
    }

    private static ShiftResponse toResponse(Shift s, Map<Long, String> names) {
        return new ShiftResponse(
                s.getId(), s.getStaffId(), names.getOrDefault(s.getStaffId(), "—"),
                s.getWorkDate(), s.getStartTime(), s.getEndTime(), s.getNote());
    }
}
