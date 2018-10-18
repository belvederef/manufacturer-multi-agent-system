package napier.ac.uk_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;

// Action. The customer asks the manufacturer if they can accept this order
public class CanManufacture implements AgentAction {
  private AID manufacturer;
  private Order order;
  
  public AID getManufacturer() {
    return manufacturer;
  }
  public void setManufacturer(AID manufacturer) {
    this.manufacturer = manufacturer;
  }
  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  } 
}

