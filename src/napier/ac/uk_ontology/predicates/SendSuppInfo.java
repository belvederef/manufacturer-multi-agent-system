package napier.ac.uk_ontology.predicates;

import java.util.HashMap;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class SendSuppInfo implements Predicate {
  private static final long serialVersionUID = 1L;
  
  private AID supplier;
  private HashMap<ComputerComponent, Integer> componentsForSale; // component, price
  private int speed; // speed in days
  
  @Slot(mandatory = true)
  public AID getSupplier() {
    return supplier;
  }
  public void setSupplier(AID supplier) {
    this.supplier = supplier;
  } 
  @Slot(mandatory = true)
  public HashMap<ComputerComponent, Integer> getComponentsForSale() {
    return componentsForSale;
  }
  public void setComponentsForSale(HashMap<ComputerComponent, Integer> componentsForSale) {
    this.componentsForSale = componentsForSale;
  }
  @Slot(mandatory = true)
  public int getSpeed() {
    return speed;
  }
  public void setSpeed(int speed) {
    this.speed = speed;
  }
}
