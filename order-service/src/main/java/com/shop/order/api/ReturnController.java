package com.shop.order.api;
import com.shop.order.returns.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Locale;

@RestController @SecurityRequirement(name = "bearerAuth")
public class ReturnController {
    private final ReturnService returns;
    public ReturnController(ReturnService returns) { this.returns = returns; }
    @PostMapping("/api/v1/returns") public ReturnDtos.Response create(@Valid @RequestBody ReturnDtos.CreateRequest request, Authentication authentication) { return returns.create(request, authentication); }
    @GetMapping("/api/v1/returns/my") public Page<ReturnDtos.Response> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) { return returns.mine(authentication, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"))); }
    @GetMapping("/api/v1/returns/{id}") public ReturnDtos.Response get(@PathVariable UUID id, Authentication authentication) { return returns.get(id, authentication, false); }
    @GetMapping("/api/v1/admin/returns") @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('RETURN_READ')") public Page<ReturnDtos.Response> adminList(@RequestParam(required = false) ReturnStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return returns.adminList(status, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"))); }
    @GetMapping("/api/v1/admin/returns/{id}") @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('RETURN_READ')") public ReturnDtos.Response adminGet(@PathVariable UUID id, Authentication authentication) { return returns.get(id, authentication, true); }
    @PostMapping("/api/v1/admin/returns/{id}/{action}") @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('RETURN_MANAGE')") public ReturnDtos.Response transition(@PathVariable UUID id, @PathVariable String action) { return returns.transition(id, ReturnStatus.valueOf(action.trim().toUpperCase(Locale.ROOT))); }
}
