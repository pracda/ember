package com.ember.repository;

import com.ember.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {

    List<MenuItem> findAllByOrderByCategoryAscNameAsc();
}
