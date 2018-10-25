package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

public class OsLinux extends Os {

	
	@Override
	public String toString() {
		return "Linux OS";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof OsLinux)) {
          return false;
      }

      OsLinux that = (OsLinux) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
