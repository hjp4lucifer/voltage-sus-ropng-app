package cn.lucifer.voltagesusropngapp.service;

import android.content.Context;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoPresent;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.auto.RunningCallback;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

/**
 * 领取角色礼物服务：按角色名匹配礼物并逐个领取
 */
public class AutoPresentCharacterService extends BaseAutoService {

	public static final String PRESENT_CHARACTER_TAG = "present_character_tag";

	public static final String PRESENT_CHARACTER_START = "present_character_start";

	private AutoPresent autoPresent;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_PRESENT_CHARACTER;
	}

	@Override
	protected String startTag() {
		return PRESENT_CHARACTER_TAG;
	}

	@Override
	protected String startValue() {
		return PRESENT_CHARACTER_START;
	}

	@Override
	protected String taskDisplayName() {
		return "领取角色礼物";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoPresentCharacter";
	}

	@Override
	protected void initTask() {
		autoPresent = new AutoPresent();
		autoPresent.setRunningCallback(new RunningCallback() {
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
	}

	@Override
	protected void onStopRequested() {
		if (autoPresent != null) {
			autoPresent.setRunning(false);
		}
	}

	@Override
	protected void doWork() {
		String characterName = AppSettings.getCharacterName(this);

		try {
			autoPresent.runPresentCharacter(characterName);
		} catch (Exception e) {
			Log.e("presentCharacter", "领取角色礼物异常！", e);
			LogUtils.error("领取角色礼物异常！", e);
		}
	}
}
