package com.shop.shipping.shipment;

import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentItemEntity;
import com.shop.shipping.fulfillment.FulfillmentService;
import com.shop.shipping.api.ShippingDtos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemizedFulfillmentTest {
    private static final UUID ORDER_ITEM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U001 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID U002 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID U003 = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @Test
    void keepsAuthorizedPhysicalUnitsAndRejectsReplacementUnit() {
        FulfillmentEntity fulfillment = new FulfillmentEntity();
        fulfillment.setId(UUID.randomUUID());
        FulfillmentItemEntity item = new FulfillmentItemEntity();
        item.setOrderItemId(ORDER_ITEM);
        item.setSku("IP17-BLK-256");
        item.setProductNameSnapshot("Phone");
        item.setQuantity(2);
        item.setInventoryUnitIds(List.of(U001, U002));
        item.setInventoryUnitCodes(List.of("U001", "U002"));

        FulfillmentService fulfillments = new FulfillmentService(null, null, null, null, null) {
            @Override
            public List<FulfillmentItemEntity> itemEntities(UUID id) { return List.of(item); }
        };
        ShipmentService service = new ShipmentService(null, null, null, null, null, fulfillments,
                null, null, null, null, null);

        List<ShipmentService.PreparedItem> prepared = service.prepareItems(fulfillment,
                List.of(new ShippingDtos.ShipmentLineRequest(ORDER_ITEM, 2, List.of(U001, U002))));
        assertEquals(List.of(U001, U002), prepared.stream().map(ShipmentService.PreparedItem::inventoryUnitId).toList());
        assertThrows(RuntimeException.class, () -> service.prepareItems(fulfillment,
                List.of(new ShippingDtos.ShipmentLineRequest(ORDER_ITEM, 2, List.of(U001, U003)))));
    }
}
