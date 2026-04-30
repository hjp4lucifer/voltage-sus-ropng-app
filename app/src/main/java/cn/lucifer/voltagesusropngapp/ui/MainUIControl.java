package cn.lucifer.voltagesusropngapp.ui;

import android.content.Context;
import android.content.Intent;

/**
 * MainActivity 的UI 控制器
 */
public class MainUIControl {
	public static final String UI_RECEIVER_ACTION = "lcf.main.ui";

	public final static String EXTRA_UI_ID = "ui.id";
	public final static String EXTRA_UI_NAME = "ui.name";
	public final static String EXTRA_UI_TYPE = "ui.type";

	/**
	 * 停止广播 Action
	 */
	public static final String STOP_RECEIVER_ACTION = "lcf.main.stop";

	/**
	 * 状态广播 Action
	 */
	public static final String STATUS_RECEIVER_ACTION = "lcf.main.status";

	public final static String EXTRA_STATUS = "status";
	public final static String EXTRA_SERVICE_NAME = "service.name";
	public final static String EXTRA_DETAIL = "status.detail";

	/**
	 * 状态值
	 */
	public static final String STATUS_RUNNING = "running";
	public static final String STATUS_STOPPED = "stopped";

	/**
	 * 服务名称
	 */
	public static final String SERVICE_AUTO_LOGIN = "auto_login";
	public static final String SERVICE_ARENA = "arena";

	private Context context;

	public MainUIControl(Context context) {
		this.context = context;
	}

	public void changeUiName(int uiId, String name) {
		Intent intent = new Intent(UI_RECEIVER_ACTION);
		intent.putExtra(EXTRA_UI_ID, uiId);
		intent.putExtra(EXTRA_UI_NAME, name);
		intent.putExtra(EXTRA_UI_TYPE, UITypeEnum.MenuItem);
		context.sendBroadcast(intent);
	}

	/**
	 * 发送服务运行状态广播
	 */
	public void sendStatus(String status, String serviceName, String detail) {
		Intent intent = new Intent(STATUS_RECEIVER_ACTION);
		intent.putExtra(EXTRA_STATUS, status);
		intent.putExtra(EXTRA_SERVICE_NAME, serviceName);
		intent.putExtra(EXTRA_DETAIL, detail);
		context.sendBroadcast(intent);
	}

	/**
	 * 发送停止广播
	 */
	public void sendStop() {
		Intent intent = new Intent(STOP_RECEIVER_ACTION);
		context.sendBroadcast(intent);
	}
}
