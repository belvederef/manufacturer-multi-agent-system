package napier.ac.uk.helpers;

import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

// This class is used by the manufacturer to keep track of all the things it 
// needs to know about an order. It includes an order object
public class OrderWrapper {
  
  private Order order;
  private AID supplierAssigned; // best supplier
  private AID customer;
  private State orderState;
    
  public OrderWrapper(Order order) {
    this.setOrder(order);
  }

  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  };
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
  }
}
