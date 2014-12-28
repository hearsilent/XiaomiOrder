package silent.miorder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
import java.util.LinkedList;
import java.util.List;

import us.codecraft.xsoup.Xsoup;


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
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerArrowDrawable drawerArrow;

    private MaterialEditText UserNameEditText;
    private MaterialEditText PasswordEditText;
    private FlatButton SignInButton;

    Thread LoginThread;
    Thread OrderThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        SignInButton = (FlatButton) findViewById(R.id.SignIn);
        UserNameEditText = (MaterialEditText) findViewById(R.id.UserName);
        PasswordEditText = (MaterialEditText) findViewById(R.id.Password);

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
        String[] values = new String[]{
                "Stop Animation (Back icon)",
                "Stop Animation (Home icon)",
                "Start Animation",
                "Change Color",
                "GitHub Page",
                "Share",
                "Rate"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                switch (position) {
                    case 0:
                        mDrawerToggle.setAnimateEnabled(false);
                        drawerArrow.setProgress(1f);
                        break;
                    case 1:
                        mDrawerToggle.setAnimateEnabled(false);
                        drawerArrow.setProgress(0f);
                        break;
                    case 2:
                        mDrawerToggle.setAnimateEnabled(true);
                        mDrawerToggle.syncState();
                        break;
                    case 3:
                        break;
                    case 4:
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/IkiMuhendis/LDrawer"));
                        startActivity(browserIntent);
                        break;
                    case 5:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        share.putExtra(Intent.EXTRA_SUBJECT,
                                getString(R.string.app_name));
                        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_description) + "\n" +
                                "GitHub Page : https://github.com/IkiMuhendis/LDrawer\n" +
                                "Sample App : https://play.google.com/store/apps/details?id=" +
                                getPackageName());
                        startActivity(Intent.createChooser(share,
                                getString(R.string.app_name)));
                        break;
                    case 6:
                        String appUrl = "https://play.google.com/store/apps/details?id=" + getPackageName();
                        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl));
                        startActivity(rateIntent);
                        break;
                }
            }
        });

        LoginThread = new Thread(new Runnable(){
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
                        Log.d("Error", "帳號或密碼輸入錯誤 !");
                    get_url_contents(_loginUrl, null, cookieStore);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        OrderThread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    String _Payment = null, _Tcat = null, _MiStatus = null, _Place = null, _Status = null, _Xiaomi = null, _Detail = null, _OrderDetail = null;
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
                                _Tcat = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[3]/table/tbody/tr[2]").evaluate(documentx).getElements().text();
                            _Detail = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[1]/td").evaluate(documentx).getElements().text() + "/";
                            _Detail += Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[2]/td").evaluate(documentx).getElements().text() + "/";
                            _Detail += Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[3]/td").evaluate(documentx).getElements().text();
                            for (int j = 1; j <= Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[2]/div[1]/table/tbody/tr[3]/td").evaluate(documentx).list().size(); j++)
                                _OrderDetail += Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[1]/ul/li[" + j + "]/a[2]").evaluate(documentx).getElements().text() + "|";
                            _Payment = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[3]/div/span/span").evaluate(documentx).getElements().text();
                            _Status = Xsoup.compile("/html/body/div[4]/div/div/div[2]/div/div[2]/div[1]/table/tbody/tr[1]/td[4]/div/span").evaluate(documentx).getElements().text();
                            System.out.println((_Status));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        SignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginThread.start();
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(UserNameEditText.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(PasswordEditText.getWindowToken(), 0);
                SignInButton.setEnabled(false);
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
            //e.printStackTrace();
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
            //e.printStackTrace();
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
            //e.printStackTrace();
        }
        return new String(mByteArrayOutputStream.toByteArray());
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
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}