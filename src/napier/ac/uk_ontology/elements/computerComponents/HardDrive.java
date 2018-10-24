package napier.ac.uk_ontology.elements.computerComponents;

import jade.content.onto.annotations.Slot;

public class HardDrive extends ComputerComponent {
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
