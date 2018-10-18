package napier.ac.uk_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Order implements Concept {
  private Computer computer; // All are the same computer, so list probably not needed. If list, use @AggregateSlot(cardMin = 1)
  private int quantity;
  private float price;
  private int dueInDays;
  
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
  public float getPrice() {
    return price;
  }
  public void setPrice(float price) {
    this.price = price;
  }
  @Slot(mandatory = true)
  public int getDueInDays() {
    return dueInDays;
  }
  public void setDueInDays(int dueInDays) {
    this.dueInDays = dueInDays;
  }
  

}
