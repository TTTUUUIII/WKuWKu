package ink.snowland.wkuwku;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import ink.snowland.wkuwku.interfaces.Emulator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Emulator emulator = EmulatorManager.getEmulator(EmulatorManager.NES);
        boolean loaded = emulator.load("/sdcard/Android/data/ink.snowland.wkuwku/cache/Super Mario USA (J) [!].nes");
        System.out.println(loaded);
        if (loaded) {
            emulator.run();
        }
        emulator.suspend();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}