package napier.ac.uk_ontology.actions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

//Agent actions i.e. special concepts that indicate actions that can be performed by some agents e.g.
//(Sell (Book :title �The Lord of the rings�) (Person :name John))
//It is useful to treat agent actions separately since, unlike �normal� concepts, they are meaningful
//contents of certain types of ACLMessage such as REQUEST. Communicative acts (i.e. ACL
//messages) are themselves agent actions. 


// Action. The manufacturer sends the order to the buyer
public class DELETE_ShipOrder implements AgentAction {
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

