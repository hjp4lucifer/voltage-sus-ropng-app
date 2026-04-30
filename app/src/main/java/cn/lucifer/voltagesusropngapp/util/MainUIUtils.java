package cn.lucifer.voltagesusropngapp.util;

import cn.lucifer.voltagesusropngapp.ui.MainUIControl;

public final class MainUIUtils {
	public static MainUIControl mainUIControl;

	public static void changeUiName(int uiId, String name) {
		mainUIControl.changeUiName(uiId, name);
	}

	/**
	 * 发送服务运行状态广播
	 */
	public static void sendStatus(String status, String serviceName, String detail) {
		if (mainUIControl != null) {
			mainUIControl.sendStatus(status, serviceName, detail);
		}
	}

	/**
	 * 发送停止广播
	 */
	public static void sendStop() {
		if (mainUIControl != null) {
			mainUIControl.sendStop();
		}
	}
}
