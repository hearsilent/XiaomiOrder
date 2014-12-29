package silent.miorder;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.alertdialogpro.ProgressDialogPro;
import com.cengalabs.flatui.views.FlatButton;
import com.ikimuhendis.ldrawer.ActionBarDrawerToggle;
import com.ikimuhendis.ldrawer.DrawerArrowDrawable;
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

import us.codecraft.xsoup.Xsoup;

public class MainActivity extends Activity implements PullToRefreshView.OnPullToRefreshListener {
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
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerArrowDrawable drawerArrow;

    private MaterialEditText UserNameEditText;
    private MaterialEditText PasswordEditText;
    private FlatButton SignInButton;

    Runnable OrderRunnable;
    Runnable LoginRunnable;

    AlertDialog LoadingDialog;

    protected PullToRefreshView.Attacher mAttacher;
    private AnimatedExpandableListView mList;
    private ExampleAdapter OrderListAdapter;

    String[][] Detail = new String[][]{};
    int[][] OrderDetailCount = new int[][]{};
    String[][] OrderDetail = new String[][]{};
    List<GroupItem> items = new ArrayList<>();
    int OldPosition = -1;
    String NowLayout = "Main";
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

        LoginRunnable = new Runnable() {
            @Override
            public void run() {
                try {
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

                    String _Payment = null, _Tcat = null, _Status = null, _Xiaomi = null;
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
                        if (!PageUrl.equals(_orderUrl))
                            document = Jsoup.parse(get_url_contents(PageUrl, null, cookieStore));
                        List<String> list = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li").evaluate(document).list();
                        for (int i = 1; i <= list.size(); i++)
                        {
                            if (Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li[" + i + "]/table/thead/tr/th/div/span[1]/a").evaluate(document).get() == null)
                                continue;
                            _Xiaomi = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div/div/ul/li[" + i + "]/table/thead/tr/th/div/span[1]/a").evaluate(document).getElements().text();
                            Document documentx = Jsoup.parse(get_url_contents(_orderView + _Xiaomi, null, cookieStore));
                            if (Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[3]/table/tbody/tr[2]").evaluate(documentx).get() != null)
                                _Tcat = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[3]/table/tbody/tr[2]/td").evaluate(documentx).getElements().text();
                            OrderItemCounter ++;
                            Detail[OrderItemCounter][0] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[1]").evaluate(documentx).getElements().text();
                            Detail[OrderItemCounter][1] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[2]").evaluate(documentx).getElements().text().split(" ")[ Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[2]").evaluate(documentx).getElements().text().split(" ").length - 1];
                            Detail[OrderItemCounter][2] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[3]").evaluate(documentx).getElements().text();
                            OrderDetailCount[OrderItemCounter][0] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li").evaluate(documentx).list().size();
                            for (int j = 1; j <= Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li").evaluate(documentx).list().size(); j++)
                                OrderDetail[OrderItemCounter][j - 1] = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li[" + j + "]/a[2]").evaluate(documentx).getElements().text();
                            _Payment = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[3]/div/span/span").evaluate(documentx).getElements().text();
                            _Status = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[4]/div/span").evaluate(documentx).getElements().text();
                            GroupItem item = new GroupItem();

                            item.title = "訂單號：" + _Xiaomi;
                            ChildItem child = new ChildItem();
                            child.title = "目前狀態：" + _Status;
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
                            OrderHandler.sendEmptyMessage(1);
                        }
                    }
                    OrderHandler.sendEmptyMessage(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        LoadingDialog = new ProgressDialogPro(this);
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

                LoadingDialog.setMessage("Loading...");
                ((ProgressDialogPro) LoadingDialog).setIndeterminate(true);
                ((ProgressDialogPro) LoadingDialog).setProgressStyle(ProgressDialogPro.STYLE_SPINNER);
                LoadingDialog.setCancelable(false);
                LoadingDialog.setCanceledOnTouchOutside(false);
                LoadingDialog.show();
            }
        });

        restorePrefs();
    }

