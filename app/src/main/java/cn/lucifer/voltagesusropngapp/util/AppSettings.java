package cn.lucifer.voltagesusropngapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import cn.lucifer.voltage.sus.api.BaseApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局配置管理，封装 SharedPreferences 的读写操作
 */
public final class AppSettings {

	private static final String PREFS_NAME = "voltage_sus_settings";

	private static final String KEY_ARENA_ID = "arena_id";
	private static final String KEY_RAID_ID = "raid_id";
	private static final String KEY_APPLI_VERSION = "appli_version";
	private static final String KEY_RUNNING_SERVICE = "running_service";
	private static final String KEY_NSID = "nsid";
	private static final String KEY_DEVICE_UID = "device_uid";
	private static final String KEY_PUKEY = "pu_key";
	private static final String KEY_PFID = "pfid";
	private static final String KEY_ROOKIE = "rookie";
	private static final String KEY_CARD_UPGRADE_ID_LIST = "card_upgrade_id_list";
	private static final String KEY_CHARACTER_NAME = "character_name";
	private static final String KEY_EXCLUDE_NAME = "exclude_name";
	private static final String KEY_MAX_COUNT = "max_count";

	private static final int DEFAULT_ARENA_ID = 117;
	private static final String DEFAULT_RAID_ID = null;
	private static final String DEFAULT_APPLI_VERSION = "9.2.0";
	private static final String DEFAULT_NSID = null;
	private static final String DEFAULT_DEVICE_UID = null;
	private static final String DEFAULT_PUKEY = null;
	private static final int DEFAULT_PFID = 8;
	private static final boolean DEFAULT_ROOKIE = true;
	private static final int DEFAULT_MAX_COUNT = 100;

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

	// ==================== nsid ====================

	public static String getNsid(Context context) {
		return getPrefs(context).getString(KEY_NSID, DEFAULT_NSID);
	}

	public static void setNsid(Context context, String nsid) {
		getPrefs(context).edit().putString(KEY_NSID, nsid).apply();
	}

	// ==================== deviceUid ====================

	public static String getDeviceUid(Context context) {
		return getPrefs(context).getString(KEY_DEVICE_UID, DEFAULT_DEVICE_UID);
	}

	public static void setDeviceUid(Context context, String deviceUid) {
		getPrefs(context).edit().putString(KEY_DEVICE_UID, deviceUid).apply();
	}

	// ==================== puKey ====================

	public static String getPuKey(Context context) {
		return getPrefs(context).getString(KEY_PUKEY, DEFAULT_PUKEY);
	}

	public static void setPuKey(Context context, String puKey) {
		getPrefs(context).edit().putString(KEY_PUKEY, puKey).apply();
	}

	// ==================== pfid ====================

	public static int getPfid(Context context) {
		return getPrefs(context).getInt(KEY_PFID, DEFAULT_PFID);
	}

	public static void setPfid(Context context, int pfid) {
		getPrefs(context).edit().putInt(KEY_PFID, pfid).apply();
	}

	// ==================== rookie ====================

	public static boolean getRookie(Context context) {
		return getPrefs(context).getBoolean(KEY_ROOKIE, DEFAULT_ROOKIE);
	}

	public static void setRookie(Context context, boolean rookie) {
		getPrefs(context).edit().putBoolean(KEY_ROOKIE, rookie).apply();
	}

	// ==================== card_upgrade_id_list ====================

	/**
	 * 获取卡牌升阶ID列表（逗号分隔字符串解析为 List<Integer>）
	 */
	public static List<Integer> getCardUpgradeIdList(Context context) {
		String raw = getPrefs(context).getString(KEY_CARD_UPGRADE_ID_LIST, null);
		List<Integer> result = new ArrayList<>();
		if (raw == null || raw.trim().isEmpty()) {
			return result;
		}
		String[] parts = raw.split(",");
		for (String part : parts) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				try {
					result.add(Integer.parseInt(trimmed));
				} catch (NumberFormatException e) {
					// 忽略无法解析的值
				}
			}
		}
		return result;
	}

	/**
	 * 保存卡牌升阶ID列表（原始逗号分隔字符串）
	 */
	public static void setCardUpgradeIdList(Context context, String cardIdListStr) {
		getPrefs(context).edit().putString(KEY_CARD_UPGRADE_ID_LIST, cardIdListStr).apply();
	}

	/**
	 * 获取卡牌升阶ID列表原始字符串
	 */
	public static String getCardUpgradeIdListRaw(Context context) {
		return getPrefs(context).getString(KEY_CARD_UPGRADE_ID_LIST, null);
	}

	// ==================== character_name ====================

	public static String getCharacterName(Context context) {
		return getPrefs(context).getString(KEY_CHARACTER_NAME, null);
	}

	public static void setCharacterName(Context context, String characterName) {
		getPrefs(context).edit().putString(KEY_CHARACTER_NAME, characterName).apply();
	}

	// ==================== exclude_name ====================

	public static String getExcludeName(Context context) {
		return getPrefs(context).getString(KEY_EXCLUDE_NAME, null);
	}

	public static void setExcludeName(Context context, String excludeName) {
		getPrefs(context).edit().putString(KEY_EXCLUDE_NAME, excludeName).apply();
	}

	// ==================== max_count ====================

	public static int getMaxCount(Context context) {
		return getPrefs(context).getInt(KEY_MAX_COUNT, DEFAULT_MAX_COUNT);
	}

	public static void setMaxCount(Context context, int maxCount) {
		getPrefs(context).edit().putInt(KEY_MAX_COUNT, maxCount).apply();
	}

	// ==================== 公共覆盖方法 ====================

	/**
	 * 将 SP 中的全部配置覆盖到 BaseApi（含身份字段）
	 * 适用于单账号场景（如 AutoArena）
	 * 注意：多账号批量场景（如 AutoLogin）不应使用此方法，
	 *       否则所有账号的身份字段会被覆盖为同一个值
	 */
	public static void applyOverrideSettings(Context context, BaseApi api) {
		// 业务参数
		api.setArenaId(getArenaId(context));
		api.setAppliVersion(getAppliVersion(context));
		String raidId = getRaidId(context);
		if (raidId != null) {
			api.setRaidId(raidId);
		}

		// 身份字段
		String nsid = getNsid(context);
		if (nsid != null) {
			api.setNsid(nsid);
		}
		String deviceUid = getDeviceUid(context);
		if (deviceUid != null) {
			api.setDeviceUid(deviceUid);
		}
		String puKey = getPuKey(context);
		if (puKey != null) {
			api.setPuKey(puKey);
		}
		api.setPfid(getPfid(context));
		api.setRookie(getRookie(context));
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
