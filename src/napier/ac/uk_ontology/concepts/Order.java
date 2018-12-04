package napier.ac.uk_ontology.concepts;

import java.util.Objects;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Order implements Concept {
  private static final long serialVersionUID = 1L;
  
  private int orderId = -1; // This is set by the manufacturer
  private Computer computer;
  private int quantity;
  private double price;
  private int dueInDays;
  
  public int getOrderId() {
    return orderId;
  }
  public void setOrderId(int orderId) {
    this.orderId = orderId;
  }
  @Slot(mandatory = true)
  public Computer getComputer() {
    return computer;
  }
  public void setComputer(Computer computer) {
    this.computer = computer;
  }
  @Slot(mandatory = true)
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
  @Slot(mandatory = true)
  public double getPrice() {
    return price;
  }
  public void setPrice(double price) {
    this.price = price;
  }
  @Slot(mandatory = true)
  public int getDueInDays() {
    return dueInDays;
  }
  public void setDueInDays(int dueInDays) {
    this.dueInDays = dueInDays;
  }
  
  @Override
  public String toString() {
    String compString = computer.toString();
    return String.format("(\n computer: %s, \n quantity: %s, \n price: %s, \n dueInDays: %s)",
        compString, quantity, price, dueInDays);
  }
  
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof Order)) {
          return false;
      }

      Order that = (Order) other;

      // Custom equality check here.
      return this.computer.equals(that.computer)
          && this.quantity == that.quantity
          && this.price == that.price
          && this.dueInDays == that.dueInDays;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(computer, quantity, price, dueInDays);
  }
}
