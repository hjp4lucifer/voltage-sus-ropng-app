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
}
