package silent.miorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.alertdialogpro.ProgressDialogPro;
import com.cengalabs.flatui.views.FlatButton;
import com.cocosw.bottomsheet.BottomSheet;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import silent.pullrefreshlayout.PullRefreshLayout;
import silent.titanic.Titanic;
import silent.titanic.TitanicTextView;
import silent.titanic.Typefaces;
import us.codecraft.xsoup.Xsoup;

import static android.view.Gravity.START;

public class MainActivity extends Activity {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0";

    String _loginPassportUrl = "https://account.xiaomi.com/pass/serviceLoginAuth2";
    String _orderView = "http://buy.mi.com/tw/user/orderView/";
    String _cancelOrder  = "http://buy.mi.com/tw/user/cancelOrder/";
    String _confirmOrder = "http://buy.mi.com/tw/buy/confirm/";
    String _orderUrl = "http://buy.mi.com/tw/user/order";
    String _loginUrl = "http://buy.mi.com/tw/site/login";

    CookieStore cookieStore = new BasicCookieStore();

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private MaterialEditText UserNameEditText;
    private MaterialEditText PasswordEditText;
    private FlatButton SignInButton;

    Runnable OrderRunnable;
    Runnable LoginRunnable;
    Runnable PinCodeRunnable;
    Runnable CancelRunnable;

    private String _bank = "seven_eleven";
    private String _OrderId = "";

    AlertDialog LoadingDialog;
    private int mSingleChoice = 0;

    private AnimatedExpandableListView mList;
    private ExampleAdapter OrderListAdapter;

    String[][] Detail = new String[][]{};
    int[][] OrderDetailCount = new int[][]{};
    String[][] OrderDetail = new String[][]{};
    List<GroupItem> items = new ArrayList<>();
    int OldPosition = -1;
    String NowLayout = "Main";

    PullRefreshLayout layout;

    private DrawerArrowDrawable drawerArrowDrawable;
    private float offset;
    private boolean flipped;
    private ImageView githubImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (NowLayout.equals("Main"))
        {
            setContentView(R.layout.activity_main);
            InitMain();
        }
        else if (NowLayout.equals("Order"))
        {
            setContentView(R.layout.activity_order);
            InitOrder();
            new Thread(OrderRunnable).start();
        }
        else if (NowLayout.equals("About"))
        {
            setContentView(R.layout.activity_about);
            InitAbout();
        }


        LoginRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    LoginHandler.sendEmptyMessage(-1);
                    List<NameValuePair> params = new LinkedList<>();
                    params.add(new BasicNameValuePair("user", UserNameEditText.getText().toString()));
                    params.add(new BasicNameValuePair("_json", "true"));
                    params.add(new BasicNameValuePair("pwd", PasswordEditText.getText().toString()));
                    params.add(new BasicNameValuePair("callback", "https://account.xiaomi.com"));
                    params.add(new BasicNameValuePair("sid", "passport"));
                    params.add(new BasicNameValuePair("qs", "%3Fsid%3Dpassport"));
                    params.add(new BasicNameValuePair("hidden", ""));
                    params.add(new BasicNameValuePair("_sign", "KKkRvCpZoDC+gLdeyOsdMhwV0Xg="));
                    params.add(new BasicNameValuePair("serviceParam","{\"checkSafePhone\":false}"));
                    JSONObject jsonObj = new JSONObject(post_url_contents(_loginPassportUrl, params, cookieStore).replace("&&&START&&&", ""));
                    if (!jsonObj.optString("desc").equals("成功"))
                    {
                        Thread.sleep(1500);
                        LoginHandler.sendEmptyMessage(1);
                    }
                    else
                    {
                        get_url_contents(_loginUrl, null, cookieStore);
                        savePrefs();
                        LoginHandler.sendEmptyMessage(2);
                    }
                } catch (Exception e) {
                    LoginHandler.sendEmptyMessage(1);
                    e.printStackTrace();
                }
            }
        };

        PinCodeRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    List<NameValuePair> params = new LinkedList<>();
                    params.add(new BasicNameValuePair("bank", _bank));
                    Document document = Jsoup.parse( post_url_contents(_confirmOrder + _OrderId + "#", params, cookieStore) );
                    Message message = Message.obtain();
                    message.obj = Xsoup.compile("/html/body/div[3]/div/div[2]/div/div[1]/ul/li[2]").evaluate(document).getElements().text();
                    message.what = 1;
                    PincodeHandler.sendMessage(message);
                } catch (Exception e) {
                    PincodeHandler.sendEmptyMessage(-1);
                    e.printStackTrace();
                }
            }
        };

        CancelRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    get_url_contents(_cancelOrder + _OrderId, null, cookieStore);
                    Document document = Jsoup.parse( get_url_contents(_orderView + _OrderId, null, cookieStore) );
                    if (document.html().contains("已關閉"))
                        CancleHandler.sendEmptyMessage(1);
                    else
                        CancleHandler.sendEmptyMessage(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        OrderRunnable = new Runnable(){
            @Override
            public void run() {
                try {
                    OrderHandler.sendEmptyMessage(-1);

                    items.clear();
                    OrderHandler.sendEmptyMessage(1);

                    String _Payment = null, _Tcat = "", _Status = null, _Xiaomi = null;
                    int OrderItemCount = 0, OrderItemCounter = -1;
                    ArrayList<String> PageList = new ArrayList<>();
                    PageList.add(_orderUrl);
                    Document document = Jsoup.parse(get_url_contents(_orderUrl, null, cookieStore));
                    if ( Xsoup.compile("html/body/div[4]/div/div/div[2]/div/div/div/ul/div/a").evaluate(document).get() != null)
                    {
                        List<String> list = Xsoup.compile("html/body/div[4]/div/div/div[2]/div/div/div/ul/div/a").evaluate(document).list();
                        for (int i = 1; i < list.size(); i++)
                            PageList.add(Xsoup.compile("html/body/div[4]/div/div/div[2]/div/div/div/ul/div/a[" + i + "]").evaluate(document).getElements().attr("href"));
                    }

                    for (String PageUrl : PageList)
                        OrderItemCount += Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li").evaluate(Jsoup.parse(get_url_contents(PageUrl, null, cookieStore))).list().size();
                    Detail = new String[OrderItemCount][3];
                    OrderDetail = new String[OrderItemCount][2000];
                    OrderDetailCount = new int[OrderItemCount][1];
                    for (String PageUrl : PageList)
                    {
                        if (!NowLayout.equals("Order"))
                            break;
                        if (!PageUrl.equals(_orderUrl))
                            document = Jsoup.parse(get_url_contents(PageUrl, null, cookieStore));
                        List<String> list = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li").evaluate(document).list();
                        for (int i = 1; i <= list.size(); i++)
                        {
                            if (!NowLayout.equals("Order"))
                                break;
                            if (Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li[" + i + "]/table/thead/tr/th/div/span[1]/a").evaluate(document).get() == null)
                                continue;
                            _Xiaomi = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li[" + i + "]/table/thead/tr/th/div/span[1]/a").evaluate(document).getElements().text();
                            Document documentx = Jsoup.parse(get_url_contents(_orderView + _Xiaomi, null, cookieStore));
                            if (Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[3]/table/tbody/tr[2]").evaluate(documentx).get() != null)
                                _Tcat = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[3]/table/tbody/tr[2]/td").evaluate(documentx).getElements().text();
                            OrderItemCounter ++;
                            Detail[OrderItemCounter][0] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[1]/td").evaluate(documentx).getElements().text();
                            Detail[OrderItemCounter][1] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[2]/td").evaluate(documentx).getElements().text().split(" ")[ Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[2]/td").evaluate(documentx).getElements().text().split(" ").length - 1];
                            Detail[OrderItemCounter][2] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[3]/td").evaluate(documentx).getElements().text();
                            for (int j = 1; j <= Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li").evaluate(documentx).list().size(); j++)
                            {
                                if (Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li[" + j + "]/a[2]").evaluate(documentx).get() == null)
                                    continue;
                                OrderDetailCount[OrderItemCounter][0] = j;
                                OrderDetail[OrderItemCounter][j - 1] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li[" + j + "]/a[2]").evaluate(documentx).getElements().text();
                            }
                            _Payment = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[3]/div/span/span").evaluate(documentx).getElements().text();
                            _Status = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[4]/div/span").evaluate(documentx).getElements().text();
                            GroupItem item = new GroupItem();

                            item.title = "訂單號：" + _Xiaomi;
                            ChildItem child = new ChildItem();
                            child.title = "目前狀態：" + _Status;
                            if (_Status.length() == 4)
                                child.hint = "點擊繳費";
                            item.items.add(child);
                            child = new ChildItem();
                            child.title = "金額：" + _Payment + "元";
                            item.items.add(child);
                            child = new ChildItem();
                            child.title = "運輸單號：" + _Tcat;
                            item.items.add(child);
                            child = new ChildItem();
                            child.title = "收件資料";
                            child.hint = "點擊查看";
                            item.items.add(child);
                            child = new ChildItem();
                            child.title = "訂單明細";
                            child.hint = "點擊查看";
                            item.items.add(child);
                            items.add(item);

                            _Tcat = "";
                            OrderHandler.sendEmptyMessage(1);
                        }
                    }
                    OrderHandler.sendEmptyMessage(2);
                } catch (Exception e) {
                    OrderHandler.sendEmptyMessage(2);
                    e.printStackTrace();
                }
            }
        };

        LoadingDialog = new ProgressDialogPro(this, R.style.Theme_AlertDialogPro_Material);
    }
    private static class GroupItem {
        String title;
        List<ChildItem> items = new ArrayList<ChildItem>();
    }
    private static class ChildItem {
        String title;
        String hint;
    }
    private static class ChildHolder {
        TextView title;
        TextView hint;
    }
    private static class GroupHolder {
        TextView title;
    }
    /**
     * Adapter for our list of {@link GroupItem}s.
     */
    private class ExampleAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {
        private LayoutInflater inflater;
        private List<GroupItem> items;
        public ExampleAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }
        public void setData(List<GroupItem> items) {
            this.items = items;
        }
        @Override
        public ChildItem getChild(int groupPosition, int childPosition) {
            return items.get(groupPosition).items.get(childPosition);
        }
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        @Override
        public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ChildHolder holder;
            ChildItem item = getChild(groupPosition, childPosition);
            if (convertView == null) {
                holder = new ChildHolder();
                convertView = inflater.inflate(R.layout.list_item, parent, false);
                holder.title = (TextView) convertView.findViewById(R.id.textTitle);
                holder.hint = (TextView) convertView.findViewById(R.id.textHint);
                convertView.setTag(holder);
            } else {
                holder = (ChildHolder) convertView.getTag();
            }
            holder.title.setText(item.title);
            holder.hint.setText(item.hint);
            if (item.hint == null)
            {
                holder.title.setHeight(92);
                holder.hint.setTextSize(0);
            }
            else
            {
                holder.title.setHeight(47);
                holder.hint.setTextSize(13);
            }
            return convertView;
        }
        @Override
        public int getRealChildrenCount(int groupPosition) {
            return items.get(groupPosition).items.size();
        }
        @Override
        public GroupItem getGroup(int groupPosition) {
            return items.get(groupPosition);
        }
        @Override
        public int getGroupCount() {
            return items.size();
        }
        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupHolder holder;
            GroupItem item = getGroup(groupPosition);
            if (convertView == null) {
                holder = new GroupHolder();
                convertView = inflater.inflate(R.layout.group_item, parent, false);
                holder.title = (TextView) convertView.findViewById(R.id.textTitle);
                convertView.setTag(holder);
            } else {
                holder = (GroupHolder) convertView.getTag();
            }
            holder.title.setText(item.title);
            return convertView;
        }
        @Override
        public boolean hasStableIds() {
            return true;
        }
        @Override
        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }
    }

    private BottomSheet.Builder getShareActions(BottomSheet.Builder builder, String text) {
        PackageManager pm = this.getPackageManager();
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        final List<ResolveInfo> list = pm.queryIntentActivities(shareIntent, 0);
        for (int i = 0; i < list.size(); i++) {
            builder.sheet(i,list.get(i).loadIcon(pm),list.get(i).loadLabel(pm));
        }
        builder.listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityInfo activity = list.get(which).activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                        activity.name);
                Intent newIntent = (Intent) shareIntent.clone();
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                newIntent.setComponent(name);
                startActivity(newIntent);
            }
        });
        return builder;
    }

    void InitMain()
    {
        SignInButton = (FlatButton) findViewById(R.id.SignIn);
        UserNameEditText = (MaterialEditText) findViewById(R.id.UserName);
        PasswordEditText = (MaterialEditText) findViewById(R.id.Password);

        SignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(LoginRunnable).start();
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(UserNameEditText.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(PasswordEditText.getWindowToken(), 0);
                SignInButton.setEnabled(false);
                UserNameEditText.setEnabled(false);
                PasswordEditText.setEnabled(false);
            }
        });

        restorePrefs();
    }

    void InitAbout()
    {
        TitanicTextView tv = (TitanicTextView) findViewById(R.id.my_text_view);
        // set fancy typeface
        tv.setTypeface(Typefaces.get(this, "Satisfy-Regular.ttf"));
        // start animation
        new Titanic().start(tv);
        githubImage = (ImageView) findViewById(R.id.imageView);
        githubImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hearsilent/XiaomiOrder"));
                startActivity(browserIntent);
            }
        });

        mDrawerList = (ListView)findViewById(R.id.drawerlistView);
        String[] values = new String[]{ "Order", "Sign out","Bug Report", "Share" };
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                this,android.R.layout.simple_list_item_1, values){
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);
                TextView textView=(TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                return view;
            }
        };
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        setContentView(R.layout.activity_order);
                        NowLayout = "Order";
                        onCreate(null);
                        break;
                    case 1:
                        get_url_contents("http://buy.mi.com/tw/site/logout", null, cookieStore);
                        NowLayout = "Main";
                        setContentView(R.layout.activity_main);
                        onCreate(null);
                        break;
                    case 2:
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hearsilent/XiaomiOrder/issues/new"));
                        startActivity(browserIntent);
                        break;
                    case 3:
                        getShareActions(new BottomSheet.Builder(MainActivity.this, R.style.BottomSheet_StyleDialog).grid().title("Share"),getString(R.string.app_description) + "\n" +
                                "GitHub Page : https://github.com/hearsilent/XiaomiOrder").build().show();
                        break;
                }
            }
        });

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ImageView imageView = (ImageView) findViewById(R.id.drawer_indicator);
        final Resources resources = getResources();
        drawerArrowDrawable = new DrawerArrowDrawable(resources);
        drawerArrowDrawable.setStrokeColor(Color.WHITE);
        imageView.setImageDrawable(drawerArrowDrawable);
        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {
                offset = slideOffset;
                // Sometimes slideOffset ends up so close to but not quite 1 or 0.
                if (slideOffset >= .995) {
                    flipped = true;
                    drawerArrowDrawable.setFlip(flipped);
                } else if (slideOffset <= .005) {
                    flipped = false;
                    drawerArrowDrawable.setFlip(flipped);
                }
                drawerArrowDrawable.setParameter(offset);
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (drawer.isDrawerVisible(START)) {
                    drawer.closeDrawer(START);
                } else {
                    drawer.openDrawer(START);
                }
            }
        });

    }

    void InitOrder()
    {
        mList = (AnimatedExpandableListView) super.findViewById(R.id.OrderList);
        layout = (PullRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        layout.setRefreshStyle(PullRefreshLayout.STYLE_MATERIAL);
        layout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(OrderRunnable).start();
            }
        });

        mDrawerList = (ListView)findViewById(R.id.drawerlistView);
        String[] values = new String[]{ "Sign out","Bug Report", "Share", "About" };
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                this,android.R.layout.simple_list_item_1, values){
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);
                TextView textView=(TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                return view;
            }
        };
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                 switch (position) {
                    case 0:
                        get_url_contents("http://buy.mi.com/tw/site/logout", null, cookieStore);
                        NowLayout = "Main";
                        setContentView(R.layout.activity_main);
                        onCreate(null);
                        break;
                    case 1:
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hearsilent/XiaomiOrder/issues/new"));
                        startActivity(browserIntent);
                        break;
                    case 2:
                        getShareActions(new BottomSheet.Builder(MainActivity.this, R.style.BottomSheet_StyleDialog).grid().title("Share"),getString(R.string.app_description) + "\n" +
                                "GitHub Page : https://github.com/hearsilent/XiaomiOrder").build().show();
                        break;
                     case 3:
                         setContentView(R.layout.activity_about);
                         NowLayout = "About";
                         onCreate(null);
                         break;
                }
            }
        });

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ImageView imageView = (ImageView) findViewById(R.id.drawer_indicator);
        final Resources resources = getResources();
        drawerArrowDrawable = new DrawerArrowDrawable(resources);
        drawerArrowDrawable.setStrokeColor(Color.WHITE);
        imageView.setImageDrawable(drawerArrowDrawable);
        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {
                offset = slideOffset;
                // Sometimes slideOffset ends up so close to but not quite 1 or 0.
                if (slideOffset >= .995) {
                    flipped = true;
                    drawerArrowDrawable.setFlip(flipped);
                } else if (slideOffset <= .005) {
                    flipped = false;
                    drawerArrowDrawable.setFlip(flipped);
                }
                drawerArrowDrawable.setParameter(offset);
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (drawer.isDrawerVisible(START)) {
                    drawer.closeDrawer(START);
                } else {
                    drawer.openDrawer(START);
                }
            }
        });

        mList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                if (layout.isRefreshing())
                    return true;
                switch (childPosition) {
                    case 0:
                        if (OrderListAdapter.getChild(groupPosition, childPosition).title.length() == 9) {
                            mSingleChoice = 0;
                            new AlertDialogPro.Builder(MainActivity.this)
                                    .setIcon(R.drawable.ic_payment_white_48dp)
                                    .setTitle("繳費")
                                    .setSingleChoiceItems(new String[]{"7-11繳費", "全家繳費", "取消訂單"},
                                            0,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    mSingleChoice = which;
                                                }
                                            })
                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (mSingleChoice)
                                            {
                                                case 0:
                                                    _bank = "seven_eleven";
                                                    LoadingDialog.setMessage("Loading...");
                                                    LoadingDialog.show();
                                                    new Thread(PinCodeRunnable).start();
                                                    break;
                                                case 1:
                                                    _bank = "familymart";
                                                    LoadingDialog.setMessage("Loading...");
                                                    LoadingDialog.show();
                                                    new Thread(PinCodeRunnable).start();
                                                    break;
                                                case 2:
                                                    new AlertDialogPro.Builder(MainActivity.this)
                                                            .setTitle("MI Order")
                                                            .setIcon(R.drawable.ic_warning_amber_48dp)
                                                            .setMessage("是否取消訂單？")
                                                            .setNegativeButton("取消", null)
                                                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    LoadingDialog.setMessage("Loading...");
                                                                    LoadingDialog.show();
                                                                    new Thread(CancelRunnable).start();
                                                                }
                                                            }).show();
                                                    break;
                                            }
                                        }
                                    }).show();
                        }
                        break;
                    case 2:
                        if (!OrderListAdapter.getChild(groupPosition, childPosition).title.equals("運輸單號：")) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.t-cat.com.tw/Inquire/TraceDetail.aspx?BillID=" + OrderListAdapter.getChild(groupPosition, childPosition).title.split("：")[1]));
                            startActivity(browserIntent);
                        }
                        break;
                    case 3:
                        new AlertDialogPro.Builder(MainActivity.this).setTitle("收件資料")
                                .setIcon(R.drawable.ic_contacts_white_48dp)
                                .setItems(Detail[groupPosition], null)
                                .setPositiveButton("確定", null)
                                .show();
                        break;
                    case 4:
                        String[] NewOrderDetail = Arrays.copyOf(OrderDetail[groupPosition], OrderDetailCount[groupPosition][0]);
                        System.out.println(OrderDetailCount[groupPosition][0]);
                        new AlertDialogPro.Builder(MainActivity.this).setTitle("訂單明細")
                                .setIcon(R.drawable.ic_receipt_white_48dp)
                                .setItems(NewOrderDetail, null)
                                .setPositiveButton("確定", null)
                                .show();
                        break;
                }
                return false;
            }
        });

        mList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem != 0)
                    layout.setEnabled(false);
                else
                    layout.setEnabled(true);
            }
        });

        mList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // We call collapseGroupWithAnimation(int) and
                // expandGroupWithAnimation(int) to animate group
                // expansion/
                if (layout.isRefreshing())
                    return true;

                _OrderId = OrderListAdapter.getGroup(groupPosition).title.split("：")[1];
                if (OldPosition != -1 && OldPosition != groupPosition)
                    mList.collapseGroup(OldPosition);
                OldPosition = groupPosition;
                if (mList.isGroupExpanded(groupPosition))
                    mList.collapseGroupWithAnimation(groupPosition);
                else
                    mList.expandGroupWithAnimation(groupPosition);
                return true;
            }
        });
    }

    String get_url_contents( String url , List<NameValuePair> params , CookieStore cookieStore ) {
        try {
            HttpClient client = MySSLSocketFactory.createMyHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
            HttpResponse response = null;
            if( cookieStore == null )
                response = client.execute( new HttpGet( params == null || params.size() == 0 ? url : url + "?" + URLEncodedUtils.format(params, "utf-8") ) );
            else {
                HttpContext mHttpContext = new BasicHttpContext();
                mHttpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
                response = client.execute( new HttpGet( params == null || params.size() == 0 ? url : url + "?" + URLEncodedUtils.format(params, "utf-8") ) , mHttpContext );
            }
            HttpEntity result = response.getEntity();
            if( result != null ) {
                InputStream mInputStream = result.getContent();
                String out = getStringFromInputStream(mInputStream);
                mInputStream.close();
                return out;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    String post_url_contents( String url, List<NameValuePair> params , CookieStore cookieStore ) {
        try {
            HttpClient client = MySSLSocketFactory.createMyHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
            HttpPost mHttpPost = new HttpPost(url);
            HttpResponse response = null;

            if( params != null && params.size() > 0 )
                mHttpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            if( cookieStore == null )
                response = client.execute( mHttpPost );
            else {
                HttpContext mHttpContext = new BasicHttpContext();
                mHttpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
                response = client.execute( mHttpPost , mHttpContext );
            }
            HttpEntity result = response.getEntity();
            if( result != null ) {
                InputStream mInputStream = result.getContent();
                String out = getStringFromInputStream(mInputStream);
                mInputStream.close();
                return out;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    String getStringFromInputStream(InputStream in) {
        byte []data = new byte[1024];
        int length;
        if( in == null )
            return null;
        ByteArrayOutputStream mByteArrayOutputStream = new ByteArrayOutputStream();
        try {
            while( (length = in.read(data)) != -1 )
                mByteArrayOutputStream.write(data, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(mByteArrayOutputStream.toByteArray());
    }

    private Handler OrderHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what)
            {
                case -1:
                    layout.setRefreshing(true);
                    break;
                case 1:
                    OrderListAdapter = new ExampleAdapter(MainActivity.this);
                    OrderListAdapter.setData(items);
                    mList.setAdapter(OrderListAdapter);
                    break;
                case 2:
                    layout.setRefreshing(false);
                    break;
            }
        };
    };

    private Handler PincodeHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            AlertDialogPro.Builder builder = new AlertDialogPro.Builder(MainActivity.this);
            switch (msg.what)
            {
                case -1:
                    LoadingDialog.dismiss();
                    builder.setTitle("Error").
                            setMessage("系統繁忙請自行取得PinCode").
                            setIcon(R.drawable.ic_error_red_48dp).
                            setPositiveButton("確定", null).show();
                    break;
                default:
                    LoadingDialog.dismiss();
                    builder.setTitle("MI Order").
                            setIcon(R.drawable.ic_payment_white_48dp).
                            setMessage((String) msg.obj).
                            setPositiveButton("確定", null).show();
                    break;
            }
        };
    };

    private Handler CancleHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            AlertDialogPro.Builder builder = new AlertDialogPro.Builder(MainActivity.this);
            switch (msg.what)
            {
                case -1:
                    LoadingDialog.dismiss();
                    builder.setTitle("Error").
                            setMessage("取消訂單失敗 , 請稍後再試 !").
                            setIcon(R.drawable.ic_error_red_48dp).
                            setPositiveButton("確定", null).show();
                    break;
                case 1:
                    LoadingDialog.dismiss();
                    builder.setTitle("MI Order").
                            setIcon(R.drawable.ic_launcher).
                            setMessage("取消訂單成功 !").
                            setPositiveButton("確定", null).show();
                    new Thread(OrderRunnable).start();
                    break;
            }
        };
    };

    private Handler LoginHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
           switch (msg.what)
           {
               case -1:
                   LoadingDialog.setMessage("Loading...");
                   ProgressDialogPro progressDialog = (ProgressDialogPro) LoadingDialog;
                   progressDialog.setProgressStyle(ProgressDialogPro.STYLE_SPINNER);
                   progressDialog.setIndeterminate(true);
                   LoadingDialog.setCancelable(false);
                   LoadingDialog.setCanceledOnTouchOutside(false);
                   LoadingDialog.show();
                   break;
               case 1:
                   LoadingDialog.dismiss();
                   AlertDialogPro.Builder builder = new AlertDialogPro.Builder(MainActivity.this);
                   builder.setTitle("Error").
                           setMessage("帳號或密碼輸入錯誤 !").
                           setIcon(R.drawable.ic_error_red_48dp).
                           setPositiveButton("確定", null).show();
                   SignInButton.setEnabled(true);
                   UserNameEditText.setEnabled(true);
                   PasswordEditText.setEnabled(true);
                   break;
               case 2:
                   NowLayout = "Order";
                   LoadingDialog.dismiss();
                   onCreate(null);
                   break;
           }
        };
    };

    private void restorePrefs() {
        SharedPreferences setting = getSharedPreferences("MI Order", 0);
        String username = setting.getString("User", "");
        String password = setting.getString("Pwd", "");
        UserNameEditText.setText(username);
        PasswordEditText.setText(password);
    }
    private void savePrefs() {
        SharedPreferences setting = getSharedPreferences("MI Order", 0);
        setting.edit()
                .putString("User", UserNameEditText.getText().toString())
                .putString("Pwd", PasswordEditText.getText().toString())
                .apply();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 什麼都不用寫
        }
        else {
            // 什麼都不用寫
        }
    }
}