package com.routex.model;
import com.routex.enums.OrderStatus;
import com.routex.enums.Priority;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A ShipmentOrder is created either automatically (stock threshold breach)
 * or manually by the Inventory Manager (UC04).  It is then approved or
 * rejected in UC05 before a vehicle is assigned in UC06.
 *
 * OOP Principles:
 *  - Encapsulation: private fields, controlled state transitions
 *  - Abstraction: hides status string comparisons behind isApprovable()
 */
public class ShipmentOrder {

    private String      orderId;
    private String      itemId;
    private String      itemName;         // Denormalised for display
    private int         requiredQty;
    private String      destinationAddress;
    private Priority    priority;
    private OrderStatus status;
    private LocalDate   expectedDeliveryDate;
    private LocalDateTime createdAt;
    private String      rejectionReason;

    public ShipmentOrder() {}

    public ShipmentOrder(String orderId, String itemId, int requiredQty,
                         String destinationAddress, Priority priority,
                         LocalDate expectedDeliveryDate) {
        this.orderId              = orderId;
        this.itemId               = itemId;
        this.requiredQty          = requiredQty;
        this.destinationAddress   = destinationAddress;
        this.priority             = priority;
        this.status               = OrderStatus.PENDING_APPROVAL;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.createdAt            = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Business Logic
    // -------------------------------------------------------------------------

    /** An order can only be approved if it is still in PENDING_APPROVAL state. */
    public boolean isApprovable() {
        return status == OrderStatus.PENDING_APPROVAL;
    }

    /** An order can only be rejected if it is still in PENDING_APPROVAL state. */
    public boolean isRejectable() {
        return status == OrderStatus.PENDING_APPROVAL;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getOrderId()               { return orderId; }
    public void   setOrderId(String id)      { this.orderId = id; }

    public String getItemId()                { return itemId; }
    public void   setItemId(String itemId)   { this.itemId = itemId; }

    public String getItemName()                  { return itemName; }
    public void   setItemName(String itemName)   { this.itemName = itemName; }

    public int  getRequiredQty()             { return requiredQty; }
    public void setRequiredQty(int qty)      { this.requiredQty = qty; }

    public String getDestinationAddress()                       { return destinationAddress; }
    public void   setDestinationAddress(String addr)            { this.destinationAddress = addr; }

    public Priority getPriority()                   { return priority; }
    public void     setPriority(Priority priority)  { this.priority = priority; }

    public OrderStatus getStatus()                      { return status; }
    public void        setStatus(OrderStatus status)    { this.status = status; }

    public LocalDate getExpectedDeliveryDate()                   { return expectedDeliveryDate; }
    public void      setExpectedDeliveryDate(LocalDate date)     { this.expectedDeliveryDate = date; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void          setCreatedAt(LocalDateTime dt)  { this.createdAt = dt; }

    public String getRejectionReason()                       { return rejectionReason; }
    public void   setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    @Override
    public String toString() {
        return "Order[" + orderId.substring(0, 8) + "] " + itemName + " x" + requiredQty;
    }
}