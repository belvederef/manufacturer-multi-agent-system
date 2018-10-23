package napier.ac.uk_ontology.elements.computerComponents;

import jade.content.onto.annotations.Slot;

public class HardDrive {
	private String capacity;
	
	public HardDrive(String capacity) {
		this.capacity = capacity;
	}
	
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