    void InitOrder()
    {
        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        mList = (AnimatedExpandableListView) super.findViewById(R.id.OrderList);
        mAttacher = new PullToRefreshView.Attacher(mList);
        mAttacher.setOnPullToRefreshListener(this);
        mAttacher.getHeaderTextView().setTextColor(Color.WHITE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navdrawer);
        drawerArrow = new DrawerArrowDrawable(this) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                drawerArrow, R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        String[] values = new String[]{ "Sign out", "About" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                switch (position) {
                    case 0:
                        break;
//                    Intent share = new Intent(Intent.ACTION_SEND);
//                    share.setType("text/plain");
//                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    share.putExtra(Intent.EXTRA_SUBJECT,
//                            getString(R.string.app_name));
//                    share.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_description) + "\n" +
//                            "GitHub Page : https://github.com/IkiMuhendis/LDrawer\n" +
//                            "Sample App : https://play.google.com/store/apps/details?id=" +
//                            getPackageName());
//                    startActivity(Intent.createChooser(share,
//                            getString(R.string.app_name)));
                }
            }
        });

        mList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                if (mAttacher.isRefreshing())
                    return true;
                switch (childPosition)
                {
                    case 0:
                        if (OrderListAdapter.getChild(groupPosition, childPosition).title.length() != 9)
                        {
                            new AlertDialogPro.Builder(MainActivity.this)
                                    .setTitle("繳費")
                                    .setSingleChoiceItems(new String[]{"7-11繳費", "全家繳費"},
                                            0,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            })
                                    .setPositiveButton("確定", null)
                                    .show();
                        }
                        break;
                    case 2:
                        if (!OrderListAdapter.getChild(groupPosition, childPosition).title.equals("運輸單號："))
                        {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.t-cat.com.tw/Inquire/TraceDetail.aspx?BillID=" + OrderListAdapter.getChild(groupPosition, childPosition).title.split("：")[1]));
                            startActivity(browserIntent);
                        }
                        break;
                    case 3:
                        new AlertDialogPro.Builder(MainActivity.this).setTitle("收件資料")
                                .setItems(Detail[groupPosition], null)
                                .setPositiveButton("確定", null)
                                .show();
                        break;
                    case 4:
                        String[] NewOrderDetail = Arrays.copyOf(OrderDetail[groupPosition], OrderDetailCount[groupPosition][0]);
                        new AlertDialogPro.Builder(MainActivity.this).setTitle("訂單明細")
                                .setItems(NewOrderDetail, null)
                                .setPositiveButton("確定",null)
                                .show();
                        break;
                }
                return false;
            }
        });

        mList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // We call collapseGroupWithAnimation(int) and
                // expandGroupWithAnimation(int) to animate group
                // expansion/collapse.
                if (OldPosition != -1)
                    mList.collapseGroupWithAnimation(OldPosition);
                OldPosition = groupPosition;
                if (mAttacher.isRefreshing())
                    return true;
                if (mList.isGroupExpanded(groupPosition))
                    mList.collapseGroupWithAnimation(groupPosition);
                else
                    mList.expandGroupWithAnimation(groupPosition);
                return true;
            }
        });
    }

    @Override
    public void onRefresh () {
        new Thread(OrderRunnable).start();
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
                    mAttacher.setRefreshing();
                    break;
                case 1:
                    OrderListAdapter = new ExampleAdapter(MainActivity.this);
                    OrderListAdapter.setData(items);
                    mList.setAdapter(OrderListAdapter);
                    break;
                case 2:
                    mAttacher.setRefreshComplete();
                    break;
            }
        };
    };

    private Handler LoginHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
           switch (msg.what)
           {
               case 1:
                   LoadingDialog.dismiss();
                   AlertDialogPro.Builder builder = new AlertDialogPro.Builder(MainActivity.this);
                   builder.setTitle("Error").
                           setMessage("帳號或密碼輸入錯誤 !").
                           setIcon(R.drawable.ic_error_red_48dp).
                           setPositiveButton("確定", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   SignInButton.setEnabled(true);
                                   UserNameEditText.setEnabled(true);
                                   PasswordEditText.setEnabled(true);
                               }
                           }).show();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                mDrawerLayout.openDrawer(mDrawerList);
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (NowLayout.equals("Order"))
            mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (NowLayout.equals("Order"))
            mDrawerToggle.onConfigurationChanged(newConfig);
    }
}