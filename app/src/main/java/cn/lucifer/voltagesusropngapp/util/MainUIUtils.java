package cn.lucifer.voltagesusropngapp.util;

import cn.lucifer.voltagesusropngapp.ui.MainUIControl;

public final class MainUIUtils {
	public static MainUIControl mainUIControl;

	public static void changeUiName(int uiId, String name) {
		mainUIControl.changeUiName(uiId, name);
	}
}
