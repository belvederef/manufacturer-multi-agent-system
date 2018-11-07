package napier.ac.uk_ontology.predicates;

import java.util.ArrayList;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;


// Useful for checking if a supplier has a computer (with all the computer parts) in stock
public class OwnsComponents implements Predicate {
  private static final long serialVersionUID = 1L;
  
	private AID owner;
	private ArrayList<ComputerComponent> components;
	private int quantity;
	
  @Slot(mandatory = true)
	public AID getOwner() {
		return owner;
	}
	public void setOwner(AID owner) {
		this.owner = owner;
	}
  @Slot(mandatory = true)
  public ArrayList<ComputerComponent> getComponents() {
    return components;
  }
  public void setComponents(ArrayList<ComputerComponent> components) {
    this.components = components;
  }
  @Slot(mandatory = true)
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
