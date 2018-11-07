package napier.ac.uk.helpers;

import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

// This class is used by the manufacturer to keep track of all the things it 
// needs to know about an order. It extends the normal order class
public class OrderManuf extends Order {
  private static final long serialVersionUID = 1L;
  
  private AID supplierAssigned; // best supplier
  private AID customer;
  private State orderState;
    
  public OrderManuf(Order order) {
    super();
    this.setComputer(order.getComputer());
    this.setPrice(order.getPrice());
    this.setQuantity(order.getQuantity());
    this.setDueInDays(order.getDueInDays());
  }

  public AID getSupplierAssigned() {
    return supplierAssigned;
  }
  public void setSupplierAssigned(AID supplierAssigned) {
    this.supplierAssigned = supplierAssigned;
  }
  public AID getCustomer() {
    return customer;
  }
  public void setCustomer(AID customer) {
    this.customer = customer;
  }
  
  // Approved and confirmed are set by manufac 
  public enum State{
    APPROVED, CONFIRMED,
  }
  public State getOrderState() {
    return orderState;
  }
  public void setOrderState(State orderState) {
    this.orderState = orderState;
  };
}
