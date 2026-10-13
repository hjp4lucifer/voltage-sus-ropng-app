package cn.lucifer.voltagesusropngapp.service;

import android.content.Context;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoRaidClimb;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.auto.RunningCallback;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * Event 爬楼自动执行服务
 */
public class AutoRaidClimbService extends BaseAutoService {

	public static final String RAID_CLIMB_TAG = "raid_climb_tag";

	public static final String RAID_CLIMB_START = "raid_climb_start";

	private AutoRaidClimb autoRaidClimb;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_RAID_CLIMB;
	}

	@Override
	protected String startTag() {
		return RAID_CLIMB_TAG;
	}

	@Override
	protected String startValue() {
		return RAID_CLIMB_START;
	}

	@Override
	protected String taskDisplayName() {
		return "Event 爬楼";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoRaidClimb";
	}

	@Override
	protected void initTask() {
		autoRaidClimb = new AutoRaidClimb();
		autoRaidClimb.setRunningCallback(new RunningCallback() {
			@Override
			public void onDetailUpdate(String detail) {
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
						MainUIControl.SERVICE_RAID_CLIMB, detail);
			}
		});

		// 从 SharedPreferences 读取轮询延时基数，再启动监控线程，避免线程先读到默认值
		int sleepBaseMs = AppSettings.getSleepBaseMs(this);
		LogUtils.info(StrUtils.generateMessage("Event 爬楼轮询延时基数={} ms", sleepBaseMs));
		autoRaidClimb.setSleepBaseMs(sleepBaseMs);

		// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
		final Context context = this;
		autoRaidClimb.setOverrideSettings(new OverrideSettingsCallback() {
			@Override
			public void overrideSettings(BaseApi api) {
				AppSettings.applyOverrideSettings(context, api);
			}
		});
	}

	@Override
	protected void onStopRequested() {
		if (autoRaidClimb != null) {
			autoRaidClimb.setRunning(false);
		}
	}

	@Override
	protected void doWork() {
		// 从 SharedPreferences 读取全局配置
		String raidId = AppSettings.getRaidId(this);
		String appliVersion = AppSettings.getAppliVersion(this);

		LogUtils.info(StrUtils.generateMessage("Event 爬楼开始, raidId={}, appliVersion={}",
				raidId, appliVersion));

		try {
			autoRaidClimb.runRaidClimb();
		} catch (Exception e) {
			Log.e("autoRaidClimb", "autoRaidClimb Exception!!!", e);
			LogUtils.error("autoRaidClimb Exception!!!", e);
		}
	}
}
