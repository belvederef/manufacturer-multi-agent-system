package napier.ac.uk_ontology.actions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class BuyComponent implements AgentAction {
  private static final long serialVersionUID = 1L;
  
  private AID buyer;
  private ComputerComponent component;
  private int quantity;
  
  @Slot(mandatory = true)
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  @Slot(mandatory = true)
  public ComputerComponent getComponent() {
    return component;
  }
  public void setComponent(ComputerComponent component) {
    this.component = component;
  }
  @Slot(mandatory = true)
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
