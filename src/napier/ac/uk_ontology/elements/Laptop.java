package napier.ac.uk_ontology.elements;

import jade.content.onto.annotations.Slot;

public class Laptop extends Computer {
  private String lapCpu;
  private String lapMotherboard;
  private String lapScreen;
  
  @Slot(mandatory = true)
  public String getLapCpu() {
    return lapCpu;
  }
  public void setLapCpu(String lapCpu) {
    this.lapCpu = lapCpu;
  }
  @Slot(mandatory = true)
  public String getLapMotherboard() {
    return lapMotherboard;
  }
  public void setLapMotherboard(String lapMotherboard) {
    this.lapMotherboard = lapMotherboard;
  }
  @Slot(mandatory = true)
  public String getLapScreen() {
    return lapScreen;
  }
  public void setLapScreen(String lapScreen) {
    this.lapScreen = lapScreen;
  }
}
