package cn.lucifer.voltagesusropngapp.adt;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedList;

public class LogAdapter extends BaseAdapter {

	protected Context context;
	protected LinkedList<String> msgList = new LinkedList<>();

	public LogAdapter(Context context) {
		this.context = context;
		msgList.add("一切从【菜单】键开始 in sdop —— 沧之云！ License: GPL v2.");
	}

	protected Spanned getText(String text) {
		return Html.fromHtml(text);
	}

	private final int padding = 5;
	private final int maxCount = 50;
	private boolean oddCount;

	public void addFirst(String text) {
		msgList.addFirst(text);
		oddCount = !oddCount;
		if (getCount() > maxCount) {
			msgList.removeLast();
		}
		notifyDataSetChanged();
	}

	public void addLast(String text) {
		msgList.addLast(text);
		if (getCount() > maxCount) {
			msgList.removeFirst();
		}
		notifyDataSetChanged();
	}

	public void clear() {
		msgList.clear();
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String text = getItem(position);
		TextView tv;
		if (convertView != null && convertView instanceof TextView) {
			tv = (TextView) convertView;
			setStyle(tv, position, text);
			return tv;
		}
		tv = new TextView(context);
		tv.setPadding(padding, padding, padding, padding);
		setStyle(tv, position, text);
		return tv;
	}

	protected void setStyle(TextView tv, int position, String text) {
		tv.setBackgroundColor(position % 2 == (oddCount ? 0 : 1) ? Color.LTGRAY
				: Color.TRANSPARENT);
		tv.setText(getText(text));
	}

	@Override
	public int getCount() {
		return msgList.size();
	}

	@Override
	public String getItem(int arg0) {
		// TODO Auto-generated method stub
		return msgList.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
