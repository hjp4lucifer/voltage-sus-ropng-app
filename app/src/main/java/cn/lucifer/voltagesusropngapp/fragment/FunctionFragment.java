package cn.lucifer.voltagesusropngapp.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;
import cn.lucifer.voltagesusropngapp.service.AutoArenaService;
import cn.lucifer.voltagesusropngapp.service.AutoLoginService;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;

/**
 * 功能页：包含自动登录和竞技场对战的功能卡片 + 日志列表
 */
public class FunctionFragment extends Fragment {

	private TextView textAutoLoginStatus;
	private Button btnAutoLoginStart;
	private TextView textArenaBattleStatus;
	private Button btnArenaBattleStart;

	private ListView listViewLog;
	private LogAdapter logAdapter;

	/**
	 * 当前运行的服务名称，null 表示空闲
	 */
	private String currentRunningService;

	private BroadcastReceiver logReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String logText = intent.getExtras().getString(LogPrinter.EXTRA_LOG_NAME);
			addLog(logText);
		}
	};

	private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String status = intent.getStringExtra(MainUIControl.EXTRA_STATUS);
			String serviceName = intent.getStringExtra(MainUIControl.EXTRA_SERVICE_NAME);
			String detail = intent.getStringExtra(MainUIControl.EXTRA_DETAIL);

			if (MainUIControl.STATUS_RUNNING.equals(status)) {
				currentRunningService = serviceName;
				updateCardStates(serviceName, detail);
			} else if (MainUIControl.STATUS_STOPPED.equals(status)) {
				currentRunningService = null;
				updateCardStates(null, null);
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_function, container, false);

		textAutoLoginStatus = root.findViewById(R.id.text_auto_login_status);
		btnAutoLoginStart = root.findViewById(R.id.btn_auto_login_start);
		textArenaBattleStatus = root.findViewById(R.id.text_arena_battle_status);
		btnArenaBattleStart = root.findViewById(R.id.btn_arena_battle_start);

		listViewLog = root.findViewById(R.id.listView_log);
		logAdapter = new LogAdapter(getContext());
		listViewLog.setAdapter(logAdapter);

		// 初始化按钮点击事件
		btnAutoLoginStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startAutoLogin();
			}
		});

		btnArenaBattleStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startArenaBattle();
			}
		});

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();

		// 注册日志广播
		IntentFilter logFilter = new IntentFilter();
		logFilter.addAction(LogPrinter.LOG_RECEIVER_ACTION);
		getActivity().registerReceiver(logReceiver, logFilter);

		// 注册状态广播
		IntentFilter statusFilter = new IntentFilter();
		statusFilter.addAction(MainUIControl.STATUS_RECEIVER_ACTION);
		getActivity().registerReceiver(statusReceiver, statusFilter);

		// 恢复状态
		restoreRunningState();
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			getActivity().unregisterReceiver(logReceiver);
		} catch (Exception e) {
			Log.w("FunctionFragment", "unregisterReceiver logReceiver error", e);
		}
		try {
			getActivity().unregisterReceiver(statusReceiver);
		} catch (Exception e) {
			Log.w("FunctionFragment", "unregisterReceiver statusReceiver error", e);
		}
	}

	private void addLog(String text) {
		logAdapter.addFirst(text);
	}

	/**
	 * 启动自动登录
	 */
	private void startAutoLogin() {
		if (currentRunningService != null) {
			return;
		}
		Context context = getContext();
		Intent intent = new Intent(context, AutoLoginService.class);
		intent.putExtra(AutoLoginService.AUTO_LOGIN_TAG, AutoLoginService.AUTO_LOGIN_START);
		context.startService(intent);
	}

	/**
	 * 启动竞技场对战
	 */
	private void startArenaBattle() {
		if (currentRunningService != null) {
			return;
		}
		Context context = getContext();
		Intent intent = new Intent(context, AutoArenaService.class);
		intent.putExtra(AutoArenaService.ARENA_BATTLE_TAG, AutoArenaService.ARENA_BATTLE_START);
		context.startService(intent);
	}

	/**
	 * 从 SharedPreferences 恢复运行状态
	 */
	private void restoreRunningState() {
		String runningService = AppSettings.getRunningService(getContext());
		if (runningService != null) {
			currentRunningService = runningService;
			updateCardStates(runningService, null);
		} else {
			currentRunningService = null;
			updateCardStates(null, null);
		}
	}

	/**
	 * 更新功能卡片状态
	 */
	private void updateCardStates(String runningService, String detail) {
		if (runningService == null) {
			// 空闲态
			textAutoLoginStatus.setText(R.string.status_not_running);
			btnAutoLoginStart.setEnabled(true);
			btnAutoLoginStart.setText(R.string.btn_start);

			textArenaBattleStatus.setText(R.string.status_not_running);
			btnArenaBattleStart.setEnabled(true);
			btnArenaBattleStart.setText(R.string.btn_start);
		} else if (MainUIControl.SERVICE_AUTO_LOGIN.equals(runningService)) {
			// 自动登录运行中
			String statusText = getString(R.string.status_running);
			if (detail != null) {
				statusText = getString(R.string.status_running) + " (" + detail + ")";
			}
			textAutoLoginStatus.setText(statusText);
			btnAutoLoginStart.setEnabled(false);
			btnAutoLoginStart.setText(R.string.status_running);

			textArenaBattleStatus.setText(getString(R.string.hint_blocked_by_other, getString(R.string.action_auto_login)));
			btnArenaBattleStart.setEnabled(false);
		} else if (MainUIControl.SERVICE_ARENA.equals(runningService)) {
			// 竞技场运行中
			textAutoLoginStatus.setText(getString(R.string.hint_blocked_by_other, getString(R.string.action_arena_battle)));
			btnAutoLoginStart.setEnabled(false);

			String statusText = getString(R.string.status_running);
			if (detail != null) {
				statusText = getString(R.string.status_running) + " (" + detail + ")";
			}
			textArenaBattleStatus.setText(statusText);
			btnArenaBattleStart.setEnabled(false);
			btnArenaBattleStart.setText(R.string.status_running);
		}
	}
}
