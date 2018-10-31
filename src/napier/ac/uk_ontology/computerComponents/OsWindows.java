package napier.ac.uk_ontology.computerComponents;

import java.util.Objects;

public class OsWindows extends Os {
  private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return "Windows OS";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof OsWindows)) {
          return false;
      }

      OsWindows that = (OsWindows) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
