package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

public class MotherboardLaptop extends Motherboard {

	@Override
	public String toString() {
		return "Laptop motherboard";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof MotherboardLaptop)) {
          return false;
      }

      MotherboardLaptop that = (MotherboardLaptop) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
