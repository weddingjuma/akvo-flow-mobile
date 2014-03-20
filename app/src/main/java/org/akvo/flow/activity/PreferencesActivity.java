/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.service.LocationService;
import org.akvo.flow.util.ArrayPreferenceUtil;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * Displays user editable preferences and takes care of persisting them to the
 * database. Some options require the user to enter an administrator passcode
 * via a dialog box before the operation can be performed.
 * 
 * @author Christopher Fagiani
 */
public class PreferencesActivity extends Activity implements OnClickListener,
        OnCheckedChangeListener {
    private CheckBox saveUserCheckbox;
    private CheckBox beaconCheckbox;
    private CheckBox screenOnCheckbox;
    private CheckBox mobileDataCheckbox;
    private TextView languageTextView;
    private TextView serverTextView;
    private TextView identTextView;
    private TextView maxImgSizeTextView;
    private View prefLocaleView;
    private TextView localeTextView;

    private SurveyDbAdapter database;

    private LangsPreferenceData langsPrefData;
    private String[] langsSelectedNameArray;
    private boolean[] langsSelectedBooleanArray;
    private int[] langsSelectedMasterIndexArray;

    private String[] serverArray;
    private String[] maxImgSizes;
    private PropertyUtil props;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.preferences);

        saveUserCheckbox = (CheckBox) findViewById(R.id.lastusercheckbox);
        beaconCheckbox = (CheckBox) findViewById(R.id.beaconcheckbox);
        screenOnCheckbox = (CheckBox) findViewById(R.id.screenoptcheckbox);
        mobileDataCheckbox = (CheckBox) findViewById(R.id.uploadoptioncheckbox);
        languageTextView = (TextView) findViewById(R.id.surveylangvalue);
        serverTextView = (TextView) findViewById(R.id.servervalue);
        identTextView = (TextView) findViewById(R.id.identvalue);
        maxImgSizeTextView = (TextView) findViewById(R.id.max_img_size_txt);
        prefLocaleView = findViewById(R.id.pref_locale);
        localeTextView = (TextView) prefLocaleView.findViewById(R.id.locale_name);

        Resources res = getResources();
        props = new PropertyUtil(res);

        serverArray = res.getStringArray(R.array.servers);
        maxImgSizes = res.getStringArray(R.array.max_image_size_pref);
    }

    /**
     * loads the preferences from the DB and sets their current value in the UI
     */
    private void populateFields() {
        HashMap<String, String> settings = database.getPreferences();
        String val = settings.get(ConstantUtil.USER_SAVE_SETTING_KEY);
        if (val != null && Boolean.parseBoolean(val)) {
            saveUserCheckbox.setChecked(true);
        } else {
            saveUserCheckbox.setChecked(false);
        }

        val = settings.get(ConstantUtil.SCREEN_ON_KEY);
        if (val != null && Boolean.parseBoolean(val)) {
            screenOnCheckbox.setChecked(true);
        } else {
            screenOnCheckbox.setChecked(false);
        }

        val = settings.get(ConstantUtil.LOCATION_BEACON_SETTING_KEY);
        if (val != null && Boolean.parseBoolean(val)) {
            beaconCheckbox.setChecked(true);
        } else {
            beaconCheckbox.setChecked(false);
        }

        val = settings.get(ConstantUtil.CELL_UPLOAD_SETTING_KEY);
        mobileDataCheckbox.setChecked(val != null && Boolean.parseBoolean(val));

        val = settings.get(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = settings.get(ConstantUtil.SURVEY_LANG_PRESENT_KEY);
        langsPrefData = LangsPreferenceUtil.createLangPrefData(this, val, langsPresentIndexes);

        languageTextView.setText(ArrayPreferenceUtil.formSelectedItemString(
                langsPrefData.getLangsSelectedNameArray(),
                langsPrefData.getLangsSelectedBooleanArray()));

        val = settings.get(ConstantUtil.SERVER_SETTING_KEY);
        if (val != null && val.trim().length() > 0) {
            serverTextView.setText(serverArray[Integer.parseInt(val)]);
        } else {
            serverTextView.setText(props.getProperty(ConstantUtil.SERVER_BASE));
        }

        val = settings.get(ConstantUtil.MAX_IMG_SIZE);
        if (val != null && val.trim().length() > 0) {
            maxImgSizeTextView.setText(maxImgSizes[Integer.parseInt(val)]);
        } else {
            maxImgSizeTextView.setText(maxImgSizes[0]);
        }

        val = settings.get(ConstantUtil.DEVICE_IDENT_KEY);
        if (val != null) {
            identTextView.setText(val);
        }

        localeTextView.setText(FlowApp.getApp().getAppDisplayLanguage());
    }

    /**
     * opens db connection and sets up listeners (after we hydrate values so we
     * don't trigger the onCheckChanged listener when we set initial values)
     */
    public void onResume() {
        super.onResume();
        database = new SurveyDbAdapter(this);
        database.open();
        populateFields();
        // TODO: this listeners assignations should be moved to onCreate()
        saveUserCheckbox.setOnCheckedChangeListener(this);
        beaconCheckbox.setOnCheckedChangeListener(this);
        screenOnCheckbox.setOnCheckedChangeListener(this);
        mobileDataCheckbox.setOnCheckedChangeListener(this);
        findViewById(R.id.surveylangbutton).setOnClickListener(this);
        findViewById(R.id.serverbutton).setOnClickListener(this);
        findViewById(R.id.identbutton).setOnClickListener(this);
        findViewById(R.id.max_img_size_btn).setOnClickListener(this);
        prefLocaleView.setOnClickListener(this);
    }

    public void onPause() {
        database.close();
        super.onPause();
    }

    /**
     * displays a pop-up dialog containing the upload or language options
     * depending on what was clicked
     */
    @Override
    public void onClick(View v) {
        if (R.id.surveylangbutton == v.getId()) {
            langsSelectedNameArray = langsPrefData.getLangsSelectedNameArray();
            langsSelectedBooleanArray = langsPrefData.getLangsSelectedBooleanArray();
            langsSelectedMasterIndexArray = langsPrefData.getLangsSelectedMasterIndexArray();

            ViewUtil.displayLanguageSelector(this, langsSelectedNameArray,
                    langsSelectedBooleanArray,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int clicked) {
                            database.savePreference(
                                    ConstantUtil.SURVEY_LANG_SETTING_KEY,
                                    LangsPreferenceUtil
                                            .formLangPreferenceString(langsSelectedBooleanArray,
                                                    langsSelectedMasterIndexArray));

                            languageTextView.setText(ArrayPreferenceUtil
                                    .formSelectedItemString(langsSelectedNameArray,
                                            langsSelectedBooleanArray));
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        }
                    });
        } else if (R.id.pref_locale == v.getId()) {
            showLanguageDialog();
        } else if (R.id.serverbutton == v.getId()) {
            ViewUtil.showAdminAuthDialog(this,
                    new ViewUtil.AdminAuthDialogListener() {
                        @Override
                        public void onAuthenticated() {
                            // We'll show a first options containing the default server,
                            // but it's value will be an empty string "".
                            String[] keys = new String[serverArray.length + 1];
                            String[] values = new String[serverArray.length + 1];
                            keys[0] = "";
                            values[0] = props.getProperty(ConstantUtil.SERVER_BASE);
                            for (int i=0; i<serverArray.length; i++) {
                                keys[i+1] = String.valueOf(i);// DB value
                                values[i+1] = serverArray[i];// Text to show
                            }
                            showPreferenceDialogBase(R.string.serverlabel,
                                    ConstantUtil.SERVER_SETTING_KEY,
                                    keys, values, serverTextView);

                        }
                    });
        } else if (R.id.identbutton == v.getId()) {
            ViewUtil.showAdminAuthDialog(this,
                    new ViewUtil.AdminAuthDialogListener() {
                        @Override
                        public void onAuthenticated() {
                            final EditText inputView = new EditText(PreferencesActivity.this);
                            // one line only
                            inputView.setSingleLine();
                            ViewUtil.ShowTextInputDialog(
                                    PreferencesActivity.this,
                                    R.string.identlabel,
                                    R.string.setidentlabel, inputView,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String s = StringUtil.ControlToSPace(inputView
                                                    .getText().toString());
                                            // drop any control chars,
                                            // especially tabs
                                            identTextView.setText(s);
                                            database.savePreference(
                                                    ConstantUtil.DEVICE_IDENT_KEY, s);
                                        }
                                    }
                            );
                        }
                    }
            );
        } else if (R.id.max_img_size_btn == v.getId()) {
            String[] keys = new String[maxImgSizes.length];
            for (int i = 0; i < maxImgSizes.length; i++) {
                keys[i] = String.valueOf(i);
            }
            showPreferenceDialogBase(R.string.resize_large_images,// TODO: change string
                    ConstantUtil.MAX_IMG_SIZE,
                    keys, maxImgSizes, maxImgSizeTextView);
        }
    }

    /**
     * displays a dialog that allows the user to choose a setting from a string
     * array
     * 
     * @param titleId - resource id of dialog title
     * @param settingKey - key of setting to edit
     * @param keys - string array containing keys (to be stored in the DB)
     * @param values - string array containing values (text mapping of the key)
     * @param currentValView - view to update with value selected
     */
    private void showPreferenceDialogBase(int titleId,  final String settingKey,
            final String[] keys, final String[] values, final TextView currentValView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId).setItems(values,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database.savePreference(settingKey, keys[which]);
                        currentValView.setText(values[which]);
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
        builder.show();
    }

    /**
     * saves the value of the checkbox to the database
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == saveUserCheckbox) {
            database.savePreference(ConstantUtil.USER_SAVE_SETTING_KEY, ""
                    + isChecked);
        } else if (buttonView == beaconCheckbox) {
            database.savePreference(ConstantUtil.LOCATION_BEACON_SETTING_KEY, "" + isChecked);
            if (isChecked) {
                // if the option changed, kick the service so it reflects the change
                startService(new Intent(this, LocationService.class));
            } else {
                stopService(new Intent(this, LocationService.class));
            }
        } else if (buttonView == screenOnCheckbox) {
            database.savePreference(ConstantUtil.SCREEN_ON_KEY, "" + isChecked);
        } else if (buttonView == mobileDataCheckbox) {
            database.savePreference(ConstantUtil.CELL_UPLOAD_SETTING_KEY, "" + isChecked);
        }
    }

    private void showLanguageDialog() {
        final String[] languageCodes = getResources().getStringArray(R.array.app_language_codes);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_language).setItems(R.array.app_languages,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FlowApp.getApp().setAppLanguage(languageCodes[which], true);
                    }
                });
        builder.show();
    }

}
