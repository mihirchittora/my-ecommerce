package com.shop.inventory.location;

import com.shop.inventory.adjustment.AdjustmentRepository;
import com.shop.inventory.common.ConflictException;
import com.shop.inventory.common.NotFoundException;
import com.shop.inventory.item.InventoryItemRepository;
import com.shop.inventory.item.InventoryItem;
import com.shop.inventory.movement.MovementRepository;
import com.shop.inventory.reservation.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class LocationService {
    private final LocationRepository locations;
    private final InventoryItemRepository items;
    private final ReservationRepository reservations;
    private final AdjustmentRepository adjustments;
    private final MovementRepository movements;

    public LocationService(LocationRepository locations,
                           InventoryItemRepository items,
                           ReservationRepository reservations,
                           AdjustmentRepository adjustments,
                           MovementRepository movements) {
        this.locations = locations;
        this.items = items;
        this.reservations = reservations;
        this.adjustments = adjustments;
        this.movements = movements;
    }

    public LocationDtos.Response create(LocationDtos.CreateRequest request) {
        String code = normalizeCode(request.code());
        if (locations.existsByCode(code)) throw new ConflictException("Location code already exists: " + code);
        InventoryLocation location = new InventoryLocation();
        location.setCode(code);
        location.setName(request.name().trim());
        location.setStatus(request.status() == null ? LocationStatus.ACTIVE : request.status());
        try {
            return LocationDtos.Response.from(locations.saveAndFlush(location));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Location code already exists: " + code);
        }
    }

    @Transactional(readOnly = true)
    public List<LocationDtos.Response> list() {
        return locations.findAllByOrderByCodeAsc().stream().map(LocationDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return locations.countByStatus(LocationStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public LocationDtos.Response get(UUID id) {
        return LocationDtos.Response.from(find(id));
    }

    public LocationDtos.Response update(UUID id, LocationDtos.UpdateRequest request) {
        InventoryLocation location = find(id);
        String code = normalizeCode(request.code());
        if (locations.existsByCodeAndIdNot(code, id)) {
            throw new ConflictException("Location code already exists: " + code);
        }
        location.setCode(code);
        location.setName(request.name().trim());
        if (request.status() != null) location.setStatus(request.status());
        try {
            return LocationDtos.Response.from(locations.saveAndFlush(location));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Location code already exists: " + code);
        }
    }

    public void delete(UUID id) {
        InventoryLocation location = find(id);
        if (items.existsByLocation_Id(id)
                || reservations.existsByLocation_Id(id)
                || adjustments.existsByLocation_Id(id)
                || movements.existsByFromLocation_Id(id)
                || movements.existsByToLocation_Id(id)) {
            throw new ConflictException("Location cannot be deleted while inventory history depends on it");
        }
        locations.delete(location);
    }

    public InventoryLocation find(UUID id) {
        return locations.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory location not found: " + id));
    }

    public InventoryLocation findLocationForReservation(String sku, long quantity) {
        return items.findBySkuOrderByLocation_Code(sku).stream()
                .filter(item -> item.getLocation().getStatus() == LocationStatus.ACTIVE)
                .filter(item -> item.available() >= quantity)
                .map(InventoryItem::getLocation)
                .findFirst()
                .orElseThrow(() -> new ConflictException("Insufficient available inventory"));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
