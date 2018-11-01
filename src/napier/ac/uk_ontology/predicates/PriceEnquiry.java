package napier.ac.uk_ontology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class PriceEnquiry implements Predicate {
  private static final long serialVersionUID = 1L;
  
  private AID manufacturer;
  private ComputerComponent component; // This could be a list of components instead of a single one
  private int quantity;
  
  @Slot(mandatory = true)
  public AID getManufacturer() {
    return manufacturer;
  }
  public void setManufacturer(AID manufacturer) {
    this.manufacturer = manufacturer;
  }
  @Slot(mandatory = true)
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  } 
}
