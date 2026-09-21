package com.shop.auth.role;

import com.shop.auth.permission.Permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class RoleDtos {
    private RoleDtos() { }
    public record PermissionResponse(UUID id, String code, String description) {
        public static PermissionResponse from(Permission permission) {
            return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
        }
    }
    public record Response(UUID id, String name, String description, List<PermissionResponse> permissions) {
        public static Response from(Role role) {
            return new Response(role.getId(), role.getName(), role.getDescription(),
                    role.getPermissions().stream().sorted(java.util.Comparator.comparing(Permission::getCode)).map(PermissionResponse::from).toList());
        }
    }
    public record CreateRequest(
            @NotBlank @Size(max = 80)
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "name must contain only letters, numbers, and underscores")
            String name,
            @Size(max = 500) String description) { }
    public record UpdatePermissionsRequest(List<UUID> permissionIds) { }
}
