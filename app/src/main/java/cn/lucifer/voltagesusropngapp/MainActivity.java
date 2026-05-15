package cn.lucifer.voltagesusropngapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import cn.lucifer.util.LogUtils;
import cn.lucifer.voltagesusropngapp.fragment.FunctionFragment;
import cn.lucifer.voltagesusropngapp.fragment.SettingsFragment;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

public class MainActivity extends AppCompatActivity {

	private Fragment functionFragment;
	private Fragment settingsFragment;
	private Fragment currentFragment;

	private BottomNavigationView navigation;

	/**
	 * 状态广播接收器，用于更新停止 tab 状态
	 */
	private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateStopTabState();
		}
	};

	private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
			= new BottomNavigationView.OnNavigationItemSelectedListener() {

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.navigation_function:
					switchFragment(getFunctionFragment());
					return true;
				case R.id.navigation_stop:
					handleStopClick();
					return true;
				case R.id.navigation_settings:
					switchFragment(getSettingsFragment());
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

		// 初始化日志系统
		LogPrinter logPrinter = new LogPrinter(getApplicationContext());
		LogUtils.isDebugEnabled = false;
		LogUtils.info_printer = logPrinter;
		LogUtils.error_printer = logPrinter;

		// 初始化 UI 控制器
		MainUIControl mainUIControl = new MainUIControl(this);
		MainUIUtils.mainUIControl = mainUIControl;

		// 设置底部导航
		navigation = findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

		// 默认显示功能页
		switchFragment(getFunctionFragment());

		// 初始化停止按钮状态
		updateStopTabState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateStopTabState();

		// 注册状态广播
		IntentFilter statusFilter = new IntentFilter();
		statusFilter.addAction(MainUIControl.STATUS_RECEIVER_ACTION);
		registerReceiver(statusReceiver, statusFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(statusReceiver);
		} catch (Exception e) {
			Log.w("MainActivity", "unregisterReceiver error", e);
		}
	}

	/**
	 * 获取功能页 Fragment
	 */
	private Fragment getFunctionFragment() {
		if (functionFragment == null) {
			functionFragment = new FunctionFragment();
		}
		return functionFragment;
	}

	/**
	 * 获取设置页 Fragment
	 */
	private Fragment getSettingsFragment() {
		if (settingsFragment == null) {
			settingsFragment = new SettingsFragment();
		}
		return settingsFragment;
	}

	/**
	 * 切换 Fragment
	 */
	private void switchFragment(Fragment target) {
		if (currentFragment == target) {
			return;
		}
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();

		if (currentFragment != null) {
			transaction.hide(currentFragment);
		}

		if (target.isAdded()) {
			transaction.show(target);
		} else {
			transaction.add(R.id.fragment_container, target);
		}

		transaction.commit();
		currentFragment = target;
	}

	/**
	 * 处理停止按钮点击
	 */
	private void handleStopClick() {
		String runningService = AppSettings.getRunningService(this);
		if (runningService == null) {
			// 空闲态，不响应
			return;
		}
		// 发送停止广播
		MainUIUtils.sendStop();
	}

	/**
	 * 更新停止 tab 的可用状态
	 */
	public void updateStopTabState() {
		String runningService = AppSettings.getRunningService(this);
		MenuItem stopItem = navigation.getMenu().findItem(R.id.navigation_stop);
		if (stopItem != null) {
			stopItem.setEnabled(runningService != null);
		}
	}
}
