package napier.ac.uk_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.elements.computerComponents.HardDrive;
import napier.ac.uk_ontology.elements.computerComponents.Os;
import napier.ac.uk_ontology.elements.computerComponents.Ram;

//Concepts i.e. expressions that indicate entities with a complex structure that can be defined in
//terms of slots e.g. (Person :name John :age 33)
//Concepts typically make no sense if used directly as the content of an ACL message. I general they
//are referenced inside predicates and other concepts such as in
//(Book :title “The Lord of the rings” :author (Person :name “J.R.R. Tolkjien”)) 

//TODO: make the computer only have enums for ram, hardDrive and OS
public class Computer implements Concept {
  private Ram ram;
  private HardDrive hardDrive; 
  private Os os;

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
  
  @Override
  public String toString() {
    return String.format("(\n\t"
        + "ram: %s, \n\t"
        + "hardDrive: %s, \n\t"
        + "os: %s,",
        ram, hardDrive, os);
  }
}
