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
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltage.sus.thread.WatchingThread;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * 功能 Service 公共基类：统一唤醒锁、停止广播、互斥校验、运行状态广播与收尾逻辑。
 * <p>
 * 子类只需提供业务差异：Service 标识、启动标识、显示名、监控线程名、任务初始化、执行主体与停止通知。
 */
public abstract class BaseAutoService extends Service implements IWatchingRunning {

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private WatchingThread watchingThread;

	/**
	 * 是否已收到停止请求。置位后业务循环在当前轮次边界退出
	 */
	private volatile boolean stopRequested = false;

	/**
	 * 任务是否已初始化，避免重复 initTask
	 */
	private boolean started = false;

	/**
	 * 停止广播接收器
	 */
	private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(LogPrinter.LOG_TAG, BaseAutoService.this.getClass().getSimpleName()
					+ " received stop broadcast");
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

		Log.i(LogPrinter.LOG_TAG, "--------- " + getClass().getSimpleName() + " onCreate ! ");

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
		if (!startValue().equals(intent.getStringExtra(startTag()))) {
			// 非法启动请求：结束自身，避免残留空转实例（WakeLock 由 onDestroy 释放）
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !serviceName().equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动{}：{}正在运行", taskDisplayName(), runningService));
			// 被互斥拒绝：结束自身，避免残留空转实例（WakeLock 由 onDestroy 释放）
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, serviceName());

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, serviceName(), null);

		if (!started) {
			started = true;
			// 先完成业务对象与配置的准备，再启动监控线程，避免线程先读到默认值
			initTask();
			watchingThread = new WatchingThread(watchingThreadName(), this);
			watchingThread.start();
		}

		return super.onStartCommand(intent, flags, startId);
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
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, serviceName(), null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		doWork();

		// 无论任务如何结束，Service 都应结束自身；"已完成"日志仅在自然完成时输出
		if (!stopRequested) {
			LogUtils.info(taskDisplayName() + "已完成！");
		}
		stopSelf();
	}

	/**
	 * 请求停止当前任务：置停止标志位，并通知业务对象
	 */
	private void requestStop() {
		stopRequested = true;
		onStopRequested();
	}

	/**
	 * 申请设备电源锁
	 */
	@SuppressLint("InvalidWakeLockTag")
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, getPackageName() + ".service." + serviceName());
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

	/**
	 * Service 业务标识（MainUIControl.SERVICE_*），同时用于运行标志与 WakeLock tag
	 */
	protected abstract String serviceName();

	/**
	 * 启动请求的标识 key
	 */
	protected abstract String startTag();

	/**
	 * 启动请求的标识 value
	 */
	protected abstract String startValue();

	/**
	 * 功能显示名，用于互斥提示与完成日志
	 */
	protected abstract String taskDisplayName();

	/**
	 * 监控线程名
	 */
	protected abstract String watchingThreadName();

	/**
	 * 创建业务对象并注册回调（含 API 配置覆盖回调）
	 */
	protected abstract void initTask();

	/**
	 * 通知业务对象停止
	 */
	protected abstract void onStopRequested();

	/**
	 * 执行任务主体，需自行处理异常
	 */
	protected abstract void doWork();
}
