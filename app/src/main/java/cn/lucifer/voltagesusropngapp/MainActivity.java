package cn.lucifer.voltagesusropngapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import cn.lucifer.voltagesusropngapp.adt.LogAdapter;

public class MainActivity extends AppCompatActivity {

	protected ListView listView_log;
	protected LogAdapter logAdapter;

	private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
			= new BottomNavigationView.OnNavigationItemSelectedListener() {

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.navigation_home:
					logAdapter.addFirst("title_home");
					return true;
				case R.id.navigation_dashboard:
					logAdapter.addFirst("title_dashboard");
					return true;
				case R.id.navigation_notifications:
					logAdapter.addFirst("title_notifications");
					return true;
				default:
					break;
			}
			return false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		listView_log = (ListView) findViewById(R.id.listView_log);
		logAdapter = new LogAdapter(this);
		listView_log.setAdapter(logAdapter);

		BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
	}

}
