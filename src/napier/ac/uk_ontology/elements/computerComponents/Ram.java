package napier.ac.uk_ontology.elements.computerComponents;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Ram implements Concept {
	private String capacity;
	
	@Slot(mandatory = true)
	public String getCapacity() {
		return capacity;
	}
	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}
	
	@Override
	public String toString() {
		return this.capacity;
	}
}
