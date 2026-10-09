package cn.lucifer.voltagesusropngapp.service;

import android.content.Context;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoRaidGacha;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.auto.RunningCallback;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * Event 抽卡自动执行服务
 */
public class AutoRaidGachaService extends BaseAutoService {

	public static final String RAID_GACHA_TAG = "raid_gacha_tag";

	public static final String RAID_GACHA_START = "raid_gacha_start";

	private AutoRaidGacha autoRaidGacha;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_RAID_GACHA;
	}

	@Override
	protected String startTag() {
		return RAID_GACHA_TAG;
	}

	@Override
	protected String startValue() {
		return RAID_GACHA_START;
	}

	@Override
	protected String taskDisplayName() {
		return "Event 抽卡";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoRaidGacha";
	}

	@Override
	protected void initTask() {
		autoRaidGacha = new AutoRaidGacha();
		autoRaidGacha.setRunningCallback(new RunningCallback() {
			@Override
			public void onDetailUpdate(String detail) {
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
						MainUIControl.SERVICE_RAID_GACHA, detail);
			}
		});

		// 从 SharedPreferences 读取轮间延时基数，再启动监控线程，避免线程先读到默认值
		int sleepBaseMs = AppSettings.getSleepBaseMs(this);
		LogUtils.info(StrUtils.generateMessage("Event 抽卡轮间延时基数={} ms", sleepBaseMs));
		autoRaidGacha.setSleepBaseMs(sleepBaseMs);

		// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
		final Context context = this;
		autoRaidGacha.setOverrideSettings(new OverrideSettingsCallback() {
			@Override
			public void overrideSettings(BaseApi api) {
				AppSettings.applyOverrideSettings(context, api);
			}
		});
	}

	@Override
	protected void onStopRequested() {
		if (autoRaidGacha != null) {
			autoRaidGacha.setRunning(false);
		}
	}

	@Override
	protected void doWork() {
		// 从 SharedPreferences 读取全局配置
		String raidId = AppSettings.getRaidId(this);
		String appliVersion = AppSettings.getAppliVersion(this);

		LogUtils.info(StrUtils.generateMessage("Event 抽卡开始, raidId={}, appliVersion={}",
				raidId, appliVersion));

		try {
			autoRaidGacha.runRaidGacha();
		} catch (Exception e) {
			Log.e("autoRaidGacha", "autoRaidGacha Exception!!!", e);
			LogUtils.error("autoRaidGacha Exception!!!", e);
		}
	}
}
