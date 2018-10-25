package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

import napier.ac.uk_ontology.elements.Computer;

// TODO: This could all be simplified having only a concept CPU that has a property type. However, 
// a desktop cpu is a real life entity. I believe this is the right approach, but better to ask.
// The same happens with the OS
public class CpuDesktop extends Cpu {

	@Override
	public String toString() {
		return "Desktop cpu";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof CpuDesktop)) {
          return false;
      }

      CpuDesktop that = (CpuDesktop) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
