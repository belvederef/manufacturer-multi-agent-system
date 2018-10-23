package napier.ac.uk_ontology.elements.computerComponents;

// TODO: This could all be simplified having only a concept CPU that has a property type. However, 
// a desktop cpu is a real life entity. I believe this is the right approach, but better to ask.
// The same happens with the OS
public class CpuDesktop extends Cpu {

	@Override
	public String toString() {
		return "Desktop cpu";
	}
}
