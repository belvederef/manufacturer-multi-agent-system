package napier.ac.uk_ontology.elements;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.elements.computerComponents.ComputerComponent;

public class SellComponent implements AgentAction {
  private AID buyer;
  private ComputerComponent component;
  
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
  
  
}
