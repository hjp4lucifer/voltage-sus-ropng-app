package cn.lucifer.voltagesusropngapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 全局配置管理，封装 SharedPreferences 的读写操作
 */
public final class AppSettings {

	private static final String PREFS_NAME = "voltage_sus_settings";

	private static final String KEY_ARENA_ID = "arena_id";
	private static final String KEY_RAID_ID = "raid_id";
	private static final String KEY_APPLI_VERSION = "appli_version";
	private static final String KEY_RUNNING_SERVICE = "running_service";

	private static final int DEFAULT_ARENA_ID = 117;
	private static final String DEFAULT_RAID_ID = null;
	private static final String DEFAULT_APPLI_VERSION = "9.2.0";

	private AppSettings() {
	}

	private static SharedPreferences getPrefs(Context context) {
		return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	// ==================== arenaId ====================

	public static int getArenaId(Context context) {
		return getPrefs(context).getInt(KEY_ARENA_ID, DEFAULT_ARENA_ID);
	}

	public static void setArenaId(Context context, int arenaId) {
		getPrefs(context).edit().putInt(KEY_ARENA_ID, arenaId).apply();
	}

	// ==================== raidId ====================

	public static String getRaidId(Context context) {
		return getPrefs(context).getString(KEY_RAID_ID, DEFAULT_RAID_ID);
	}

	public static void setRaidId(Context context, String raidId) {
		getPrefs(context).edit().putString(KEY_RAID_ID, raidId).apply();
	}

	// ==================== appliVersion ====================

	public static String getAppliVersion(Context context) {
		return getPrefs(context).getString(KEY_APPLI_VERSION, DEFAULT_APPLI_VERSION);
	}

	public static void setAppliVersion(Context context, String appliVersion) {
		getPrefs(context).edit().putString(KEY_APPLI_VERSION, appliVersion).apply();
	}

	// ==================== 运行标志（互斥控制）====================

	/**
	 * 获取当前正在运行的 Service 名称，null 表示无服务运行
	 */
	public static String getRunningService(Context context) {
		return getPrefs(context).getString(KEY_RUNNING_SERVICE, null);
	}

	/**
	 * 设置当前正在运行的 Service 名称
	 */
	public static void setRunningService(Context context, String serviceName) {
		getPrefs(context).edit().putString(KEY_RUNNING_SERVICE, serviceName).apply();
	}

	/**
	 * 清除运行标志
	 */
	public static void clearRunningService(Context context) {
		getPrefs(context).edit().remove(KEY_RUNNING_SERVICE).apply();
	}
}
