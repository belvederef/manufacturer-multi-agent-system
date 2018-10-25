package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

public class MotherboardDesktop extends Motherboard {

	@Override
	public String toString() {
		return "Desktop motherboard";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof MotherboardDesktop)) {
          return false;
      }

      MotherboardDesktop that = (MotherboardDesktop) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
