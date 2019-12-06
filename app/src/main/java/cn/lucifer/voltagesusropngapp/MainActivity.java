package cn.lucifer.voltagesusropngapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import cn.lucifer.util.LogUtils;
import cn.lucifer.voltage.sus.auto.AutoLogin;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;

public class MainActivity extends AppCompatActivity {

	protected ListView listView_log;
	protected LogAdapter logAdapter;
	protected AutoLogin autoLogin;

	private BroadcastReceiver logReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			addLog(intent.getExtras().getString(LogPrinter.EXTRA_LOG_NAME));
		}

	};

	protected void addLog(String text) {
		logAdapter.addFirst(text);
		// logAdapter.notifyDataSetChanged();//数据发生变化, 刷新
	}

	private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
			= new BottomNavigationView.OnNavigationItemSelectedListener() {

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.navigation_home:
					logAdapter.addFirst(getString(R.string.title_home));
					return true;
				case R.id.navigation_dashboard: {
					//logAdapter.addFirst(getString(R.string.action_auto_login));
					if (null == autoLogin) {
						autoLogin = new AutoLogin(5);
						try {
							autoLogin.setUp();
							autoLogin.autoLogin();
							autoLogin.tearDown();
						} catch (Exception e) {
							Log.e("autoLogin", "autoLogin Exception!!!", e);
							LogUtils.error("autoLogin Exception!!!", e);
						}
					}
					return true;
				}
				case R.id.navigation_notifications:
					logAdapter.addFirst(getString(R.string.title_notifications));
					return true;
				default:
					break;
			}
			return false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		listView_log = (ListView) findViewById(R.id.listView_log);
		logAdapter = new LogAdapter(this);
		listView_log.setAdapter(logAdapter);
		LogPrinter logPrinter = new LogPrinter(getApplicationContext());
		LogUtils.isDebugEnabled = false;
		LogUtils.info_printer = logPrinter;
		LogUtils.error_printer = logPrinter;

		IntentFilter filter = new IntentFilter();
		filter.addAction(LogPrinter.LOG_RECEIVER_ACTION);
		registerReceiver(logReceiver, filter);

		BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
	}

}
