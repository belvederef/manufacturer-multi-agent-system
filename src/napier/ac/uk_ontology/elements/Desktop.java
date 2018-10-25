package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.elements.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardDesktop;

public class Desktop extends Computer {
  private CpuDesktop cpu;
  private MotherboardDesktop motherboard;
  
  public Desktop() {
  	this.cpu = new CpuDesktop();
  	this.motherboard = new MotherboardDesktop();
  }
  
  @Slot(mandatory = true)
  public CpuDesktop getCpu() {
    return cpu;
  }
  public void setCpu(CpuDesktop deskCpu) {
    this.cpu = deskCpu;
  }
  @Slot(mandatory = true)
  public MotherboardDesktop getMotherboard() {
    return motherboard;
  }
  public void setMotherboard(MotherboardDesktop deskMotherboard) {
    this.motherboard = deskMotherboard;
  }
  
  @Override
  public String toString() {
    return super.toString() + 
		String.format("\n\t"
	        + "cpu: %s, \n\t"
	        + "motherboard: %s, \n\t)",
	        cpu, motherboard);
  }
}
