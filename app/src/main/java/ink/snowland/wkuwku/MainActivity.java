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
        emulator.powerOn();
        boolean status = emulator.loadGame(null);
        if (status) {
            emulator.next();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}