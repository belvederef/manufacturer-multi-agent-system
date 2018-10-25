package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.elements.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.elements.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.elements.computerComponents.Screen;

public class Laptop extends Computer {
  private CpuLaptop cpu;
  private MotherboardLaptop motherboard;
  private Screen screen;
  
  public Laptop() {
	this.cpu = new CpuLaptop();
	this.motherboard = new MotherboardLaptop();
	this.screen = new Screen();
  }
  
  @Slot(mandatory = true)
  public CpuLaptop getCpu() {
    return cpu;
  }
  public void setCpu(CpuLaptop lapCpu) {
    this.cpu = lapCpu;
  }
  @Slot(mandatory = true)
  public MotherboardLaptop getMotherboard() {
    return motherboard;
  }
  public void setMotherboard(MotherboardLaptop lapMotherboard) {
    this.motherboard = lapMotherboard;
  }
  @Slot(mandatory = true)
  public Screen getScreen() {
    return screen;
  }
  public void setScreen(Screen lapScreen) {
    this.screen = lapScreen;
  }
  
  @Override
  public String toString() {
    return super.toString() + 
		String.format("\n\t"
	        + "cpu: %s, \n\t"
	        + "motherboard: %s, \n\t"
	        + "screen: %s, \n\t)",
	        cpu, motherboard, screen);
  }
}
