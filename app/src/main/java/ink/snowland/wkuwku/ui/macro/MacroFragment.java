package ink.snowland.wkuwku.ui.macro;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.common.BaseFragment;
import ink.snowland.wkuwku.databinding.FragmentMacroBinding;
import ink.snowland.wkuwku.widget.MacroEditDialog;

public class MacroFragment extends BaseFragment implements View.OnClickListener {
    private MacroViewModel mViewModel;
    private FragmentMacroBinding binding;
    private MacroEditDialog mMacroEditDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MacroViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMacroBinding.inflate(getLayoutInflater());
        binding.fab.setOnClickListener(this);
        parentActivity.setActionbarTitle(R.string.key_macro_manage);
        return binding.getRoot();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.fab) {
            if (mMacroEditDialog == null)
                mMacroEditDialog = new MacroEditDialog(parentActivity);
            mMacroEditDialog.show(script -> {
                System.out.println(script);
            });
        }
    }
}