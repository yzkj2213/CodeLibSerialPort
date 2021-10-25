package com.izis.serialport.connect;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Collection;

public class PermissionsFragment extends Fragment {
    private Callback startActivityForResultListener;
    private Callback requestPermissionsListener;
    private final ActivityResultLauncher<Intent> forActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (startActivityForResultListener == null) return;
        if (result.getResultCode() == Activity.RESULT_OK) {
            startActivityForResultListener.onSuccess();
        } else {
            startActivityForResultListener.onFail();
        }
    });
    private final ActivityResultLauncher<String[]> requestPermissionsResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        boolean success = true;
        Collection<Boolean> values = result.values();
        for (Boolean item : values) {
            if (!item) {
                success = false;
                break;
            }
        }

        if (requestPermissionsListener == null) return;
        if (success) {
            requestPermissionsListener.onSuccess();
        } else {
            requestPermissionsListener.onFail();
        }
    });

    public PermissionsFragment() {
    }

    static PermissionsFragment getInstance(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        PermissionsFragment permission_fragment = (PermissionsFragment) fragmentManager.findFragmentByTag("permission_fragment");
        if (permission_fragment == null){
            permission_fragment = new PermissionsFragment();
            fragmentManager.beginTransaction().add(0, permission_fragment, "permission_fragment").commitNow();
        }
        return permission_fragment;
    }

    public PermissionsFragment setRequestPermissionsListener(Callback requestPermissionsListener) {
        this.requestPermissionsListener = requestPermissionsListener;
        return this;
    }

    public PermissionsFragment setStartActivityForResultListener(Callback startActivityForResultListener) {
        this.startActivityForResultListener = startActivityForResultListener;
        return this;
    }

    void _requestPermissions(@NonNull String[] permissions) {
        requestPermissionsResult.launch(permissions);
    }

    void _startActivityForResult(Intent intent) {
        forActivityResult.launch(intent);
    }
}

interface Callback {
    void onSuccess();

    void onFail();
}
