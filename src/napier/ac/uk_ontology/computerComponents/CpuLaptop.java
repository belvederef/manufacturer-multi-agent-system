package napier.ac.uk_ontology.computerComponents;

import java.util.Objects;

public class CpuLaptop extends Cpu {
  private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return "Laptop cpu";
	}
	
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof CpuLaptop)) {
          return false;
      }

      CpuLaptop that = (CpuLaptop) other;

      // Custom equality check here.
      return this.toString().equals(that.toString());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
