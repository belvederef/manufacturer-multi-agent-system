package napier.ac.uk_ontology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.Order;

//are expressions that say something about the status of the world and can be
//true or false e.g. (Works-for (Person :name John) (Company :name TILAB))
//stating that �the person John works for the company TILAB�.
//Predicates can be meaningfully used for instance as the content of an INFORM or QUERY-IF
//message, while would make no sense if used as the content of a REQUEST message. 

// The customer asks the manufacturer if they can accept this order
public class CanManufacture implements Predicate {
  private static final long serialVersionUID = 1L;
  
  private AID manufacturer;
  private Order order;
  
  @Slot(mandatory = true)
  public AID getManufacturer() {
    return manufacturer;
  }
  public void setManufacturer(AID manufacturer) {
    this.manufacturer = manufacturer;
  }
  @Slot(mandatory = true)
  public Order getOrder() {
    return order;
  }
  public void setOrder(Order order) {
    this.order = order;
  } 
}

