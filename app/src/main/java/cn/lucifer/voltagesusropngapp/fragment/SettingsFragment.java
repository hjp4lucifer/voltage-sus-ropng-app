package cn.lucifer.voltagesusropngapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.model.ConfigFile;
import cn.lucifer.voltagesusropngapp.util.AppSettings;

/**
 * 设置页：全局配置管理
 */
public class SettingsFragment extends Fragment {

	private static final int REQUEST_CODE_SELECT_CONFIG = 1001;

	private static final int REQUEST_CODE_EXPORT_CONFIG = 1002;

	private Button btnSelectConfigFile;
	private Button btnLoadConfig;
	private TextView textNsid;
	private TextView textDeviceUid;
	private TextView textPfid;
	private TextView textPukey;
	private TextView textRookie;
	private EditText editArenaId;
	private EditText editRaidId;
	private EditText editAppliVersion;
	private EditText editSleepBase;
	private EditText editCardIdList;
	private Button btnExportConfig;
	private Button btnSave;

	private Uri selectedConfigUri;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_settings, container, false);

		btnSelectConfigFile = root.findViewById(R.id.btn_select_config_file);
		btnLoadConfig = root.findViewById(R.id.btn_load_config);
		textNsid = root.findViewById(R.id.text_nsid);
		textDeviceUid = root.findViewById(R.id.text_device_uid);
		textPfid = root.findViewById(R.id.text_pfid);
		textPukey = root.findViewById(R.id.text_pukey);
		textRookie = root.findViewById(R.id.text_rookie);
		editArenaId = root.findViewById(R.id.edit_arena_id);
		editRaidId = root.findViewById(R.id.edit_raid_id);
		editAppliVersion = root.findViewById(R.id.edit_appli_version);
		editSleepBase = root.findViewById(R.id.edit_sleep_base_ms);
		editCardIdList = root.findViewById(R.id.edit_card_upgrade_id_list);
		btnExportConfig = root.findViewById(R.id.btn_export_config);
		btnSave = root.findViewById(R.id.btn_save_settings);

		// 加载已保存的配置
		loadSettings();

		// 选择配置文件按钮
		btnSelectConfigFile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openFilePicker();
			}
		});

		// 加载按钮
		btnLoadConfig.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadConfigFile();
			}
		});

		// 导出按钮
		btnExportConfig.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openExportFilePicker();
			}
		});

		// 保存按钮
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveSettings();
			}
		});

		return root;
	}

	/**
	 * 打开系统文件选择器（SAF）
	 */
	private void openFilePicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivityForResult(intent, REQUEST_CODE_SELECT_CONFIG);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_SELECT_CONFIG && resultCode == getActivity().RESULT_OK) {
			if (data != null && data.getData() != null) {
				selectedConfigUri = data.getData();
				// 选中后自动加载
				loadConfigFile();
			}
			return;
		}

		if (requestCode == REQUEST_CODE_EXPORT_CONFIG && resultCode == getActivity().RESULT_OK) {
			if (data != null && data.getData() != null) {
				// 选中保存位置后自动导出
				exportConfigFile(data.getData());
			}
		}
	}

	/**
	 * 打开系统文件创建器（SAF），用于导出配置
	 */
	private void openExportFilePicker() {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/json");
		intent.putExtra(Intent.EXTRA_TITLE, "voltage_sus_config.json");
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		startActivityForResult(intent, REQUEST_CODE_EXPORT_CONFIG);
	}

	/**
	 * 把当前已保存的配置导出为 JSON 文件
	 * <p>
	 * 导出源为 SharedPreferences（而非页面输入框草稿），字段集与 {@link #fillUIFromConfig(ConfigFile)} 严格一致
	 */
	private void exportConfigFile(Uri uri) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		try {
			ConfigFile config = AppSettings.toConfigFile(context);

			OutputStream outputStream = getActivity().getContentResolver().openOutputStream(uri);
			if (outputStream == null) {
				Toast.makeText(context, R.string.config_export_failed, Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				IOUtils.write(JSON.toJSONString(config), outputStream, "UTF-8");
			} finally {
				outputStream.close();
			}

			Toast.makeText(context, R.string.config_exported, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(context, R.string.config_export_failed, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 从选中的 Uri 加载配置文件并填充 UI
	 */
	private void loadConfigFile() {
		if (selectedConfigUri == null) {
			Toast.makeText(getContext(), R.string.config_load_failed, Toast.LENGTH_SHORT).show();
			return;
		}

		try {
			InputStream inputStream = getActivity().getContentResolver().openInputStream(selectedConfigUri);
			if (inputStream == null) {
				Toast.makeText(getContext(), R.string.config_load_failed, Toast.LENGTH_SHORT).show();
				return;
			}

			String jsonStr = IOUtils.toString(inputStream, "UTF-8");
			inputStream.close();

			ConfigFile config = JSON.parseObject(jsonStr, ConfigFile.class);
			fillUIFromConfig(config);

			Toast.makeText(getContext(), R.string.config_loaded, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(getContext(), R.string.config_load_failed, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 将配置对象填充到 UI。null 字段表示"未提供"，SHALL NOT 覆盖当前显示值
	 * <p>
	 * 本方法同时服务于两条路径：设置页首次加载（{@link #loadSettings()}，由 {@link AppSettings#toConfigFile} 提供
	 * 完整快照）与文件导入（文件缺失的字段保持 null，从而"沿用当前配置"）。
	 */
	private void fillUIFromConfig(ConfigFile config) {
		// 账号身份信息（只读展示）
		if (config.getNsid() != null) {
			textNsid.setText(config.getNsid());
		}
		if (config.getDeviceUid() != null) {
			textDeviceUid.setText(config.getDeviceUid());
		}
		if (config.getPfid() != null) {
			textPfid.setText(String.valueOf(config.getPfid()));
		}
		if (config.getPuKey() != null) {
			textPukey.setText(config.getPuKey());
		}
		if (config.getRookie() != null) {
			textRookie.setText(String.valueOf(config.getRookie()));
		}

		// 业务参数（自动填充 EditText）
		if (config.getArenaId() != null) {
			editArenaId.setText(String.valueOf(config.getArenaId()));
		}
		if (config.getRaidId() != null) {
			editRaidId.setText(config.getRaidId());
		}
		if (config.getAppliVersion() != null) {
			editAppliVersion.setText(config.getAppliVersion());
		}
		if (config.getSleepBaseMs() != null) {
			editSleepBase.setText(String.valueOf(config.getSleepBaseMs()));
		}
		if (config.getCardUpgradeIdList() != null) {
			editCardIdList.setText(config.getCardUpgradeIdList());
		}
	}

	/**
	 * 从 SharedPreferences 加载已保存的配置（SP 是完整快照，因此各输入框会被完整渲染）
	 */
	private void loadSettings() {
		Context context = getContext();
		if (context == null) {
			return;
		}

		fillUIFromConfig(AppSettings.toConfigFile(context));
	}

	/**
	 * 保存配置到 SharedPreferences
	 * <p>
	 * 先全量校验、再一次性写入：任一字段非法时不会写入任何字段，避免"提示失败但配置已被改了一半"
	 */
	private void saveSettings() {
		Context context = getContext();
		if (context == null) {
			return;
		}

		ConfigFile config = collectForm();
		if (config == null) {
			// 校验未通过，已在对应输入框给出提示，且未写入任何字段
			return;
		}

		AppSettings.saveConfigFile(context, config);

		// 回显实际生效的值：空输入会被解析为"保持当前配置"，刷新后输入框显示的就是落库结果
		loadSettings();

		Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 采集设置页表单为 {@link ConfigFile}，并完成全量校验。
	 * <p>
	 * 第一版的 8 个配置字段（nsid、deviceUid、pfid、puKey、rookie、arenaId、raidId、appliVersion）是核心字段，
	 * 空值与空字符串都不允许写入。空输入的处理方式统一为"沿用 {@link AppSettings} 中已保存的值"——即 UI 上的
	 * 值被清空不会削弱既有配置；仅当已保存值也为空（确实缺失）时才通过 {@code setError} 提示并返回 null。
	 * 非核心字段（sleepBaseMs、cardUpgradeIdList）同样"空 = 沿用已保存值"，其默认值由 {@link AppSettings} 提供。
	 * 任一字段非法时返回 null，由调用方保证不落库。
	 */
	private ConfigFile collectForm() {
		Context context = getContext();
		ConfigFile config = new ConfigFile();

		// arenaId：核心字段；UI 为空时沿用已保存值（默认 117），非整数则报错
		String arenaIdStr = editArenaId.getText().toString().trim();
		if (arenaIdStr.isEmpty()) {
			config.setArenaId(AppSettings.getArenaId(context));
		} else {
			try {
				config.setArenaId(Integer.parseInt(arenaIdStr));
			} catch (NumberFormatException e) {
				editArenaId.setError(getString(R.string.settings_arena_id_invalid));
				return null;
			}
		}

		// raidId：核心字段；UI 为空时沿用已保存值，两边都为空则报错
		String raidId = resolveCoreField(editRaidId, AppSettings.getRaidId(context));
		if (raidId == null) {
			return null;
		}
		config.setRaidId(raidId);

		// appliVersion：核心字段；UI 为空时沿用已保存值（默认 9.2.0）
		String appliVersion = resolveCoreField(editAppliVersion, AppSettings.getAppliVersion(context));
		if (appliVersion == null) {
			return null;
		}
		config.setAppliVersion(appliVersion);

		// sleepBaseMs：非核心字段，空 = 保持当前配置；非负整数
		String sleepBaseStr = editSleepBase.getText().toString().trim();
		if (sleepBaseStr.isEmpty()) {
			config.setSleepBaseMs(AppSettings.getSleepBaseMs(context));
		} else {
			try {
				int sleepBaseMs = Integer.parseInt(sleepBaseStr);
				if (sleepBaseMs < 0) {
					editSleepBase.setError(getString(R.string.settings_sleep_base_ms_invalid));
					return null;
				}
				config.setSleepBaseMs(sleepBaseMs);
			} catch (NumberFormatException e) {
				editSleepBase.setError(getString(R.string.settings_sleep_base_ms_invalid));
				return null;
			}
		}

		// cardUpgradeIdList：空 = 保持当前配置；非空时校验逗号分隔的每个值为正整数
		String cardIdListStr = editCardIdList.getText().toString().trim();
		if (cardIdListStr.isEmpty()) {
			config.setCardUpgradeIdList(AppSettings.getCardUpgradeIdListRaw(context));
		} else {
			String[] parts = cardIdListStr.split(",");
			for (String part : parts) {
				String trimmed = part.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				try {
					int value = Integer.parseInt(trimmed);
					if (value <= 0) {
						editCardIdList.setError(getString(R.string.settings_card_upgrade_id_list_invalid));
						return null;
					}
				} catch (NumberFormatException e) {
					editCardIdList.setError(getString(R.string.settings_card_upgrade_id_list_invalid));
					return null;
				}
			}
			config.setCardUpgradeIdList(cardIdListStr);
		}

		// 账号身份字段（只读展示区）：核心字段，UI 被清空时沿用已保存值
		String nsid = resolveCoreField(textNsid, AppSettings.getNsid(context));
		if (nsid == null) {
			return null;
		}
		config.setNsid(nsid);

		String deviceUid = resolveCoreField(textDeviceUid, AppSettings.getDeviceUid(context));
		if (deviceUid == null) {
			return null;
		}
		config.setDeviceUid(deviceUid);

		String pfidStr = textPfid.getText().toString().trim();
		if (pfidStr.isEmpty()) {
			config.setPfid(AppSettings.getPfid(context));
		} else {
			try {
				config.setPfid(Integer.parseInt(pfidStr));
			} catch (NumberFormatException e) {
				// pfid 解析失败时沿用已保存值
				config.setPfid(AppSettings.getPfid(context));
			}
		}

		String puKey = resolveCoreField(textPukey, AppSettings.getPuKey(context));
		if (puKey == null) {
			return null;
		}
		config.setPuKey(puKey);

		String rookieStr = textRookie.getText().toString().trim();
		if (rookieStr.isEmpty()) {
			config.setRookie(AppSettings.getRookie(context));
		} else {
			config.setRookie(Boolean.parseBoolean(rookieStr));
		}

		return config;
	}

	/**
	 * 解析核心字段的最终值：UI 上的值被清空（"强行删除"）时，沿用 {@link AppSettings} 中已保存的值；
	 * 两者都为空说明该字段确实缺失，给出"不能为空"提示并返回 null。
	 *
	 * @param view        承载该字段的控件，用于在确实缺失时给出错误提示
	 * @param storedValue SharedPreferences 中已保存的值，可能为 null
	 * @return 解析后的非空值；确实缺失时返回 null
	 */
	private String resolveCoreField(TextView view, String storedValue) {
		String uiValue = view.getText().toString().trim();
		String resolvedValue = uiValue.isEmpty() ? storedValue : uiValue;
		if (resolvedValue == null || resolvedValue.isEmpty()) {
			view.setError(getString(R.string.settings_field_required));
			return null;
		}
		return resolvedValue;
	}
}
