package com.routex.service;
import com.routex.enums.OrderStatus;
import com.routex.enums.Priority;
import com.routex.model.InventoryItem;
import com.routex.model.ShipmentOrder;
import com.routex.dal.AuditLogDAO;
import com.routex.dal.InventoryItemDAO;
import com.routex.dal.ShipmentOrderDAO;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic service for:
 *  - UC04 — Generate Shipment Order (automatic + manual)
 *  - UC05 — Approve / Reject Shipment Order
 *
 * Business Rules:
 *  - Automatic generation: triggered when stock ≤ reorder threshold.
 *  - Manual generation: any quantity, destination, and priority.
 *  - Approval: only PENDING_APPROVAL orders may be approved; quantity may be adjusted.
 *  - Rejection: requires a non-empty reason; draft is updated and re-queued.
 *
 * Design Patterns:
 *  - GRASP Controller: this service is the system operation handler for UC04/UC05.
 *  - GoF Strategy: priority selection acts as a configurable strategy parameter.
 */
public class ShipmentOrderService {

    private final ShipmentOrderDAO orderDAO;
    private final InventoryItemDAO itemDAO;
    private final AuditLogDAO      auditDAO;

    public ShipmentOrderService() {
        this.orderDAO = new ShipmentOrderDAO();
        this.itemDAO  = new InventoryItemDAO();
        this.auditDAO = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC04 — Generate Shipment Order (Manual)
    // -------------------------------------------------------------------------

    /**
     * Creates a draft shipment order manually initiated by the Inventory Manager.
     *
     * @param itemId      the inventory item to restock
     * @param qty         the quantity required
     * @param destination the delivery address
     * @param priority    urgency level
     * @param deliveryDate expected delivery date (may be null)
     * @param actorId     the Inventory Manager's UserId
     */
    public void generateOrder(String itemId, int qty, String destination,
                              Priority priority, LocalDate deliveryDate,
                              String actorId) throws OrderException {
        validateOrderInput(itemId, qty, destination);

        ShipmentOrder order = new ShipmentOrder();
        order.setItemId(itemId);
        order.setRequiredQty(qty);
        order.setDestinationAddress(destination);
        order.setPriority(priority);
        order.setExpectedDeliveryDate(deliveryDate);
        order.setStatus(OrderStatus.PENDING_APPROVAL);

        boolean saved = orderDAO.save(order);
        if (!saved) throw new OrderException("Failed to create shipment order.");
        auditDAO.log(actorId, "ORDER_GENERATED_MANUAL", "ShipmentOrder", null);
    }

    /**
     * UC04 — Automatic variant: generates draft orders for all items whose
     * stock has dropped at or below their reorder threshold.
     *
     * Called by the Inventory Manager or on a scheduled trigger.
     *
     * @param actorId the Inventory Manager's UserId
     * @return number of orders generated
     */
    public int generateOrdersForLowStock(String actorId) {
        List<InventoryItem> lowStock = itemDAO.findBelowThreshold();
        int count = 0;
        for (InventoryItem item : lowStock) {
            ShipmentOrder order = new ShipmentOrder();
            order.setItemId(item.getItemId());
            // Request double the threshold quantity to replenish comfortably
            order.setRequiredQty(item.getReorderThreshold() * 2);
            order.setDestinationAddress(item.getWarehouseName() != null
                                        ? item.getWarehouseName()
                                        : "Primary Warehouse");
            // Perishable items automatically get HIGH priority
            order.setPriority(item.isPerishable() ? Priority.HIGH : Priority.MEDIUM);
            order.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
            order.setStatus(OrderStatus.PENDING_APPROVAL);

            if (orderDAO.save(order)) {
                auditDAO.log(actorId, "ORDER_GENERATED_AUTO", "ShipmentOrder", null);
                count++;
            }
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // UC05 — Approve Shipment Order
    // -------------------------------------------------------------------------

    /**
     * Approves a pending order, optionally adjusting the required quantity.
     *
     * @param orderId     the UUID of the order to approve
     * @param adjustedQty the (possibly adjusted) quantity to dispatch
     * @param actorId     the Inventory Manager's UserId
     */
    public void approveOrder(String orderId, int adjustedQty, String actorId)
            throws OrderException {
        ShipmentOrder order = orderDAO.findById(orderId);
        if (order == null) throw new OrderException("Order not found.");
        if (!order.isApprovable())
            throw new OrderException("Only PENDING_APPROVAL orders can be approved. "
                                   + "Current status: " + order.getStatus().getDisplayName());
        if (adjustedQty <= 0)
            throw new OrderException("Adjusted quantity must be greater than zero.");

        boolean ok = orderDAO.approve(orderId, adjustedQty);
        if (!ok) throw new OrderException("Approval failed — order may have changed state.");
        auditDAO.log(actorId, "ORDER_APPROVED", "ShipmentOrder", null);
    }

    // -------------------------------------------------------------------------
    // UC05 — Reject Shipment Order
    // -------------------------------------------------------------------------

    /**
     * Rejects a pending order with a mandatory written reason.
     *
     * @param orderId the UUID of the order to reject
     * @param reason  the rejection rationale (required, non-empty)
     * @param actorId the Inventory Manager's UserId
     */
    public void rejectOrder(String orderId, String reason, String actorId)
            throws OrderException {
        if (reason == null || reason.isBlank())
            throw new OrderException("A rejection reason is mandatory.");

        ShipmentOrder order = orderDAO.findById(orderId);
        if (order == null) throw new OrderException("Order not found.");
        if (!order.isRejectable())
            throw new OrderException("Only PENDING_APPROVAL orders can be rejected. "
                                   + "Current status: " + order.getStatus().getDisplayName());

        boolean ok = orderDAO.reject(orderId, reason);
        if (!ok) throw new OrderException("Rejection failed.");
        auditDAO.log(actorId, "ORDER_REJECTED", "ShipmentOrder", null);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<ShipmentOrder> getAllOrders()    { return orderDAO.findAll(); }
    public List<ShipmentOrder> getPendingOrders(){ return orderDAO.findByStatus(OrderStatus.PENDING_APPROVAL); }
    public List<ShipmentOrder> getApprovedOrders(){ return orderDAO.findByStatus(OrderStatus.APPROVED); }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateOrderInput(String itemId, int qty, String destination)
            throws OrderException {
        if (itemId == null || itemId.isBlank())
            throw new OrderException("An inventory item must be selected.");
        if (qty <= 0)
            throw new OrderException("Required quantity must be greater than zero.");
        if (destination == null || destination.isBlank())
            throw new OrderException("Destination address cannot be empty.");
    }

    // -------------------------------------------------------------------------
    // Inner Exception Class
    // -------------------------------------------------------------------------

    public static class OrderException extends Exception {
        public OrderException(String message) { super(message); }
    }
}