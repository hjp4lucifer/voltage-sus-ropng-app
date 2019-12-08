package cn.lucifer.voltagesusropngapp.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import cn.lucifer.util.LogUtils;
import cn.lucifer.voltage.sus.auto.AutoLogin;
import cn.lucifer.voltagesusropngapp.util.LogPrinter;

public class AutoLoginService extends Service {

	public static final String AUTO_LOGIN_TAG = "auto_login_tag";

	public static final String AUTO_LOGIN_START = "auto_login_tag_start";

	/**
	 * 电源锁
	 */
	private PowerManager.WakeLock mWakeLock;

	private AutoLogin autoLogin;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(LogPrinter.LOG_TAG, "--------- HttpService onCreate ! ");

		acquireWakeLock();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		if (!AUTO_LOGIN_START.equals(bundle.getString(AUTO_LOGIN_TAG))) {
			return super.onStartCommand(intent, flags, startId);
		}

		if (null == autoLogin) {
			autoLogin = new AutoLogin(10);
			try {
				autoLogin.setUp();
				autoLogin.autoLogin();
				autoLogin.tearDown();
			} catch (Exception e) {
				Log.e("autoLogin", "autoLogin Exception!!!", e);
				LogUtils.error("autoLogin Exception!!!", e);
			}
		}


		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 申请设备电源锁
	 */
	@SuppressLint("InvalidWakeLockTag")
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, "cn.lucifer.voltagesusropngapp.service");
			if (null != mWakeLock) {
				try {
					mWakeLock.acquire();
					Log.i(LogPrinter.LOG_TAG, "mWakeLock acquire! =================");
				} catch (SecurityException e) {
					mWakeLock = null;
				}
			}
		}
	}

	/**
	 * onDestroy时，释放设备电源锁
	 */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			Log.i(LogPrinter.LOG_TAG, "mWakeLock release! =================");
		}
		mWakeLock = null;
	}

	@Override
	public void onDestroy() {
		releaseWakeLock();
	}
}
