package napier.ac.uk_ontology.actions;

import java.util.ArrayList;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class BuyComponents implements AgentAction {
  private static final long serialVersionUID = 1L;
  
  private AID buyer;
  private int orderId;
  private ArrayList<ComputerComponent> components;
  private int quantity;
  
  @Slot(mandatory = true)
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  @Slot(mandatory = true)
  public int getOrderId() {
    return orderId;
  }
  public void setOrderId(int orderId) {
    this.orderId = orderId;
  }
  @Slot(mandatory = true)
  public ArrayList<ComputerComponent> getComponents() {
    return components;
  }
  public void setComponents(ArrayList<ComputerComponent> components) {
    this.components = components;
  }
  @Slot(mandatory = true)
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
