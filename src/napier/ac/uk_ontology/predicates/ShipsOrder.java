package napier.ac.uk_ontology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

public class ShipsOrder implements Predicate {
  private static final long serialVersionUID = 1L;
  
  private AID sender;
  private Order order;
  
  @Slot(mandatory = true)
  public AID getSender() {
    return sender;
  }
  public void setSender(AID sender) {
    this.sender = sender;
  }
  @Slot(mandatory = true)
  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  } 
}
