package com.shop.auth.role;

import com.shop.auth.permission.Permission;
import com.shop.auth.permission.PermissionRepository;
import com.shop.auth.common.ConflictException;
import com.shop.auth.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Roles and Permissions", description = "Authorization metadata")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {
    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public RoleController(RoleRepository roles, PermissionRepository permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "List roles")
    public List<RoleDtos.Response> roles() { return roles.findAllByOrderByNameAsc().stream().map(RoleDtos.Response::from).toList(); }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @Operation(summary = "Create a role")
    public RoleDtos.Response create(@Valid @RequestBody RoleDtos.CreateRequest request) {
        String name = request.name().trim().toUpperCase(java.util.Locale.ROOT);
        if (roles.findByName(name).isPresent()) {
            throw new ConflictException("A role already exists with this name");
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        return RoleDtos.Response.from(roles.save(role));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "List permissions")
    public List<RoleDtos.PermissionResponse> permissions() { return permissions.findAllByOrderByCodeAsc().stream().map(RoleDtos.PermissionResponse::from).toList(); }

    @PutMapping("/roles/{id}/permissions")
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_UPDATE')")
    @Operation(summary = "Replace a role's permissions")
    public RoleDtos.Response update(@PathVariable UUID id, @Valid @RequestBody RoleDtos.UpdatePermissionsRequest request) {
        Role role = roles.findById(id).orElseThrow(() -> new NotFoundException("Role not found: " + id));
        if ("SUPER_ADMIN".equals(role.getName())) {
            throw new ConflictException("SUPER_ADMIN permissions are system-managed");
        }
        List<Permission> values = request.permissionIds() == null ? List.of() : request.permissionIds().stream()
                .map(permissionId -> permissions.findById(permissionId).orElseThrow(() -> new NotFoundException("Permission not found: " + permissionId)))
                .toList();
        role.getPermissions().clear();
        role.getPermissions().addAll(values);
        return RoleDtos.Response.from(role);
    }
}
