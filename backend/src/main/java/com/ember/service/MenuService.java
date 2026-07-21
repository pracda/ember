package com.ember.service;

import com.ember.repository.MenuItemRepository;
import com.ember.web.dto.MenuItemResponse;
import com.ember.web.dto.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository menu;

    public MenuService(MenuItemRepository menu) {
        this.menu = menu;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> list() {
        return menu.findAllByOrderByCategoryAscNameAsc().stream()
                .map(Mappers::toMenuItem)
                .toList();
    }
}
