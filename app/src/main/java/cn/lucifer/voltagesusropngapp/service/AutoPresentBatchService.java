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
 * 批量领取礼物服务：按名字排除过滤，逐个领取礼物
 */
public class AutoPresentBatchService extends BaseAutoService {

	public static final String PRESENT_BATCH_TAG = "present_batch_tag";

	public static final String PRESENT_BATCH_START = "present_batch_start";

	private AutoPresent autoPresent;

	@Override
	protected String serviceName() {
		return MainUIControl.SERVICE_PRESENT_BATCH;
	}

	@Override
	protected String startTag() {
		return PRESENT_BATCH_TAG;
	}

	@Override
	protected String startValue() {
		return PRESENT_BATCH_START;
	}

	@Override
	protected String taskDisplayName() {
		return "批量领取礼物";
	}

	@Override
	protected String watchingThreadName() {
		return "watchingAutoPresentBatch";
	}

	@Override
	protected void initTask() {
		autoPresent = new AutoPresent();
		autoPresent.setRunningCallback(new RunningCallback() {
			@Override
			public void onDetailUpdate(String detail) {
				MainUIUtils.sendStatus(MainUIControl.STATUS_RUNNING,
						MainUIControl.SERVICE_PRESENT_BATCH, detail);
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
		String excludeName = AppSettings.getExcludeName(this);
		int maxCount = AppSettings.getMaxCount(this);

		try {
			autoPresent.runPresentBatch(excludeName, maxCount);
		} catch (Exception e) {
			Log.e("presentBatch", "批量领取礼物异常！", e);
			LogUtils.error("批量领取礼物异常！", e);
		}
	}
}
