package com.shop.inventory.service;

import com.shop.inventory.api.InventoryDtos;
import com.shop.inventory.catalog.CatalogSkuLookup;
import com.shop.inventory.common.BadRequestException;
import com.shop.inventory.common.ConflictException;
import com.shop.inventory.common.NotFoundException;
import com.shop.inventory.item.InventoryItem;
import com.shop.inventory.item.InventoryItemRepository;
import com.shop.inventory.location.InventoryLocation;
import com.shop.inventory.location.LocationService;
import com.shop.inventory.movement.InventoryUnitMovement;
import com.shop.inventory.movement.MovementRepository;
import com.shop.inventory.reservation.InventoryReservation;
import com.shop.inventory.reservation.ReservationRepository;
import com.shop.inventory.reservation.ReservationStatus;
import com.shop.inventory.reservation.ReservationUnit;
import com.shop.inventory.reservation.ReservationUnitRepository;
import com.shop.inventory.unit.InventoryUnit;
import com.shop.inventory.unit.InventoryUnitRepository;
import com.shop.inventory.unit.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservationService {
    private final CatalogSkuLookup catalog;
    private final LocationService locations;
    private final InventoryItemRepository items;
    private final InventoryUnitRepository units;
    private final ReservationRepository reservations;
    private final ReservationUnitRepository reservationUnits;
    private final MovementRepository movements;

    public ReservationService(CatalogSkuLookup catalog,
                              LocationService locations,
                              InventoryItemRepository items,
                              InventoryUnitRepository units,
                              ReservationRepository reservations,
                              ReservationUnitRepository reservationUnits,
                              MovementRepository movements) {
        this.catalog = catalog;
        this.locations = locations;
        this.items = items;
        this.units = units;
        this.reservations = reservations;
        this.reservationUnits = reservationUnits;
        this.movements = movements;
    }

    @Transactional
    public InventoryDtos.ReservationResponse reserve(String rawSku, InventoryDtos.ReservationRequest request) {
        String sku = normalizeSku(rawSku);
        catalog.requireActive(sku);
        InventoryLocation location = activeLocation(request.locationId());
        String referenceId = normalizeReference(request.referenceId());
        InventoryReservation existing = reservations.findByReferenceId(referenceId).orElse(null);
        if (existing != null) {
            assertSameReservation(existing, sku, location, request.quantity());
            return response(existing);
        }

        InventoryItem item = items.findForUpdate(sku, location.getId())
                .orElseThrow(() -> new ConflictException("Insufficient available inventory"));
        if (item.available() < request.quantity()) throw new ConflictException("Insufficient available inventory");
        List<InventoryUnit> selected = units.findAvailableForUpdate(sku, location.getId(),
                PageRequest.of(0, Math.toIntExact(request.quantity())));
        if (selected.size() != request.quantity()) throw new ConflictException("Insufficient available inventory");

        InventoryReservation reservation = new InventoryReservation();
        reservation.setSku(sku);
        reservation.setLocation(location);
        reservation.setQuantity(request.quantity());
        reservation.setReferenceId(referenceId);
        reservation.setExpiresAt(request.expiresAt());
        reservation.setStatus(ReservationStatus.ACTIVE);
        for (InventoryUnit unit : selected) {
            unit.setStatus(UnitStatus.RESERVED);
            recordMovement(unit, location, location, UnitStatus.AVAILABLE, UnitStatus.RESERVED,
                    "RESERVATION", referenceId, "Unit reserved");
            ReservationUnit link = new ReservationUnit();
            link.setReservation(reservation);
            link.setInventoryUnit(unit);
            reservation.getUnits().add(link);
        }
        item.setReservedQuantity(item.getReservedQuantity() + selected.size());
        reservations.saveAndFlush(reservation);
        return response(reservation);
    }

    @Transactional(readOnly = true)
    public InventoryDtos.ReservationResponse get(UUID id) {
        return response(reservations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<InventoryDtos.ReservationListResponse> list(ReservationStatus status, String rawSku,
                                                             UUID locationId, int page, int size, String sort) {
        String sku = normalizeOptionalSku(rawSku);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        return reservations.search(status, sku, locationId, pageable)
                .map(InventoryDtos.ReservationListResponse::from);
    }

    @Transactional
    public InventoryDtos.ReservationResponse release(UUID id) {
        InventoryReservation reservation = lock(id);
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            transitionReservedUnits(reservation, ReservationStatus.RELEASED, UnitStatus.AVAILABLE, false);
        } else if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new ConflictException("Confirmed reservation cannot be released");
        }
        return response(reservation);
    }

    @Transactional
    public InventoryDtos.ReservationResponse confirm(UUID id) {
        InventoryReservation reservation = lock(id);
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            transitionReservedUnits(reservation, ReservationStatus.CONFIRMED, UnitStatus.SOLD, true);
        } else if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("Only an ACTIVE reservation can be confirmed");
        }
        return response(reservation);
    }

    @Transactional
    public InventoryDtos.ReservationResponse cancel(UUID id) {
        InventoryReservation reservation = lock(id);
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            transitionReservedUnits(reservation, ReservationStatus.CANCELLED, UnitStatus.AVAILABLE, false);
        } else if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new ConflictException("Confirmed reservation cannot be cancelled");
        }
        return response(reservation);
    }

    @Transactional
    public void expireDueReservations() {
        Instant now = Instant.now();
        reservations.findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(ReservationStatus.ACTIVE, now)
                .forEach(reservation -> {
                    InventoryReservation locked = reservations.findByIdForUpdate(reservation.getId()).orElse(null);
                    if (locked != null && locked.getStatus() == ReservationStatus.ACTIVE
                            && locked.getExpiresAt() != null && !locked.getExpiresAt().isAfter(now)) {
                        transitionReservedUnits(locked, ReservationStatus.EXPIRED, UnitStatus.AVAILABLE, false);
                    }
                });
    }

    private void transitionReservedUnits(InventoryReservation reservation, ReservationStatus target,
                                         UnitStatus targetUnitStatus, boolean sold) {
        List<ReservationUnit> links = reservationUnits.findByReservation_IdOrderByInventoryUnit_UnitCode(reservation.getId());
        List<UUID> ids = links.stream().map(link -> link.getInventoryUnit().getId()).toList();
        if (ids.isEmpty()) throw new ConflictException("Reservation has no units");
        InventoryItem item = items.findForUpdate(reservation.getSku(), reservation.getLocation().getId())
                .orElseThrow(() -> new NotFoundException("Inventory item not found for reservation"));
        List<InventoryUnit> lockedUnits = units.findAllByIdInForUpdate(ids);
        for (InventoryUnit unit : lockedUnits) {
            if (sold && unit.getStatus() != UnitStatus.RESERVED) {
                throw new ConflictException("Reservation contains a unit that is no longer reserved");
            }
            if (!sold && reservation.getStatus() == ReservationStatus.ACTIVE
                    && unit.getStatus() != UnitStatus.RESERVED) {
                throw new ConflictException("Reservation contains a unit that is no longer reserved");
            }
            UnitStatus from = unit.getStatus();
            unit.setStatus(targetUnitStatus);
            if (sold) unit.setSoldAt(Instant.now());
            recordMovement(unit, reservation.getLocation(), reservation.getLocation(), from, targetUnitStatus,
                    "RESERVATION", reservation.getReferenceId(), target.name());
        }
        item.setReservedQuantity(item.getReservedQuantity() - reservation.getQuantity());
        if (sold) item.setQuantity(item.getQuantity() - reservation.getQuantity());
        reservation.setStatus(target);
    }

    private InventoryReservation lock(UUID id) {
        return reservations.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }

    private InventoryDtos.ReservationResponse response(InventoryReservation reservation) {
        List<InventoryUnit> units = reservationUnits.findByReservation_IdOrderByInventoryUnit_UnitCode(reservation.getId())
                .stream().map(ReservationUnit::getInventoryUnit).toList();
        return InventoryDtos.ReservationResponse.from(reservation, units);
    }

    private void assertSameReservation(InventoryReservation existing, String sku, InventoryLocation location, long quantity) {
        if (!existing.getSku().equals(sku)
                || !existing.getLocation().getId().equals(location.getId())
                || existing.getQuantity() != quantity) {
            throw new ConflictException("referenceId was already used for a different reservation");
        }
    }

    private void recordMovement(InventoryUnit unit, InventoryLocation from, InventoryLocation to,
                                UnitStatus fromStatus, UnitStatus toStatus,
                                String referenceType, String referenceId, String notes) {
        InventoryUnitMovement movement = new InventoryUnitMovement();
        movement.setInventoryUnit(unit);
        movement.setFromLocation(from);
        movement.setToLocation(to);
        movement.setFromStatus(fromStatus);
        movement.setToStatus(toStatus);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNotes(notes);
        movements.save(movement);
    }

    private InventoryLocation activeLocation(UUID id) {
        InventoryLocation location = locations.find(id);
        if (location.getStatus() != com.shop.inventory.location.LocationStatus.ACTIVE) {
            throw new ConflictException("Location is inactive: " + location.getCode());
        }
        return location;
    }

    private String normalizeSku(String sku) {
        String normalized = sku == null ? "" : sku.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new BadRequestException("SKU must not be blank");
        return normalized;
    }

    private String normalizeOptionalSku(String sku) {
        if (sku == null || sku.isBlank()) return null;
        String normalized = sku.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9][A-Z0-9._-]{0,79}$")) {
            throw new BadRequestException("Invalid SKU filter");
        }
        return normalized;
    }

    private Sort.Order[] parseSort(String sort) {
        if (sort == null || sort.isBlank()) return new Sort.Order[]{Sort.Order.desc("createdAt")};
        List<Sort.Order> orders = new ArrayList<>();
        String[] expressions = sort.split(",");
        for (int index = 0; index < expressions.length; index += 2) {
            String field = expressions[index].trim();
            if (!Set.of("createdAt", "updatedAt", "expiresAt", "quantity", "sku", "referenceId", "status").contains(field)) {
                throw new BadRequestException("Unsupported reservation sort field: " + field);
            }
            String direction = index + 1 < expressions.length ? expressions[index + 1].trim() : "asc";
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new BadRequestException("Sort direction must be asc or desc");
            }
            orders.add(new Sort.Order(Sort.Direction.fromString(direction), field));
        }
        return orders.toArray(Sort.Order[]::new);
    }

    private String normalizeReference(String reference) {
        if (reference == null || reference.isBlank()) throw new BadRequestException("referenceId must not be blank");
        return reference.trim();
    }
}
