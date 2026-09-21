package com.shop.auth.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Privileged user and role assignment management")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {
    private final UserAdminService service;

    public UserAdminController(UserAdminService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create a user")
    public UserAdminDtos.Response create(@Valid @RequestBody UserAdminDtos.CreateRequest request) { return service.create(request); }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "List users")
    public Page<UserAdminDtos.Response> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "ALL") String scope) {
        return service.list(PageRequest.of(page, Math.min(size, 100), Sort.by("email").ascending()), scope);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get a user")
    public UserAdminDtos.Response get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update a user")
    public UserAdminDtos.Response update(@PathVariable UUID id, @Valid @RequestBody UserAdminDtos.UpdateRequest request) { return service.update(id, request); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Change user status")
    public UserAdminDtos.Response status(@PathVariable UUID id, @Valid @RequestBody UserAdminDtos.StatusRequest request) { return service.status(id, request); }

    @PostMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @Operation(summary = "Assign a role")
    public UserAdminDtos.Response addRole(@PathVariable UUID id, @PathVariable UUID roleId) { return service.addRole(id, roleId); }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @Operation(summary = "Assign a role")
    public UserAdminDtos.Response addRole(@PathVariable UUID id, @Valid @RequestBody UserAdminDtos.RoleAssignmentRequest request) {
        return service.addRole(id, request.roleId());
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @Operation(summary = "Remove a role")
    public UserAdminDtos.Response removeRole(@PathVariable UUID id, @PathVariable UUID roleId) { return service.removeRole(id, roleId); }
}
