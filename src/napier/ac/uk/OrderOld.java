package napier.ac.uk;

import jade.core.AID;

public class OrderOld {
  private AID manufacturer;
  private int price;
  private int dueInDays; 
  
  public OrderOld(AID manufacturer, int price) {
    super();
    this.manufacturer = manufacturer;
    this.price = price;
  }
  
  public AID getManufacturer() {
    return manufacturer;
  }
  
  public int getPrice() {
    return price;
  }
  
}
