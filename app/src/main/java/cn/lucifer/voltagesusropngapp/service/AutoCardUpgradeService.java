package cn.lucifer.voltagesusropngapp.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoCardUpgrade;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

import java.util.List;

/**
 * 卡牌自动升阶服务
 */
public class AutoCardUpgradeService extends Service implements IWatchingRunning {

	public static final String CARD_UPGRADE_TAG = "card_upgrade_tag";

	public static final String CARD_UPGRADE_START = "card_upgrade_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoCardUpgrade autoCardUpgrade;
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
			Log.i(LogPrinter.LOG_TAG, "AutoCardUpgradeService received stop broadcast");
			running = false;
			if (autoCardUpgrade != null) {
				autoCardUpgrade.setRunning(false);
			}
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

		Log.i(LogPrinter.LOG_TAG, "--------- AutoCardUpgradeService onCreate ! ");

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
		if (!CARD_UPGRADE_START.equals(intent.getStringExtra(CARD_UPGRADE_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_CARD_UPGRADE.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动卡牌升阶：{}正在运行", runningService));
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_CARD_UPGRADE);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_CARD_UPGRADE, null);

		if (null == autoCardUpgrade) {
			autoCardUpgrade = new AutoCardUpgrade();
			autoCardUpgrade.setRunningCallback(new AutoCardUpgrade.RunningCallback() {
				@Override
				public void onDetailUpdate(String detail) {
					MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
							MainUIControl.SERVICE_CARD_UPGRADE, detail);
				}
			});
			watchingThread = new WatchingThread("watchingAutoCardUpgrade", autoCardUpgrade, this);
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
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service.card_upgrade");
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
		if (autoCardUpgrade != null) {
			autoCardUpgrade.setRunning(false);
		}

		// 注销停止广播接收器
		try {
			unregisterReceiver(stopReceiver);
		} catch (Exception e) {
			Log.w(LogPrinter.LOG_TAG, "unregisterReceiver error", e);
		}

		// 清除运行标志
		AppSettings.clearRunningService(this);

		// 发送停止状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_CARD_UPGRADE, null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		// 从 SharedPreferences 读取卡牌升阶ID列表
		List<Integer> cardIdList = AppSettings.getCardUpgradeIdList(this);

		LogUtils.info(StrUtils.generateMessage("卡牌升阶开始, cardIdList={}", cardIdList));

		try {
			autoCardUpgrade.setUp();
			autoCardUpgrade.setCardIdList(cardIdList);

			// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
			final Context context = this;
			autoCardUpgrade.setOverrideSettings(new OverrideSettingsCallback() {
				@Override
				public void overrideSettings(BaseApi api) {
					AppSettings.applyOverrideSettings(context, api);
				}
			});

			autoCardUpgrade.runCardUpgrade();
			autoCardUpgrade.tearDown();
		} catch (Exception e) {
			Log.e("autoCardUpgrade", "autoCardUpgrade Exception!!!", e);
			LogUtils.error("autoCardUpgrade Exception!!!", e);
		}

		// 正常结束后清理
		if (running) {
			LogUtils.info("卡牌升阶已完成！");
			stopSelf();
		}
	}
}
