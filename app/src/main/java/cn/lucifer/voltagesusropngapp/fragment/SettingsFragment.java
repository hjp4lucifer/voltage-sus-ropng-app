package cn.lucifer.voltagesusropngapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.util.AppSettings;

/**
 * 设置页：全局配置管理
 */
public class SettingsFragment extends Fragment {

	private EditText editArenaId;
	private EditText editRaidId;
	private EditText editAppliVersion;
	private Button btnSave;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_settings, container, false);

		editArenaId = root.findViewById(R.id.edit_arena_id);
		editRaidId = root.findViewById(R.id.edit_raid_id);
		editAppliVersion = root.findViewById(R.id.edit_appli_version);
		btnSave = root.findViewById(R.id.btn_save_settings);

		// 加载已保存的配置
		loadSettings();

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
	 * 从 SharedPreferences 加载已保存的配置
	 */
	private void loadSettings() {
		Context context = getContext();

		int arenaId = AppSettings.getArenaId(context);
		editArenaId.setText(String.valueOf(arenaId));

		String raidId = AppSettings.getRaidId(context);
		if (raidId != null) {
			editRaidId.setText(raidId);
		}

		String appliVersion = AppSettings.getAppliVersion(context);
		editAppliVersion.setText(appliVersion);
	}

	/**
	 * 保存配置到 SharedPreferences
	 */
	private void saveSettings() {
		Context context = getContext();

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

		Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show();
	}
}
