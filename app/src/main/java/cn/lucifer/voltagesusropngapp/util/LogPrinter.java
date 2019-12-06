package cn.lucifer.voltagesusropngapp.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.lucifer.util.ILogPrinter;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;

public class LogPrinter implements ILogPrinter {

	public static final String LOG_RECEIVER_ACTION = "lcf.ui.Log";
	public static final String EXTRA_LOG_NAME = "log";

	private Context context;

	public LogPrinter(Context context) {
		this.context = context;
	}

	@Override
	public void println(String x) {
		Log.d("lcf", x);
		if (context == null) {
			return;
		}
		Intent intent = new Intent(LOG_RECEIVER_ACTION);
		intent.putExtra(EXTRA_LOG_NAME, x);
		context.sendBroadcast(intent);
	}
}
