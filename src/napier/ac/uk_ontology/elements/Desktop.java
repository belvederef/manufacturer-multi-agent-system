package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.elements.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardDesktop;

public class Desktop extends Computer {
  private CpuDesktop deskCpu;
  private MotherboardDesktop deskMotherboard;
  
  public Desktop() {
	this.deskCpu = new CpuDesktop();
	this.deskMotherboard = new MotherboardDesktop();
  }
  
  @Slot(mandatory = true)
  public CpuDesktop getDeskCpu() {
    return deskCpu;
  }
  public void setDeskCpu(CpuDesktop deskCpu) {
    this.deskCpu = deskCpu;
  }
  @Slot(mandatory = true)
  public MotherboardDesktop getDeskMotherboard() {
    return deskMotherboard;
  }
  public void setDeskMotherboard(MotherboardDesktop deskMotherboard) {
    this.deskMotherboard = deskMotherboard;
  }
  
  @Override
  public String toString() {
    return super.toString() + 
		String.format("(\n\t"
	        + "cpu: %s, \n\t"
	        + "motherboard: %s, \n\t)",
	        deskCpu, deskMotherboard);
  }
}
