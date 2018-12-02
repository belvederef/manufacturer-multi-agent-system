package napier.ac.uk_ontology.concepts;

import java.util.ArrayList;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.computerComponents.CpuLaptop;
import napier.ac.uk_ontology.computerComponents.MotherboardLaptop;
import napier.ac.uk_ontology.computerComponents.Screen;

public class Laptop extends Computer {
  private static final long serialVersionUID = 1L;
  
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
  public ArrayList<ComputerComponent> getComponentList() {
    ArrayList<ComputerComponent> componentList = super.getComponentList();
    componentList.add(screen);
    return componentList;
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
  
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof Laptop)) {
          return false;
      }

      Laptop that = (Laptop) other;
      
      // Custom equality check here.
      return super.equals(that)
          && this.screen.equals(that.screen);
  }
  
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 31 * hash + ((cpu == null) ? 0 : cpu.hashCode());
    hash = 31 * hash + ((motherboard == null) ? 0 : motherboard.hashCode());
    hash = 31 * hash + ((screen == null) ? 0 : screen.hashCode());
    return hash;
  }
}
