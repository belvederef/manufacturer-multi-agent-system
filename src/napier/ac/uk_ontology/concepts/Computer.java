package napier.ac.uk_ontology.concepts;

import java.util.ArrayList;
import java.util.Objects;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.computerComponents.Cpu;
import napier.ac.uk_ontology.computerComponents.HardDrive;
import napier.ac.uk_ontology.computerComponents.Motherboard;
import napier.ac.uk_ontology.computerComponents.Os;
import napier.ac.uk_ontology.computerComponents.Ram;
import napier.ac.uk_ontology.computerComponents.Screen;

//Concepts i.e. expressions that indicate entities with a complex structure that can be defined in
//terms of slots e.g. (Person :name John :age 33)
//Concepts typically make no sense if used directly as the content of an ACL message. I general they
//are referenced inside predicates and other concepts such as in
//(Book :title �The Lord of the rings� :author (Person :name �J.R.R. Tolkjien�)) 

//TODO: make the computer only have enums for ram, hardDrive and OS
public abstract class Computer implements Concept {
  private static final long serialVersionUID = 1L;
  
  private Ram ram;
  private HardDrive hardDrive; 
  private Os os;
  private Cpu cpu;
  private Motherboard motherboard;
  private Screen screen;
  private ArrayList<ComputerComponent> componentList;

  @Slot (mandatory = true)
  public Ram getRam() {
    return ram;
  }
  public void setRam(Ram ram) {
    this.ram = ram;
  }
  @Slot (mandatory = true)
  public HardDrive getHardDrive() {
    return hardDrive;
  }
  public void setHardDrive(HardDrive hardDrive) {
    this.hardDrive = hardDrive;
  }
  @Slot (mandatory = true)
  public Os getOs() {
    return os;
  }
  public void setOs(Os os) {
    this.os = os;
  }
  
  @Slot (mandatory = true)
  public Motherboard getMotherboard() {
    return motherboard;
  }
  public void setMotherboard(Motherboard motherboard) {
    this.motherboard = motherboard;
  }
  @Slot (mandatory = true)
  public Cpu getCpu() {
    return cpu;
  }
  public void setCpu(Cpu cpu) {
    this.cpu = cpu;
  }
  @Slot (mandatory = false)
  public Screen getScreen() {
    return screen;
  }
  public void setScreen(Screen screen) {
    this.screen = screen;
  }
  @Slot (mandatory = false)
  public ArrayList<ComputerComponent> getComponentList() {
    componentList = new ArrayList<>();
    componentList.add(ram);
    componentList.add(hardDrive);
    componentList.add(os);
    componentList.add(cpu);
    componentList.add(motherboard);
    return componentList;
  }
  
  @Override
  public String toString() {
    return String.format("(\n\t"
        + "ram: %s, \n\t"
        + "hardDrive: %s, \n\t"
        + "os: %s,",
        ram, hardDrive, os);
  }
  
  @Override
  public boolean equals(Object other) {
      if (!(other instanceof Computer)) {
          return false;
      }

      Computer that = (Computer) other;

      // Custom equality check here.
      return this.ram.equals(that.ram)
          && this.hardDrive.equals(that.hardDrive)
          && this.os.equals(that.os)
          && this.cpu.equals(that.cpu)
          && this.motherboard.equals(that.motherboard);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(ram, hardDrive, os, cpu, motherboard, screen);
  }
}
