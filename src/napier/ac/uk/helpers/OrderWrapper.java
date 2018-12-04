package napier.ac.uk.helpers;

import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;
import napier.ac.uk_ontology.concepts.Order;

// This class is used by the manufacturer to keep track of all the things it 
// needs to know about an order. It includes an order object
public class OrderWrapper {
  private Order order;
  private AID supplierAssigned; // best supplier
  private AID customer;
  private State orderState;
  private HashMap <ComputerComponent, Integer> compsAssigned;
  private int expectedCompsShipDate;
  private int orderedDate;
  private double totalCost;  
  
  public OrderWrapper(Order order) {
    this.setOrder(order);
    compsAssigned = new HashMap<>();
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
    APPROVED, CONFIRMED, DISMISSED, COMPS_RECEIVED, AWAITING_PAYMENT, PAID
  }
  public State getOrderState() {
    return orderState;
  }
  public void setOrderState(State orderState) {
    this.orderState = orderState;
  }
  public HashMap <ComputerComponent, Integer> getCompsAssigned() {
    return compsAssigned;
  }
  public int getExpectedCompsShipDate() {
    return expectedCompsShipDate;
  }
  public void setExpectedCompsShipDate(int expectedCompsShipDate) {
    this.expectedCompsShipDate = expectedCompsShipDate;
  }
  public double getTotalCost() {
    return totalCost;
  }
  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }
  public int getOrderedDate() {
    return orderedDate;
  }
  public void setOrderedDate(int orderedDate) {
    this.orderedDate = orderedDate;
  }
  
  
  // The exact date that an order needs to be delivered by. Used to calc late delivery
  public int getExactDayDue() {
    return this.order.getDueInDays() + orderedDate;
  }
}
