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

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.util.AppSettings;

/**
 * 设置页：全局配置管理
 */
public class SettingsFragment extends Fragment {

	private static final int REQUEST_CODE_SELECT_CONFIG = 1001;

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

			JSONObject config = JSONObject.parseObject(jsonStr);
			fillUIFromConfig(config);

			Toast.makeText(getContext(), R.string.config_loaded, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(getContext(), R.string.config_load_failed, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 将 JSON 配置填充到 UI
	 */
	private void fillUIFromConfig(JSONObject config) {
		// 账号身份信息（只读展示）
		String nsid = config.getString("nsid");
		String deviceUid = config.getString("deviceUid");
		Integer pfid = config.getInteger("pfid");
		String puKey = config.getString("puKey");
		Boolean rookie = config.getBoolean("rookie");

		textNsid.setText(nsid != null ? nsid : "");
		textDeviceUid.setText(deviceUid != null ? deviceUid : "");
		textPfid.setText(pfid != null ? String.valueOf(pfid) : "");
		textPukey.setText(puKey != null ? puKey : "");
		textRookie.setText(rookie != null ? String.valueOf(rookie) : "");

		// 业务参数（自动填充 EditText）
		Integer arenaId = config.getInteger("arenaId");
		String raidId = config.getString("raidId");
		String appliVersion = config.getString("appliVersion");

		if (arenaId != null) {
			editArenaId.setText(String.valueOf(arenaId));
		}
		if (raidId != null) {
			editRaidId.setText(raidId);
		}
		if (appliVersion != null) {
			editAppliVersion.setText(appliVersion);
		}
	}

	/**
	 * 从 SharedPreferences 加载已保存的配置
	 */
	private void loadSettings() {
		Context context = getContext();
		if (context == null) {
			return;
		}

		int arenaId = AppSettings.getArenaId(context);
		editArenaId.setText(String.valueOf(arenaId));

		String raidId = AppSettings.getRaidId(context);
		if (raidId != null) {
			editRaidId.setText(raidId);
		}

		String appliVersion = AppSettings.getAppliVersion(context);
		editAppliVersion.setText(appliVersion);

		// 加载账号身份信息（始终展示）
		String nsid = AppSettings.getNsid(context);
		String deviceUid = AppSettings.getDeviceUid(context);
		int pfid = AppSettings.getPfid(context);
		String puKey = AppSettings.getPuKey(context);
		boolean rookie = AppSettings.getRookie(context);

		textNsid.setText(nsid != null ? nsid : "");
		textDeviceUid.setText(deviceUid != null ? deviceUid : "");
		textPfid.setText(String.valueOf(pfid));
		textPukey.setText(puKey != null ? puKey : "");
		textRookie.setText(String.valueOf(rookie));
	}

	/**
	 * 保存配置到 SharedPreferences
	 */
	private void saveSettings() {
		Context context = getContext();
		if (context == null) {
			return;
		}

		// arenaId
		String arenaIdStr = editArenaId.getText().toString().trim();
		if (!arenaIdStr.isEmpty()) {
			try {
				AppSettings.setArenaId(context, Integer.parseInt(arenaIdStr));
			} catch (NumberFormatException e) {
				editArenaId.setError("请输入有效的数字");
				return;
			}
		}

		// raidId
		String raidId = editRaidId.getText().toString().trim();
		AppSettings.setRaidId(context, raidId.isEmpty() ? null : raidId);

		// appliVersion
		String appliVersion = editAppliVersion.getText().toString().trim();
		if (!appliVersion.isEmpty()) {
			AppSettings.setAppliVersion(context, appliVersion);
		}

		// 账号身份字段（从只读展示区获取）
		String nsid = textNsid.getText().toString().trim();
		AppSettings.setNsid(context, nsid.isEmpty() ? null : nsid);

		String deviceUid = textDeviceUid.getText().toString().trim();
		AppSettings.setDeviceUid(context, deviceUid.isEmpty() ? null : deviceUid);

		String pfidStr = textPfid.getText().toString().trim();
		if (!pfidStr.isEmpty()) {
			try {
				AppSettings.setPfid(context, Integer.parseInt(pfidStr));
			} catch (NumberFormatException e) {
				// pfid 解析失败时使用默认值
				AppSettings.setPfid(context, 8);
			}
		}

		String puKey = textPukey.getText().toString().trim();
		AppSettings.setPuKey(context, puKey.isEmpty() ? null : puKey);

		String rookieStr = textRookie.getText().toString().trim();
		AppSettings.setRookie(context, !rookieStr.isEmpty() && Boolean.parseBoolean(rookieStr));

		Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show();
	}
}
