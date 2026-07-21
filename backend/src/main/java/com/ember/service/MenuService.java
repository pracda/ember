package com.ember.service;

import com.ember.domain.MenuItem;
import com.ember.domain.PriceModifier;
import com.ember.repository.MenuItemRepository;
import com.ember.web.dto.MenuItemRequest;
import com.ember.web.dto.MenuItemResponse;
import com.ember.web.dto.Mappers;
import com.ember.web.dto.ModifierRequest;
import com.ember.web.error.BadRequestException;
import com.ember.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    /* ----- menu admin (MANAGER) ----- */

    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        if (menu.existsById(request.id())) {
            throw new BadRequestException("Menu item already exists: " + request.id());
        }
        MenuItem item = new MenuItem(
                request.id(), request.name(), request.category(),
                request.basePrice(), request.mealAvailable());
        applyModifiers(item, request);
        return Mappers.toMenuItem(menu.save(item));
    }

    @Transactional
    public MenuItemResponse update(String id, MenuItemRequest request) {
        MenuItem item = menu.findById(id)
                .orElseThrow(() -> new NotFoundException("Unknown menu item: " + id));
        item.setName(request.name());
        item.setCategory(request.category());
        item.setBasePrice(request.basePrice());
        item.setMealAvailable(request.mealAvailable());
        applyModifiers(item, request);
        return Mappers.toMenuItem(menu.save(item));
    }

    @Transactional
    public void delete(String id) {
        if (!menu.existsById(id)) {
            throw new NotFoundException("Unknown menu item: " + id);
        }
        menu.deleteById(id);
    }

    private static void applyModifiers(MenuItem item, MenuItemRequest request) {
        item.setSizes(toModifiers(request.sizes()));
        item.setAddons(toModifiers(request.addons()));
    }

    private static List<PriceModifier> toModifiers(List<ModifierRequest> mods) {
        List<PriceModifier> result = new ArrayList<>();
        for (ModifierRequest m : mods) {
            result.add(new PriceModifier(m.label(), m.priceDelta()));
        }
        return result;
    }
}
