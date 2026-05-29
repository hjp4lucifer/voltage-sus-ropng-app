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
import cn.lucifer.voltage.sus.auto.AutoPresent;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * 领取角色礼物服务：按角色名匹配礼物并逐个领取
 */
public class AutoPresentCharacterService extends Service implements IWatchingRunning {

	public static final String PRESENT_CHARACTER_TAG = "present_character_tag";

	public static final String PRESENT_CHARACTER_START = "present_character_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoPresent autoPresent;
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
			Log.i(LogPrinter.LOG_TAG, "AutoPresentCharacterService received stop broadcast");
			running = false;
			if (autoPresent != null) {
				autoPresent.setRunning(false);
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

		Log.i(LogPrinter.LOG_TAG, "--------- AutoPresentCharacterService onCreate ! ");

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
		if (!PRESENT_CHARACTER_START.equals(intent.getStringExtra(PRESENT_CHARACTER_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_PRESENT_CHARACTER.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动领取角色礼物：{}正在运行", runningService));
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_PRESENT_CHARACTER);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_PRESENT_CHARACTER, null);

		if (null == autoPresent) {
			autoPresent = new AutoPresent();
			autoPresent.setRunningCallback(new AutoPresent.RunningCallback() {
				@Override
				public void onDetailUpdate(String detail) {
					MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
							MainUIControl.SERVICE_PRESENT_CHARACTER, detail);
				}
			});

			// 设置 API 配置覆盖回调
			final Context context = this;
			autoPresent.setOverrideSettings(new OverrideSettingsCallback() {
				@Override
				public void overrideSettings(BaseApi api) {
					AppSettings.applyOverrideSettings(context, api);
				}
			});

			watchingThread = new WatchingThread("watchingAutoPresentCharacter", this);
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
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service.present_character");
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
		if (autoPresent != null) {
			autoPresent.setRunning(false);
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
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_PRESENT_CHARACTER, null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		String characterName = AppSettings.getCharacterName(this);

		try {
			autoPresent.runPresentCharacter(characterName);
		} catch (Exception e) {
			Log.e("presentCharacter", "领取角色礼物异常！", e);
			LogUtils.error("领取角色礼物异常！", e);
		}

		// 正常结束后清理
		if (running) {
			stopSelf();
		}
	}
}
