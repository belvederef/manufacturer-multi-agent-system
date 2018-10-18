package napier.ac.uk_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;

// Action. The manufacturer sends the order to the buyer
public class ShipOrder implements AgentAction {
  private AID buyer;
  private Order order;
  
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  } 
}

