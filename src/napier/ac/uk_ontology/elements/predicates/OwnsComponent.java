package napier.ac.uk_ontology.elements.predicates;

import jade.content.Predicate;
import jade.core.AID;
import napier.ac.uk_ontology.elements.Computer;


// Useful for checking if a supplier has a computer (with all the computer parts) in stock
public class OwnsComponent implements Predicate {
	private AID owner;
	private Computer computer;
	
	public AID getOwner() {
		return owner;
	}
	
	public void setOwner(AID owner) {
		this.owner = owner;
	}
	
	public Computer getComputer() {
		return computer;
	}
	
	public void setComputer(Computer computer) {
		this.computer = computer;
	}
	
}
