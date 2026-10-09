package cn.lucifer.voltagesusropngapp.service;

import android.content.Context;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.util.StrUtils;
import cn.lucifer.voltage.sus.api.BaseApi;
import cn.lucifer.voltage.sus.auto.AutoCardUpgrade;
import cn.lucifer.voltage.sus.auto.OverrideSettingsCallback;
import cn.lucifer.voltage.sus.auto.RunningCallback;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.MainUIUtils;

import java.util.List;

/**
 * 卡牌自动升阶服务
 */
public class AutoCardUpgradeService extends BaseAutoService {

	public static final String CARD_UPGRADE_TAG = "card_upgrade_tag";

	public static final String CARD_UPGRADE_START = "card_upgrade_start";

	private AutoCardUpgrade autoCardUpgrade;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_CARD_UPGRADE;
	}

	@Override
	protected String startTag() {
		return CARD_UPGRADE_TAG;
	}

	@Override
	protected String startValue() {
		return CARD_UPGRADE_START;
	}

	@Override
	protected String taskDisplayName() {
		return "卡牌升阶";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoCardUpgrade";
	}

	@Override
	protected void initTask() {
		autoCardUpgrade = new AutoCardUpgrade();
		autoCardUpgrade.setRunningCallback(new RunningCallback() {
			@Override
			public void onDetailUpdate(String detail) {
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
						MainUIControl.SERVICE_CARD_UPGRADE, detail);
			}
		});

		// 设置 API 配置覆盖回调（使用公共方法，含身份字段覆盖）
		final Context context = this;
		autoCardUpgrade.setOverrideSettings(new OverrideSettingsCallback() {
			@Override
			public void overrideSettings(BaseApi api) {
				AppSettings.applyOverrideSettings(context, api);
			}
		});
	}

	@Override
	protected void onStopRequested() {
		if (autoCardUpgrade != null) {
			autoCardUpgrade.setRunning(false);
		}
	}

	@Override
	protected void doWork() {
		// 从 SharedPreferences 读取卡牌升阶ID列表
		List<Integer> cardIdList = AppSettings.getCardUpgradeIdList(this);

		LogUtils.info(StrUtils.generateMessage("卡牌升阶开始, cardIdList={}", cardIdList));

		try {
			autoCardUpgrade.setCardIdList(cardIdList);
			autoCardUpgrade.runCardUpgrade();
		} catch (Exception e) {
			Log.e("autoCardUpgrade", "autoCardUpgrade Exception!!!", e);
			LogUtils.error("autoCardUpgrade Exception!!!", e);
		}
	}
}
