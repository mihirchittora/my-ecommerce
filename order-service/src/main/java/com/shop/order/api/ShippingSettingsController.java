package com.shop.order.api;

import com.shop.order.service.ShippingSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/shipping/settings")
@Tag(name = "Shipping administration")
@SecurityRequirement(name = "bearerAuth")
public class ShippingSettingsController {
    private final ShippingSettingsService settings;

    public ShippingSettingsController(ShippingSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SHIPPING_READ')")
    public ShippingSettingsDtos.Response get() {
        return settings.get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SHIPPING_MANAGE')")
    public ShippingSettingsDtos.Response update(@Valid @RequestBody ShippingSettingsDtos.UpdateRequest request) {
        return settings.update(request);
    }
}
