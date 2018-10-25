package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

public class Screen extends ComputerComponent {

	@Override
	public String toString() {
		return "Laptop screen";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof Screen)) {
          return false;
      }

      Screen that = (Screen) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
