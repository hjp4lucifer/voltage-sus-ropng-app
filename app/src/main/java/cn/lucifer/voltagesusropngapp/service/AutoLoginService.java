package cn.lucifer.voltagesusropngapp.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.auto.AutoLogin;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

public class AutoLoginService extends Service implements IWatchingRunning {

	public static final String AUTO_LOGIN_TAG = "auto_login_tag";

	public static final String AUTO_LOGIN_START = "auto_login_tag_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoLogin autoLogin;
	private WatchingThread watchingThread;

	/**
	 * 运行标志位，用于优雅停止
	 */
	private volatile boolean running = true;

	/**
	 * 停止广播接收器
	 */
	private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(LogPrinter.LOG_TAG, "AutoLoginService received stop broadcast");
			running = false;
		}
	};

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(LogPrinter.LOG_TAG, "--------- AutoLoginService onCreate ! ");

		acquireWakeLock();

		// 注册停止广播接收器
		IntentFilter filter = new IntentFilter();
		filter.addAction(MainUIControl.STOP_RECEIVER_ACTION);
		registerReceiver(stopReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		if (!AUTO_LOGIN_START.equals(bundle.getString(AUTO_LOGIN_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_AUTO_LOGIN.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动自动登录：{}正在运行", runningService));
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_AUTO_LOGIN);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_AUTO_LOGIN, null);

		if (null == autoLogin) {
			autoLogin = new AutoLogin(15);

			// 从 SharedPreferences 读取配置并设置覆盖回调
			// 注意：AutoLogin 为多账号批量场景，仅覆盖业务参数(appliVersion)，
			// 不覆盖身份字段(nsid/puKey等)，避免所有账号变成同一身份
			final String appliVersion = AppSettings.getAppliVersion(this);
			autoLogin.setOverrideSettings(new OverrideSettingsCallback() {
				@Override
				public void overrideSettings(cn.lucifer.voltage.sus.api.BaseApi api) {
					api.setAppliVersion(appliVersion);
				}
			});

			watchingThread = new WatchingThread("watchingAutoLogin", autoLogin, this);
			watchingThread.start();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 申请设备电源锁
	 */
	@SuppressLint("InvalidWakeLockTag")
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service");
			if (null != mWakeLock) {
				try {
					mWakeLock.acquire();
					Log.i(LogPrinter.LOG_TAG, "mWakeLock acquire! =================");
				} catch (SecurityException e) {
					Log.w(LogPrinter.LOG_TAG, "mWakeLock acquire failed, no WAKE_LOCK permission", e);
					mWakeLock = null;
				}
			}
		}
	}

	/**
	 * onDestroy时，释放设备电源锁
	 */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			Log.i(LogPrinter.LOG_TAG, "mWakeLock release! =================");
		}
		mWakeLock = null;
	}

	@Override
	public void onDestroy() {
		running = false;

		// 注销停止广播接收器
		try {
			unregisterReceiver(stopReceiver);
		} catch (Exception e) {
			Log.w(LogPrinter.LOG_TAG, "unregisterReceiver error", e);
		}

		// 清除运行标志
		AppSettings.clearRunningService(this);

		// 发送停止状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_AUTO_LOGIN, null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		try {
			autoLogin.setUp();
			autoLogin.autoLogin();
			autoLogin.tearDown();
		} catch (Exception e) {
			Log.e("autoLogin", "autoLogin Exception!!!", e);
			LogUtils.error("autoLogin Exception!!!", e);
		}

		int retryCount = 0;
		while (running) {
			try {
				if (!running) {
					break;
				}

				if (CollectionUtils.isEmpty(autoLogin.errorSet)) {
					LogUtils.info(StrUtils.generateMessage(
							"autoLogin 顺利完成～～～～ retryCount={} ～～～～",
							retryCount)
					);
					return;
				}

				retryCount++;

				Set<String> errorSetBak = autoLogin.errorSet;

				LogUtils.error(StrUtils.generateMessage(
						"autoLogin 有失败的线程，准备执行第{}次的重试！！！ 需要重试的playerCount={}",
						retryCount, errorSetBak.size()),
						null);

				// 更新运行详情
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_AUTO_LOGIN,
						StrUtils.generateMessage("重试第{}次", retryCount));

				autoLogin.supperSetUp();
				autoLogin.playerPropList = Lists.newArrayList(errorSetBak);
				autoLogin.autoLogin();
				autoLogin.tearDown();
			} catch (Exception e) {
				Log.e("autoLogin", "autoLogin Exception!!!", e);
				LogUtils.error("autoLogin Exception!!!", e);
			}
		}

		if (!running) {
			LogUtils.info("自动登录已停止！");
			stopSelf();
		}
	}
}
