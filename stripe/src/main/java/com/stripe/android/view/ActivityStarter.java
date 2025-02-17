package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class ActivityStarter
        <TargetActivityType extends Activity, ArgsType extends ActivityStarter.Args> {
    @NonNull private final Activity mActivity;
    @Nullable private final Fragment mFragment;
    @NonNull private final Class<TargetActivityType> mTargetClass;
    @NonNull private final ArgsType mDefaultArgs;
    private final int mRequestCode;

    ActivityStarter(@NonNull Activity activity,
                    @NonNull Class<TargetActivityType> targetClass,
                    @NonNull ArgsType args,
                    int requestCode) {
        mActivity = activity;
        mFragment = null;
        mTargetClass = targetClass;
        mDefaultArgs = args;
        mRequestCode = requestCode;
    }

    ActivityStarter(@NonNull Fragment fragment,
                    @NonNull Class<TargetActivityType> targetClass,
                    @NonNull ArgsType args,
                    int requestCode) {
        mActivity = fragment.requireActivity();
        mFragment = fragment;
        mTargetClass = targetClass;
        mDefaultArgs = args;
        mRequestCode = requestCode;
    }

    public final void startForResult() {
        startForResult(mDefaultArgs);
    }

    public final void startForResult(@NonNull ArgsType args) {
        final Intent intent = new Intent(mActivity, mTargetClass)
                .putExtra(Args.EXTRA, args);

        if (mFragment != null) {
            mFragment.startActivityForResult(intent, mRequestCode);
        } else {
            mActivity.startActivityForResult(intent, mRequestCode);
        }
    }

    public interface Args extends Parcelable {
        String EXTRA = "extra_activity_args";
    }

    public interface Result extends Parcelable {
        String EXTRA = "extra_activity_result";

        @NonNull
        Bundle toBundle();
    }
}
