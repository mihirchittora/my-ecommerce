package com.shop.auth.user;

import com.shop.auth.common.ConflictException;
import com.shop.auth.role.Role;

import java.util.Collection;
import java.util.Set;

final class SuperAdminRolePolicy {
    private SuperAdminRolePolicy() { }

    static void ensureAllApplicationRoles(Set<Role> assignedRoles, Collection<Role> applicationRoles) {
        if (assignedRoles.stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getName()))) {
            assignedRoles.addAll(applicationRoles);
        }
    }

    static void assertCanRemove(Set<Role> assignedRoles, Role role) {
        if (!"SUPER_ADMIN".equals(role.getName())
                && assignedRoles.stream().anyMatch(item -> "SUPER_ADMIN".equals(item.getName()))) {
            throw new ConflictException("SUPER_ADMIN users must retain all application roles");
        }
    }
}
