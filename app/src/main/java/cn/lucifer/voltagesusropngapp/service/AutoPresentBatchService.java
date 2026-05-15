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
import cn.lucifer.util.HttpClient5Helper;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.api.present.PresentBoxApi;
import cn.lucifer.voltage.sus.api.present.PresentBoxGetApi;
import cn.lucifer.voltage.sus.resp.present.Item;
import cn.lucifer.voltage.sus.resp.present.PresentBoxApiResp;
import cn.lucifer.voltage.sus.thread.IWatchingRunning;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * 批量领取礼物服务：按名字排除过滤，逐个领取礼物
 */
public class AutoPresentBatchService extends Service implements IWatchingRunning {

	public static final String PRESENT_BATCH_TAG = "present_batch_tag";

	public static final String PRESENT_BATCH_START = "present_batch_start";

	private static final int RANDOM_MAX_TIME = 0;

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

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
			Log.i(LogPrinter.LOG_TAG, "AutoPresentBatchService received stop broadcast");
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

		Log.i(LogPrinter.LOG_TAG, "--------- AutoPresentBatchService onCreate ! ");

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
		if (!PRESENT_BATCH_START.equals(intent.getStringExtra(PRESENT_BATCH_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		// 检查互斥：是否有其他 Service 正在运行
		String runningService = AppSettings.getRunningService(this);
		if (runningService != null && !MainUIControl.SERVICE_PRESENT_BATCH.equals(runningService)) {
			LogUtils.info(StrUtils.generateMessage("无法启动批量领取礼物：{}正在运行", runningService));
			return super.onStartCommand(intent, flags, startId);
		}

		// 设置运行标志
		AppSettings.setRunningService(this, MainUIControl.SERVICE_PRESENT_BATCH);

		// 发送运行状态广播
		MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING, MainUIControl.SERVICE_PRESENT_BATCH, null);

		// 在新线程中执行
		new Thread(new Runnable() {
			@Override
			public void run() {
				watchThreadPoolExecutor();
			}
		}).start();

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
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service.present_batch");
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
		MainUIUtils.sendStatus(MainUIControl.STATUS_STOPPED, MainUIControl.SERVICE_PRESENT_BATCH, null);

		releaseWakeLock();
	}

	@Override
	public void watchThreadPoolExecutor() {
		String excludeName = AppSettings.getExcludeName(this);
		int maxCount = AppSettings.getMaxCount(this);

		LogUtils.info(StrUtils.generateMessage("批量领取礼物开始, 排除名称={}, 最大次数={}", excludeName, maxCount));

		try {
			PresentBoxApi api = new PresentBoxApi();
			api.setPresentId(0);
			applyOverrideSettings(api);

			for (int i = 0; i < maxCount; i++) {
				if (!running) {
					break;
				}

				String resp = requestServer(api);
				PresentBoxApiResp apiResp = api.parseResp(resp);

				Item item = findItem(apiResp, excludeName);
				if (item == null) {
					LogUtils.info("没有找到合适的礼物");
					break;
				}

				api.setPresentId(item.present_id);

				LogUtils.info(StrUtils.generateMessage("i={}, name={}, send_date={}", i, item.name, item.send_date));

				// 领取该礼物
				getPresent(item.present_id);

				Thread.sleep(1000 + (int) (Math.random() * RANDOM_MAX_TIME));
			}

			if (running) {
				LogUtils.info("批量领取礼物已完成！");
			}
		} catch (Exception e) {
			Log.e("presentBatch", "批量领取礼物异常！", e);
			LogUtils.error("批量领取礼物异常！", e);
		}

		// 正常结束后清理
		if (running) {
			stopSelf();
		}
	}

	/**
	 * 在礼物列表中查找符合条件的礼物（排除包含排除名称的）
	 *
	 * @param apiResp    响应数据
	 * @param excludeName 排除名称，为空时不排除
	 * @return 符合条件的礼物，null 表示没有合适的礼物
	 */
	private Item findItem(PresentBoxApiResp apiResp, String excludeName) {
		if (apiResp.data == null || apiResp.data.item_list == null || apiResp.data.item_list.isEmpty()) {
			return null;
		}

		List<Item> itemList = apiResp.data.item_list;
		for (Item item : itemList) {
			if (excludeName != null && !excludeName.isEmpty() && item.name.contains(excludeName)) {
				continue;
			}
			return item;
		}
		return null;
	}

	/**
	 * 领取指定礼物
	 */
	private void getPresent(int presentId) throws Exception {
		PresentBoxGetApi getApi = new PresentBoxGetApi();
		LinkedList<Integer> presentIdList = new LinkedList<>();
		presentIdList.add(presentId);
		getApi.setPresentIdList(presentIdList);
		applyOverrideSettings(getApi);

		requestServer(getApi);
		LogUtils.info(StrUtils.generateMessage("领取礼物完成, present_id={}", presentId));
	}

	/**
	 * 应用覆盖配置到 API
	 */
	private void applyOverrideSettings(BaseApi api) {
		AppSettings.applyOverrideSettings(this, api);
	}

	/**
	 * 发送请求到服务器
	 */
	private String requestServer(BaseApi api) throws Exception {
		String url = api.buildUrl();
		LogUtils.info(StrUtils.generateMessage("[POST] url={}", url));

		byte[] respData = HttpClient5Helper.httpPost(url, api.buildRequestBody(), api.generateDefaultRequestHeads());
		String respStr = new String(respData);

		if (api.ignoreResult) {
			return respStr;
		}

		return respStr;
	}
}
