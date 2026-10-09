package cn.lucifer.voltagesusropngapp.service;

import android.content.Context;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoArena;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.auto.RunningCallback;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * 竞技场自动对战服务
 */
public class AutoArenaService extends BaseAutoService {

	public static final String ARENA_BATTLE_TAG = "arena_battle_tag";

	public static final String ARENA_BATTLE_START = "arena_battle_start";

	private AutoArena autoArena;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_ARENA;
	}

	@Override
	protected String startTag() {
		return ARENA_BATTLE_TAG;
	}

	@Override
	protected String startValue() {
		return ARENA_BATTLE_START;
	}

	@Override
	protected String taskDisplayName() {
		return "竞技场对战";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoArena";
	}

	@Override
	protected void initTask() {
		autoArena = new AutoArena();
		autoArena.setRunningCallback(new RunningCallback() {
			@Override
			public void onDetailUpdate(String detail) {
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
						MainUIControl.SERVICE_ARENA, detail);
			}
		});

		// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
		final Context context = this;
		autoArena.setOverrideSettings(new OverrideSettingsCallback() {
			@Override
			public void overrideSettings(BaseApi api) {
				AppSettings.applyOverrideSettings(context, api);
			}
		});
	}

	@Override
	protected void onStopRequested() {
		if (autoArena != null) {
			autoArena.setRunning(false);
		}
	}

	@Override
	protected void doWork() {
		// 从 SharedPreferences 读取全局配置
		int arenaId = AppSettings.getArenaId(this);
		String raidId = AppSettings.getRaidId(this);
		String appliVersion = AppSettings.getAppliVersion(this);

		LogUtils.info(StrUtils.generateMessage("竞技场对战开始, arenaId={}, raidId={}, appliVersion={}",
				arenaId, raidId, appliVersion));

		try {
			autoArena.runArena(200);
		} catch (Exception e) {
			Log.e("autoArena", "autoArena Exception!!!", e);
			LogUtils.error("autoArena Exception!!!", e);
		}
	}
}
