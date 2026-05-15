package cn.lucifer.voltagesusropngapp.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.lucifer.voltagesusropngapp.R;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;
import cn.lucifer.voltagesusropngapp.service.AutoArenaService;
import cn.lucifer.voltagesusropngapp.service.AutoCardUpgradeService;
import cn.lucifer.voltagesusropngapp.service.AutoLoginService;
import cn.lucifer.voltagesusropngapp.service.AutoPresentBatchService;
import cn.lucifer.voltagesusropngapp.service.AutoPresentCharacterService;
import cn.lucifer.voltagesusropngapp.ui.MainUIControl;
import cn.lucifer.voltagesusropngapp.util.AppSettings;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;

/**
 * 功能页：列表态展示所有功能，聚焦态展示单个功能的参数、运行控制和日志
 */
public class FunctionFragment extends Fragment {

	// ==================== 功能标识常量 ====================

	private static final String FUNC_AUTO_LOGIN = "auto_login";
	private static final String FUNC_ARENA_BATTLE = "arena_battle";
	private static final String FUNC_CARD_UPGRADE = "card_upgrade";
	private static final String FUNC_PRESENT_CHARACTER = "present_character";
	private static final String FUNC_PRESENT_BATCH = "present_batch";

	// ==================== 列表态视图 ====================

	private View layoutListState;
	private LinearLayout groupDaily;
	private LinearLayout groupPresent;

	// ==================== 聚焦态视图 ====================

	private View layoutFocusState;
	private ImageView btnBack;
	private TextView textFocusTitle;
	private TextView textFocusStatus;
	private Button btnFocusStart;
	private LinearLayout layoutParams;

	// 聚焦态参数行
	private View paramArenaIdRow;
	private EditText editArenaId;
	private View paramCardUpgradeIdsRow;
	private EditText editCardUpgradeIds;
	private View paramCharacterNameRow;
	private EditText editCharacterName;
	private View paramExcludeNameRow;
	private EditText editExcludeName;
	private View paramMaxCountRow;
	private EditText editMaxCount;

	// ==================== 日志 ====================

	private ListView listViewLog;
	private LogAdapter logAdapter;

	// ==================== 状态 ====================

	/**
	 * 当前聚焦的功能标识，null 表示处于列表态
	 */
	private String currentFocusFunction;

	/**
	 * 当前运行的服务名称，null 表示空闲
	 */
	private String currentRunningService;

	/**
	 * 运行中的详情
	 */
	private String currentRunningDetail;

