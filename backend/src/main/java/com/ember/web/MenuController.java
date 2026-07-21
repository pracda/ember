package com.ember.web;

import com.ember.service.MenuService;
import com.ember.web.dto.MenuItemRequest;
import com.ember.web.dto.MenuItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The menu. {@code GET} is public (the POS and stations read it); create/update/delete
 * are manager-only, enforced by {@code SecurityConfig}.
 */
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest request) {
        return menu.create(request);
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable String id, @Valid @RequestBody MenuItemRequest request) {
        return menu.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        menu.delete(id);
    }
}
