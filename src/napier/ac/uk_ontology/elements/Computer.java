package napier.ac.uk_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

//Concepts i.e. expressions that indicate entities with a complex structure that can be defined in
//terms of slots e.g. (Person :name John :age 33)
//Concepts typically make no sense if used directly as the content of an ACL message. I general they
//are referenced inside predicates and other concepts such as in
//(Book :title “The Lord of the rings” :author (Person :name “J.R.R. Tolkjien”)) 

//TODO: make ther computer only have enums for ram, hardDrive and OS
public class Computer implements Concept {
  private String cpu;
  private String motherboard;
  private Boolean screen;
  private String ram;
  private String hardDrive; 
  private String os;
//  public enum MyEnum {
//    ONE, TWO;
//  }
  @Slot (mandatory = true)
  public String getCpu() {
    return cpu;
  }
  public void setCpu(String cpu) {
    this.cpu = cpu;
  }
  @Slot (mandatory = true)
  public String getMotherboard() {
    return motherboard;
  }
  public void setMotherboard(String motherboard) {
    this.motherboard = motherboard;
  }
  @Slot (mandatory = true)
  public Boolean getScreen() {
    return screen;
  }
  public void setScreen(Boolean screen) {
    this.screen = screen;
  }
  @Slot (mandatory = true)
  public String getRam() {
    return ram;
  }
  public void setRam(String ram) {
    this.ram = ram;
  }
  @Slot (mandatory = true)
  public String getHardDrive() {
    return hardDrive;
  }
  public void setHardDrive(String hardDrive) {
    this.hardDrive = hardDrive;
  }
  @Slot (mandatory = true)
  public String getOs() {
    return os;
  }
  public void setOs(String os) {
    this.os = os;
  }
  
  @Override
  public String toString() {
    return String.format("(\n\t"
        + "cpu: %s, \n\t"
        + "motherboard: %s, \n\t"
        + "screen: %s, \n\t"
        + "ram: %s, \n\t"
        + "hardDrive: %s, \n\t"
        + "os: %s, \n\t)",
        cpu, motherboard, screen, ram, hardDrive, os);
  }
}
