package napier.ac.uk_ontology.predicates;

import java.util.ArrayList;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

// Hashmaps are not supported... use lists. Could not find a way to make this work
public class SendsSuppInfo implements Predicate {
  private static final long serialVersionUID = 1L;
  
  private AID supplier;
  private ArrayList<ComputerComponent> componentsForSaleKeys; // components
  private ArrayList<Long> componentsForSaleVal; // prices
  private int speed; // speed in days
  
  @Slot(mandatory = true)
  public AID getSupplier() {
    return supplier;
  }
  public void setSupplier(AID supplier) {
    this.supplier = supplier;
  } 
  @Slot(mandatory = true)
  public ArrayList<ComputerComponent> getComponentsForSaleKeys() {
    return componentsForSaleKeys;
  }
  public void setComponentsForSaleKeys(ArrayList<ComputerComponent> componentsForSaleKeys) {
    this.componentsForSaleKeys = componentsForSaleKeys;
  }
  @Slot(mandatory = true)
  public ArrayList<Long> getComponentsForSaleVal() {
    return componentsForSaleVal;
  }
  public void setComponentsForSaleVal(ArrayList<Long> componentsForSaleVal) {
    this.componentsForSaleVal = componentsForSaleVal;
  }
  @Slot(mandatory = true)
  public int getSpeed() {
    return speed;
  }
  public void setSpeed(int speed) {
    this.speed = speed;
  }
}
