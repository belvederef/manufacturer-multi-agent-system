package napier.ac.uk_ontology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;


// Useful for checking if a supplier has a computer (with all the computer parts) in stock
public class OwnsComponent implements Predicate {
  private static final long serialVersionUID = 1L;
  
	private AID owner;
	private ComputerComponent component;
	
  @Slot(mandatory = true)
	public AID getOwner() {
		return owner;
	}
	public void setOwner(AID owner) {
		this.owner = owner;
	}
  @Slot(mandatory = true)
  public ComputerComponent getComponent() {
    return component;
  }
  public void setComponent(ComputerComponent component) {
    this.component = component;
  }
}