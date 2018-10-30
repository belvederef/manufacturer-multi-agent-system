package napier.ac.uk_ontology.elements.computerComponents;

import java.util.Objects;

import jade.content.onto.annotations.Slot;

public class Ram extends ComputerComponent {
	private String capacity;
	
//	public Ram () {}
	 
	public Ram (String capacity) { 
	  this.capacity = capacity;
	}
	
	@Slot(mandatory = true)
	public String getCapacity() {
		return capacity;
	}
	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}
	
	@Override
	public String toString() {
		return this.capacity;
	}
	
	 @Override
	  public boolean equals(Object other) {
	      if (!(other instanceof Ram)) {
	          return false;
	      }

	      Ram that = (Ram) other;

	      // Custom equality check here.
	      return this.capacity.equals(that.capacity);
	  }
	  
	  @Override
	  public int hashCode() {
	    return Objects.hash(this.capacity);
	  }
}
