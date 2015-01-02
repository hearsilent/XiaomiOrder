package silent.animatedexpandablelistView;

public class ExpandableListViewGroupItem{
    String title;
    ExpandableListViewChildItem Child;

    public ExpandableListViewGroupItem(String title, String status, String price, String tcat){
        this.title = "訂單號：" + title;
        Child = new ExpandableListViewChildItem(status, price, tcat);
    }

    public String getTitle(){
        return title;
    }

    public String getStatus(){
        return Child.getStatus();
    }

    public String getPrice(){
        return Child.getPrice();
    }

    public String getTCat(){
        return Child.getTCat();
    }

    public ExpandableListViewChildItem getChild(){
        return Child;
    }
}