package napier.ac.uk.helpers;

import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import napier.ac.uk_ontology.concepts.ComputerComponent;

public class SuppOrderWrapper {

  private AID buyer;
  private int deliveryDay;
  private ArrayList <ComputerComponent> components;
  private int quantity;

  public AID getBuyer() {
    return buyer;
  }
  public void setBuyer(AID buyer) {
    this.buyer = buyer;
  }
  public int getDeliveryDay() {
    return deliveryDay;
  }
  public void setDeliveryDay(int deliveryDay) {
    this.deliveryDay = deliveryDay;
  }
  public ArrayList <ComputerComponent> getComponents() {
    return components;
  }
  public void setComponents(ArrayList <ComputerComponent> components) {
    this.components = components;
  }
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
