package cn.lucifer.voltagesusropngapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import cn.lucifer.util.LogUtils;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;
import cn.lucifer.voltagesusropngapp.service.AutoLoginService;
import cn.lucifer.voltagesusropngapp.ui.UITypeEnum;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

public class MainActivity extends AppCompatActivity {

	protected ListView listView_log;
	protected LogAdapter logAdapter;

	private BroadcastReceiver logReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			addLog(intent.getExtras().getString(LogPrinter.EXTRA_LOG_NAME));
		}
	};

	private BroadcastReceiver buttonUiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			if (null == extras) {
				return;
			}
			UITypeEnum uiTypeEnum = (UITypeEnum) extras.get(MainUIControl.EXTRA_UI_TYPE);
			if (null == uiTypeEnum) {
				return;
			}

			int id = extras.getInt(MainUIControl.EXTRA_UI_ID);
			String name = extras.getString(MainUIControl.EXTRA_UI_NAME);

			switch (uiTypeEnum) {
				case MenuItem:
					MenuItem view = findViewById(id);
					view.setTitle(name);
					break;
				default:
					break;
			}

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
				case R.id.navigation_auto_login: {
					Context context = getApplicationContext();
					Intent intent = new Intent(context, AutoLoginService.class);
					intent.putExtra(AutoLoginService.AUTO_LOGIN_TAG, AutoLoginService.AUTO_LOGIN_START);
					context.startService(intent);
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

		listView_log = findViewById(R.id.listView_log);
		logAdapter = new LogAdapter(this);
		listView_log.setAdapter(logAdapter);
		LogPrinter logPrinter = new LogPrinter(getApplicationContext());
		LogUtils.isDebugEnabled = false;
		LogUtils.info_printer = logPrinter;
		LogUtils.error_printer = logPrinter;

		IntentFilter filter = new IntentFilter();
		filter.addAction(LogPrinter.LOG_RECEIVER_ACTION);
		registerReceiver(logReceiver, filter);

		MainUIControl mainUIControl = new MainUIControl(this);
		MainUIUtils.mainUIControl = mainUIControl;

		BottomNavigationView navigation = findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
	}

}
