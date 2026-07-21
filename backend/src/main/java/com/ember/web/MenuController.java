package com.ember.web;

import com.ember.service.MenuService;
import com.ember.web.dto.MenuItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menu;

    public MenuController(MenuService menu) {
        this.menu = menu;
    }

    @GetMapping
    public List<MenuItemResponse> list() {
        return menu.list();
    }
}
