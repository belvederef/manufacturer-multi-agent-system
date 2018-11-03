package napier.ac.uk_ontology.actions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class AskSuppInfo implements AgentAction {
  private static final long serialVersionUID = 1L;
  
  private AID buyer;

  @Slot(mandatory = true)
  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
}
