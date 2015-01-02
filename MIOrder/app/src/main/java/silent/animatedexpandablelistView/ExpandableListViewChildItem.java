package silent.animatedexpandablelistView;

public class ExpandableListViewChildItem{
    String status;
    String price;
    String tcat;
    String detail;
    String orderdetail;

    public ExpandableListViewChildItem(String status, String price, String tcat){
        this.status = status;
        this.tcat = "運輸單號：" + tcat;
        if (status.length() != 4)
            this.price = "NT$" + price  + "　(" + status + ")";
        else
            this.price = "NT$" + price;
        this.detail = "收件資料";
        this.orderdetail = "訂單明細";
    }

    public String getStatus(){
        return status;
    }

    public String getPrice(){
        return price;
    }

    public String getTCat(){
        return tcat;
    }

    public String getDetail(){
        return detail;
    }

    public String getOrderDetail(){
        return orderdetail;
    }

    public int size(){
        return 1;
    }
}