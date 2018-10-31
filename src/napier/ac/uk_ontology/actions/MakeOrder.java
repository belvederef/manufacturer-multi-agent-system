package napier.ac.uk_ontology.actions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

public class MakeOrder implements AgentAction {
  private static final long serialVersionUID = 1L;
  
  private AID buyer;
  private Order order;
  
  @Slot(mandatory = true)
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  @Slot(mandatory = true)
  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  }
  
  
}
