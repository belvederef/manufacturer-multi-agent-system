package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.elements.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.elements.computerComponents.Screen;

public class Laptop extends Computer {
  private CpuLaptop lapCpu;
  private MotherboardLaptop lapMotherboard;
  private Screen lapScreen;
  
  public Laptop() {
	this.lapCpu = new CpuLaptop();
	this.lapMotherboard = new MotherboardLaptop();
	this.lapScreen = new Screen();
  }
  
  @Slot(mandatory = true)
  public CpuLaptop getLapCpu() {
    return lapCpu;
  }
  public void setLapCpu(CpuLaptop lapCpu) {
    this.lapCpu = lapCpu;
  }
  @Slot(mandatory = true)
  public MotherboardLaptop getLapMotherboard() {
    return lapMotherboard;
  }
  public void setLapMotherboard(MotherboardLaptop lapMotherboard) {
    this.lapMotherboard = lapMotherboard;
  }
  @Slot(mandatory = true)
  public Screen getLapScreen() {
    return lapScreen;
  }
  public void setLapScreen(Screen lapScreen) {
    this.lapScreen = lapScreen;
  }
  
  @Override
  public String toString() {
    return super.toString() + 
		String.format("(\n\t"
	        + "cpu: %s, \n\t"
	        + "motherboard: %s, \n\t"
	        + "screen: %s, \n\t)",
	        lapCpu, lapMotherboard, lapScreen);
  }
}