	// ==================== 广播接收器 ====================

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
				currentRunningDetail = detail;
				updateStates();
			} else if (MainUIControl.STATUS_STOPPED.equals(status)) {
				currentRunningService = null;
				currentRunningDetail = null;
				updateStates();
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_function, container, false);

		// 列表态视图
		layoutListState = root.findViewById(R.id.layout_list_state);
		groupDaily = root.findViewById(R.id.group_daily);
		groupPresent = root.findViewById(R.id.group_present);

		// 聚焦态视图
		layoutFocusState = root.findViewById(R.id.layout_focus_state);
		btnBack = root.findViewById(R.id.btn_back);
		textFocusTitle = root.findViewById(R.id.text_focus_title);
		textFocusStatus = root.findViewById(R.id.text_focus_status);
		btnFocusStart = root.findViewById(R.id.btn_focus_start);
		layoutParams = root.findViewById(R.id.layout_params);

		// 参数行
		paramArenaIdRow = root.findViewById(R.id.param_arena_id_row);
		editArenaId = root.findViewById(R.id.edit_arena_id);
		paramCardUpgradeIdsRow = root.findViewById(R.id.param_card_upgrade_ids_row);
		editCardUpgradeIds = root.findViewById(R.id.edit_card_upgrade_ids);
		paramCharacterNameRow = root.findViewById(R.id.param_character_name_row);
		editCharacterName = root.findViewById(R.id.edit_character_name);
		paramExcludeNameRow = root.findViewById(R.id.param_exclude_name_row);
		editExcludeName = root.findViewById(R.id.edit_exclude_name);
		paramMaxCountRow = root.findViewById(R.id.param_max_count_row);
		editMaxCount = root.findViewById(R.id.edit_max_count);

		// 日志
		listViewLog = root.findViewById(R.id.listView_log);
		logAdapter = new LogAdapter(getContext());
		listViewLog.setAdapter(logAdapter);

		// 返回按钮
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isCurrentFunctionRunning()) {
					return;
				}
				switchToListState();
			}
		});

		// 开始按钮
		btnFocusStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startCurrentFunction();
			}
		});

		// 返回键拦截
		root.setFocusableInTouchMode(true);
		root.requestFocus();
		root.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
					if (currentFocusFunction != null) {
						if (isCurrentFunctionRunning()) {
							return true;
						}
						switchToListState();
						return true;
					}
				}
				return false;
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

	// ==================== 列表态 ====================

	/**
	 * 刷新列表态的功能行
	 */
	private void refreshFunctionList() {
		groupDaily.removeAllViews();
		groupPresent.removeAllViews();

		addFunctionRow(groupDaily, FUNC_AUTO_LOGIN, R.string.action_auto_login);
		addFunctionRow(groupDaily, FUNC_ARENA_BATTLE, R.string.action_arena_battle);
		addFunctionRow(groupDaily, FUNC_CARD_UPGRADE, R.string.action_card_upgrade);

		addFunctionRow(groupPresent, FUNC_PRESENT_CHARACTER, R.string.action_present_character);
		addFunctionRow(groupPresent, FUNC_PRESENT_BATCH, R.string.action_present_batch);
	}

	/**
	 * 添加一个功能行到指定分组
	 */
	private void addFunctionRow(LinearLayout group, final String funcId, int titleResId) {
		View rowView = LayoutInflater.from(getContext()).inflate(R.layout.item_function, group, false);

		TextView textTitle = rowView.findViewById(R.id.text_function_title);
		final TextView textStatus = rowView.findViewById(R.id.text_function_status);
		ImageView btnEnter = rowView.findViewById(R.id.btn_function_enter);

		textTitle.setText(titleResId);

		// 设置状态
		String serviceName = funcIdToServiceName(funcId);
		if (serviceName != null && serviceName.equals(currentRunningService)) {
			textStatus.setText(R.string.status_running);
			rowView.setTag(R.id.tag_function_running, true);
		} else if (currentRunningService != null) {
			textStatus.setText(getString(R.string.hint_blocked_by_other, getServiceDisplayName(currentRunningService)));
			rowView.setEnabled(false);
			rowView.setTag(R.id.tag_function_running, false);
		} else {
			textStatus.setText(R.string.status_not_running);
			rowView.setTag(R.id.tag_function_running, false);
		}

		// 点击整行进入聚焦态
		rowView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentRunningService != null && !funcIdToServiceName(funcId).equals(currentRunningService)) {
					// 被其他功能阻塞，不可进入
					return;
				}
				switchToFocusState(funcId);
			}
		});

		group.addView(rowView);
	}

	// ==================== 两态切换 ====================

	/**
	 * 切换到列表态
	 */
	private void switchToListState() {
		currentFocusFunction = null;
		layoutListState.setVisibility(View.VISIBLE);
		layoutFocusState.setVisibility(View.GONE);
		refreshFunctionList();
	}

	/**
	 * 切换到聚焦态
	 */
	private void switchToFocusState(String funcId) {
		currentFocusFunction = funcId;

		// 设置标题
		textFocusTitle.setText(getFunctionDisplayName(funcId));

		// 配置参数区
		configureParamsForFunction(funcId);

		// 更新运行状态
		updateFocusState();

		// 切换视图
		layoutListState.setVisibility(View.GONE);
		layoutFocusState.setVisibility(View.VISIBLE);
	}

	// ==================== 参数配置 ====================

	/**
	 * 根据功能类型配置参数区的显示/隐藏及预填值
	 */
	private void configureParamsForFunction(String funcId) {
		// 先隐藏所有参数
		layoutParams.setVisibility(View.GONE);
		paramArenaIdRow.setVisibility(View.GONE);
		paramCardUpgradeIdsRow.setVisibility(View.GONE);
		paramCharacterNameRow.setVisibility(View.GONE);
		paramExcludeNameRow.setVisibility(View.GONE);
		paramMaxCountRow.setVisibility(View.GONE);

		switch (funcId) {
			case FUNC_ARENA_BATTLE:
				layoutParams.setVisibility(View.VISIBLE);
				paramArenaIdRow.setVisibility(View.VISIBLE);
				editArenaId.setText(String.valueOf(AppSettings.getArenaId(getContext())));
				break;
			case FUNC_CARD_UPGRADE:
				layoutParams.setVisibility(View.VISIBLE);
				paramCardUpgradeIdsRow.setVisibility(View.VISIBLE);
				String cardIds = AppSettings.getCardUpgradeIdListRaw(getContext());
				if (cardIds != null) {
					editCardUpgradeIds.setText(cardIds);
				}
				break;
			case FUNC_PRESENT_CHARACTER:
				layoutParams.setVisibility(View.VISIBLE);
				paramCharacterNameRow.setVisibility(View.VISIBLE);
				String charName = AppSettings.getCharacterName(getContext());
				if (charName != null) {
					editCharacterName.setText(charName);
				}
				break;
			case FUNC_PRESENT_BATCH:
				layoutParams.setVisibility(View.VISIBLE);
				paramExcludeNameRow.setVisibility(View.VISIBLE);
				paramMaxCountRow.setVisibility(View.VISIBLE);
				String excludeName = AppSettings.getExcludeName(getContext());
				if (excludeName != null) {
					editExcludeName.setText(excludeName);
				}
				editMaxCount.setText(String.valueOf(AppSettings.getMaxCount(getContext())));
				break;
			default:
				// 自动登录等无参数功能，不显示参数区
				break;
		}
	}

	// ==================== 启动功能 ====================

	/**
	 * 启动当前聚焦的功能
	 */
	private void startCurrentFunction() {
		if (currentRunningService != null) {
			return;
		}

		Context context = getContext();
		Intent intent;

		switch (currentFocusFunction) {
			case FUNC_AUTO_LOGIN:
				intent = new Intent(context, AutoLoginService.class);
				intent.putExtra(AutoLoginService.AUTO_LOGIN_TAG, AutoLoginService.AUTO_LOGIN_START);
				context.startService(intent);
				break;

			case FUNC_ARENA_BATTLE:
				// 保存参数
				saveArenaParams();
				intent = new Intent(context, AutoArenaService.class);
				intent.putExtra(AutoArenaService.ARENA_BATTLE_TAG, AutoArenaService.ARENA_BATTLE_START);
				context.startService(intent);
				break;

			case FUNC_CARD_UPGRADE:
				// 保存参数
				saveCardUpgradeParams();
				intent = new Intent(context, AutoCardUpgradeService.class);
				intent.putExtra(AutoCardUpgradeService.CARD_UPGRADE_TAG, AutoCardUpgradeService.CARD_UPGRADE_START);
				context.startService(intent);
				break;

			case FUNC_PRESENT_CHARACTER:
				// 校验参数
				String charName = editCharacterName.getText().toString().trim();
				if (charName.isEmpty()) {
					Toast.makeText(getContext(), R.string.hint_character_name_required, Toast.LENGTH_SHORT).show();
					return;
				}
				// 保存参数
				AppSettings.setCharacterName(getContext(), charName);
				intent = new Intent(context, AutoPresentCharacterService.class);
				intent.putExtra(AutoPresentCharacterService.PRESENT_CHARACTER_TAG, AutoPresentCharacterService.PRESENT_CHARACTER_START);
				context.startService(intent);
				break;

			case FUNC_PRESENT_BATCH:
				// 保存参数
				savePresentBatchParams();
				intent = new Intent(context, AutoPresentBatchService.class);
				intent.putExtra(AutoPresentBatchService.PRESENT_BATCH_TAG, AutoPresentBatchService.PRESENT_BATCH_START);
				context.startService(intent);
				break;

			default:
				break;
		}
	}

	/**
	 * 保存竞技场参数
	 */
	private void saveArenaParams() {
		String arenaIdStr = editArenaId.getText().toString().trim();
		if (!arenaIdStr.isEmpty()) {
			try {
				AppSettings.setArenaId(getContext(), Integer.parseInt(arenaIdStr));
			} catch (NumberFormatException e) {
				// 忽略无效输入
			}
		}
	}

	/**
	 * 保存卡牌升阶参数
	 */
	private void saveCardUpgradeParams() {
		String cardIds = editCardUpgradeIds.getText().toString().trim();
		AppSettings.setCardUpgradeIdList(getContext(), cardIds);
	}

	/**
	 * 保存批量领取礼物参数
	 */
	private void savePresentBatchParams() {
		String excludeName = editExcludeName.getText().toString().trim();
		AppSettings.setExcludeName(getContext(), excludeName);

		String maxCountStr = editMaxCount.getText().toString().trim();
		if (!maxCountStr.isEmpty()) {
			try {
				AppSettings.setMaxCount(getContext(), Integer.parseInt(maxCountStr));
			} catch (NumberFormatException e) {
				// 忽略无效输入
			}
		}
	}

	// ==================== 状态更新 ====================

	/**
	 * 更新所有状态（聚焦态 + 列表态）
	 */
	private void updateStates() {
		updateFocusState();
		if (currentFocusFunction == null) {
			refreshFunctionList();
		}
	}

	/**
	 * 更新聚焦态的运行状态显示
	 */
	private void updateFocusState() {
		if (currentFocusFunction == null) {
			return;
		}

		String serviceName = funcIdToServiceName(currentFocusFunction);
		if (serviceName != null && serviceName.equals(currentRunningService)) {
			// 当前聚焦的功能正在运行
			String statusText = getString(R.string.status_running);
			if (currentRunningDetail != null) {
				statusText += " (" + currentRunningDetail + ")";
			}
			textFocusStatus.setText(statusText);
			btnFocusStart.setEnabled(false);
			btnFocusStart.setText(R.string.status_running);
			btnBack.setEnabled(false);
		} else if (currentRunningService != null) {
			// 其他功能正在运行
			textFocusStatus.setText(getString(R.string.hint_blocked_by_other, getServiceDisplayName(currentRunningService)));
			btnFocusStart.setEnabled(false);
			btnBack.setEnabled(false);
		} else {
			// 空闲
			textFocusStatus.setText(R.string.status_not_running);
			btnFocusStart.setEnabled(true);
			btnFocusStart.setText(R.string.btn_start);
			btnBack.setEnabled(true);
		}
	}

	/**
	 * 从 SharedPreferences 恢复运行状态
	 */
	private void restoreRunningState() {
		String runningService = AppSettings.getRunningServiceChecked(getContext());
		if (runningService != null) {
			currentRunningService = runningService;
		} else {
			currentRunningService = null;
		}
		currentRunningDetail = null;
		updateStates();
	}

	// ==================== 辅助方法 ====================

	/**
	 * 判断当前聚焦的功能是否正在运行
	 */
	private boolean isCurrentFunctionRunning() {
		if (currentRunningService == null || currentFocusFunction == null) {
			return false;
		}
		return currentRunningService.equals(funcIdToServiceName(currentFocusFunction));
	}

	private void addLog(String text) {
		logAdapter.addFirst(text);
	}

	/**
	 * 功能标识 → 服务名称映射
	 */
	private String funcIdToServiceName(String funcId) {
		switch (funcId) {
			case FUNC_AUTO_LOGIN:
				return MainUIControl.SERVICE_AUTO_LOGIN;
			case FUNC_ARENA_BATTLE:
				return MainUIControl.SERVICE_ARENA;
			case FUNC_CARD_UPGRADE:
				return MainUIControl.SERVICE_CARD_UPGRADE;
			case FUNC_PRESENT_CHARACTER:
				return MainUIControl.SERVICE_PRESENT_CHARACTER;
			case FUNC_PRESENT_BATCH:
				return MainUIControl.SERVICE_PRESENT_BATCH;
			default:
				return null;
		}
	}

	/**
	 * 获取功能的显示名称
	 */
	private String getFunctionDisplayName(String funcId) {
		switch (funcId) {
			case FUNC_AUTO_LOGIN:
				return getString(R.string.action_auto_login);
			case FUNC_ARENA_BATTLE:
				return getString(R.string.action_arena_battle);
			case FUNC_CARD_UPGRADE:
				return getString(R.string.action_card_upgrade);
			case FUNC_PRESENT_CHARACTER:
				return getString(R.string.action_present_character);
			case FUNC_PRESENT_BATCH:
				return getString(R.string.action_present_batch);
			default:
				return funcId;
		}
	}

	/**
	 * 获取服务名称对应的显示名称
	 */
	private String getServiceDisplayName(String serviceName) {
		if (MainUIControl.SERVICE_AUTO_LOGIN.equals(serviceName)) {
			return getString(R.string.action_auto_login);
		} else if (MainUIControl.SERVICE_ARENA.equals(serviceName)) {
			return getString(R.string.action_arena_battle);
		} else if (MainUIControl.SERVICE_CARD_UPGRADE.equals(serviceName)) {
			return getString(R.string.action_card_upgrade);
		} else if (MainUIControl.SERVICE_PRESENT_CHARACTER.equals(serviceName)) {
			return getString(R.string.action_present_character);
		} else if (MainUIControl.SERVICE_PRESENT_BATCH.equals(serviceName)) {
			return getString(R.string.action_present_batch);
		}
		return serviceName;
	}
}
