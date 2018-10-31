package napier.ac.uk_ontology.computerComponents;

import java.util.Objects;

public class MotherboardDesktop extends Motherboard {
  private static final long serialVersionUID = 1L;
  
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
