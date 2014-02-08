package fr.eurecom.cardify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class MainMenu extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	public void lobby(View view) {
		Intent intent = new Intent(this, Lobby.class);
		startActivity(intent);
	}

	public void startSolitaire(View view) {
		Intent intent = new Intent(this, Game.class);
		intent.putExtra("isSolitaire", true);
		this.startActivity(intent);
	}

	@Override
	public void onBackPressed() {
//		Should always exit app when back is pressed in main menu
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

    
    public void showAbout(View view) {
    	Intent intent = new Intent(this, About.class);
    	this.startActivity(intent);
    }
    
}