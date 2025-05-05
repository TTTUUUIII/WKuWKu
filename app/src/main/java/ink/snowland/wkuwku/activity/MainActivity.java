package ink.snowland.wkuwku.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseActivity;
import ink.snowland.wkuwku.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {
    private NavController mNavController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.main);
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert fragment != null;
        mNavController = fragment.getNavController();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            NavDestination destination = mNavController.getCurrentDestination();
            if (destination != null && destination.getId() != R.id.settings_fragment) {
                mNavController.navigate(R.id.settings_fragment);
            }
        }
        return true;
    }
}