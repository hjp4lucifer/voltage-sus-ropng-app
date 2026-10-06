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
import cn.lucifer.voltage.sus.auto.AutoRaidGacha;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * Event 抽卡自动执行服务
 */
public class AutoRaidGachaService extends Service implements IWatchingRunning {

	public static final String RAID_GACHA_TAG = "raid_gacha_tag";

	public static final String RAID_GACHA_START = "raid_gacha_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoRaidGacha autoRaidGacha;
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
			Log.i(LogPrinter.LOG_TAG, "AutoRaidGachaService received stop broadcast");
			running = false;
			if (autoRaidGacha != null) {
				autoRaidGacha.setRunning(false);
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

		Log.i(LogPrinter.LOG_TAG, "--------- AutoRaidGachaService onCreate ! ");

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
		if (!RAID_GACHA_START.equals(intent.getStringExtra(RAID_GACHA_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_RAID_GACHA.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动 Event 抽卡：{}正在运行", runningService));
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_RAID_GACHA);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_RAID_GACHA, null);

		if (null == autoRaidGacha) {
			autoRaidGacha = new AutoRaidGacha();
			autoRaidGacha.setRunningCallback(new AutoRaidGacha.RunningCallback() {
				@Override
				public void onDetailUpdate(String detail) {
					MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
							MainUIControl.SERVICE_RAID_GACHA, detail);
				}
			});
			watchingThread = new WatchingThread("watchingAutoRaidGacha", this);
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
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service.raidGacha");
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
		if (autoRaidGacha != null) {
			autoRaidGacha.setRunning(false);
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
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_RAID_GACHA, null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		// 从 SharedPreferences 读取全局配置
		String raidId = AppSettings.getRaidId(this);
		String appliVersion = AppSettings.getAppliVersion(this);

		LogUtils.info(StrUtils.generateMessage("Event 抽卡开始, raidId={}, appliVersion={}",
				raidId, appliVersion));

		try {
			// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
			final Context context = this;
			autoRaidGacha.setOverrideSettings(new OverrideSettingsCallback() {
				@Override
				public void overrideSettings(BaseApi api) {
					AppSettings.applyOverrideSettings(context, api);
				}
			});

			autoRaidGacha.runRaidGacha();
		} catch (Exception e) {
			Log.e("autoRaidGacha", "autoRaidGacha Exception!!!", e);
			LogUtils.error("autoRaidGacha Exception!!!", e);
		}

		// 正常结束后清理
		if (running) {
			LogUtils.info("Event 抽卡已完成！");
			stopSelf();
		}
	}
}
