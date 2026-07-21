package com.ember.web;

import com.ember.security.JwtService;
import com.ember.service.MenuService;
import com.ember.web.dto.MenuItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Authorization (MANAGER-only) is verified in SecurityIntegrationTest; filters are
// disabled here to focus on the CRUD behaviour.
@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
class MenuControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private MenuService menu;

    @MockitoBean
    private JwtService jwtService;

    private static MenuItemResponse sample() {
        return new MenuItemResponse("x1", "Test Item", "Sides", new BigDecimal("3.00"), false, List.of(), List.of());
    }

    @Test
    void createReturns201() throws Exception {
        when(menu.create(any())).thenReturn(sample());
        String body = """
                {"id":"x1","name":"Test Item","category":"Sides","basePrice":3.00,"mealAvailable":false}""";

        mvc.perform(post("/api/menu").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("x1"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        String body = """
                {"id":"x1","name":"","category":"Sides","basePrice":3.00,"mealAvailable":false}""";

        mvc.perform(post("/api/menu").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturns200() throws Exception {
        when(menu.update(eq("x1"), any())).thenReturn(sample());
        String body = """
                {"id":"x1","name":"Renamed","category":"Sides","basePrice":4.00,"mealAvailable":false}""";

        mvc.perform(put("/api/menu/x1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/menu/x1"))
                .andExpect(status().isNoContent());
        verify(menu).delete("x1");
    }
}
