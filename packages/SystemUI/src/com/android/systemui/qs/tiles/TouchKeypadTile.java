/*
 * Copyright (C) 2020-2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.service.quicksettings.Tile;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;

import org.lineageos.internal.logging.LineageMetricsLogger;

import vendor.blackberry.touchkeypad.ITouchKeypad;

import javax.inject.Inject;

public class TouchKeypadTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "touchkeypad";

    private ITouchKeypad mTouchKeypad;

    @Inject
    public TouchKeypadTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mTouchKeypad = getTouchKeypad();
        if (mTouchKeypad == null) {
            return;
        }
    }

    @Override
    public void refreshState() {
        updateTouchKeypadState();

        super.refreshState();
    }

    private void updateTouchKeypadState() {
        if (!isAvailable()) {
            return;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Will throw NPE or some other exception if HAL is either missing or broken.
            mTouchKeypad.isEnabled();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    public void handleClick(@Nullable Expandable expandable) {
        try {
            mTouchKeypad.setEnabled(!mTouchKeypad.isEnabled());
            refreshState();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_touchkeypad_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (!isAvailable()) {
            return;
        }

        state.icon = ResourceIcon.get(R.drawable.ic_qs_touchkeypad);
        state.hasLongClickEffect = false;
        try {
            state.value = mTouchKeypad.isEnabled();
            if (state.value != true)
                state.state = Tile.STATE_INACTIVE;
            else
                state.state = Tile.STATE_ACTIVE;
        } catch (Exception ex) {
            state.value = false;
            ex.printStackTrace();
        }
        state.label = mContext.getString(R.string.quick_settings_touchkeypad_label);
    }

    @Override
    public int getMetricsCategory() {
        return LineageMetricsLogger.TILE_POWERSHARE;
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    private synchronized ITouchKeypad getTouchKeypad() {
        final String fqName = ITouchKeypad.DESCRIPTOR + "/default";

        try {
            return ITouchKeypad.Stub.asInterface(ServiceManager.getService(fqName));
        } catch (Exception e) {
            // Handle both RemoteException and ServiceNotFoundException
            Log.e(TAG, "Failed to get TouchKeypad service", e);
            return null;
        }
    }
}
