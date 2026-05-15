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
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

import java.util.LinkedList;

/**
 * 领取角色礼物服务：按角色名匹配礼物并逐个领取
 */
public class AutoPresentCharacterService extends Service {

	public static final String PRESENT_CHARACTER_TAG = "present_character_tag";

	public static final String PRESENT_CHARACTER_START = "present_character_start";

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
			Log.i(LogPrinter.LOG_TAG, "AutoPresentCharacterService received stop broadcast");
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

		LogUtils.info(StrUtils.generateMessage("领取角色礼物开始, 角色名={}", characterName));

		try {
			Integer presentId = findAndGet(characterName, 0);
			while (presentId != 0 && running) {
				presentId = findAndGet(characterName, presentId);
			}

			if (running) {
				LogUtils.info("领取角色礼物已完成！");
			}
		} catch (Exception e) {
			Log.e("presentCharacter", "领取角色礼物异常！", e);
			LogUtils.error("领取角色礼物异常！", e);
		}

		// 正常结束后清理
		if (running) {
			stopSelf();
		}
	}

	/**
	 * 查找并领取指定角色的礼物
	 *
	 * @param characterName 角色名
	 * @param presentId     起始礼物ID（0表示从头开始）
	 * @return 匹配到的角色礼物的 present_id，0 表示未找到
	 */
	private Integer findAndGet(String characterName, Integer presentId) throws Exception {
		PresentBoxApi api = new PresentBoxApi();
		api.setPresentId(presentId);
		applyOverrideSettings(api);

		String resp = requestServer(api);
		PresentBoxApiResp apiResp = api.parseResp(resp);

		if (apiResp.data == null || apiResp.data.item_list == null || apiResp.data.item_list.isEmpty()) {
			LogUtils.info("没有更多礼物");
			return 0;
		}

		// 查找匹配角色的礼物
		for (Item item : apiResp.data.item_list) {
			if (!running) {
				return 0;
			}
			if (item.name.contains(characterName)) {
				LogUtils.info(StrUtils.generateMessage("找到角色礼物: name={}, present_id={}", item.name, item.present_id));

				// 领取该礼物
				getPresent(item.present_id);
				return item.present_id;
			}
		}

		// 当前页未找到匹配角色
		LogUtils.info(StrUtils.generateMessage("当前页未找到包含\"{}\"的礼物", characterName));
		return 0;
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

		Thread.sleep(1000 + (int) (Math.random() * RANDOM_MAX_TIME));
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
