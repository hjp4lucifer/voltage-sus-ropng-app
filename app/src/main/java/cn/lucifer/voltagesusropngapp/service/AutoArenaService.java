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
import cn.lucifer.voltage.sus.auto.AutoArena;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * 竞技场自动对战服务
 */
public class AutoArenaService extends Service implements IWatchingRunning {

	public static final String ARENA_BATTLE_TAG = "arena_battle_tag";

	public static final String ARENA_BATTLE_START = "arena_battle_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoArena autoArena;
	private WatchingThread watchingThread;

	/**
	 * 是否已收到停止请求。置位后业务循环在当前轮次边界退出
	 */
	private volatile boolean stopRequested = false;

	/**
	 * 停止广播接收器
	 */
	private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(LogPrinter.LOG_TAG, "AutoArenaService received stop broadcast");
			requestStop();
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

		Log.i(LogPrinter.LOG_TAG, "--------- AutoArenaService onCreate ! ");

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
		if (!ARENA_BATTLE_START.equals(intent.getStringExtra(ARENA_BATTLE_TAG))) {
			// 非法启动请求：结束自身，避免残留空转实例（WakeLock 由 onDestroy 释放）
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_ARENA.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动竞技场对战：{}正在运行", runningService));
			// 被互斥拒绝：结束自身，避免残留空转实例（WakeLock 由 onDestroy 释放）
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_ARENA);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_ARENA, null);

		if (null == autoArena) {
			autoArena = new AutoArena();
			autoArena.setRunningCallback(new AutoArena.RunningCallback() {
				@Override
				public void onDetailUpdate(String detail) {
					MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
							MainUIControl.SERVICE_ARENA, detail);
				}
			});
			watchingThread = new WatchingThread("watchingAutoArena", this);
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
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service.arena");
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
		requestStop();

		// 注销停止广播接收器
		try {
			unregisterReceiver(stopReceiver);
		} catch (Exception e) {
			Log.w(LogPrinter.LOG_TAG, "unregisterReceiver error", e);
		}

		// 清除运行标志
		AppSettings.clearRunningService(this);

		// 发送停止状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_ARENA, null);

		releaseWakeLock();
	}

	/**
	 * 请求停止当前任务：置停止标志位，并推送给业务对象
	 */
	private void requestStop() {
		stopRequested = true;
		if (autoArena != null) {
			autoArena.setRunning(false);
		}
	}

	@Override
	public void watchThreadPoolExecutor() {
		// 从 SharedPreferences 读取全局配置
		int arenaId = AppSettings.getArenaId(this);
		String raidId = AppSettings.getRaidId(this);
		String appliVersion = AppSettings.getAppliVersion(this);

		LogUtils.info(StrUtils.generateMessage("竞技场对战开始, arenaId={}, raidId={}, appliVersion={}",
				arenaId, raidId, appliVersion));

		try {
			// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
			final Context context = this;
			autoArena.setOverrideSettings(new OverrideSettingsCallback() {
				@Override
				public void overrideSettings(BaseApi api) {
					AppSettings.applyOverrideSettings(context, api);
				}
			});

			autoArena.runArena(200);
		} catch (Exception e) {
			Log.e("autoArena", "autoArena Exception!!!", e);
			LogUtils.error("autoArena Exception!!!", e);
		}

		// 无论任务如何结束，Service 都应结束自身；"已完成"日志仅在自然完成时输出
		if (!stopRequested) {
			LogUtils.info("竞技场对战已完成！");
		}
		stopSelf();
	}
}
