package ink.snowland.wkuwku.activity;

import android.app.Application;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.common.BaseViewModel;

public class MainViewModel extends BaseViewModel {
    public boolean newVersionChecked = false;
    public MainViewModel(@NonNull Application application) {
        super(application);
    }
}
