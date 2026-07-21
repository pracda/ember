package com.ember.repository;

import com.ember.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Staff> findAllByOrderByDisplayNameAsc();

    /** The station sign-in roster: active members who have a PIN, ordered by name. */
    List<Staff> findByActiveTrueAndPinHashIsNotNullOrderByDisplayNameAsc();
}
