package napier.ac.uk_ontology.computerComponents;

import java.util.Objects;

public class OsLinux extends Os {
  private static final long serialVersionUID = 1L;
	
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
