package com.enmar.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int APPWIDGET_HOST_ID = 1024;
    private static final int REQUEST_PICK_APPWIDGET = 101;
    private static final int REQUEST_CREATE_APPWIDGET = 102;
    private static final String PREFS_NAME = "enmar_launcher_prefs";
    private static final String KEY_WIDGET_IDS = "widget_ids";

    private AppWidgetManager appWidgetManager;
    private LauncherWidgetHost appWidgetHost;
    private LinearLayout widgetContainer;
    private LinearLayout homeScreen;
    private LinearLayout appDrawer;
    private EditText searchInput;
    private AppAdapter appAdapter;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetHost = new LauncherWidgetHost(this, APPWIDGET_HOST_ID);

        widgetContainer = findViewById(R.id.widget_container);
        homeScreen = findViewById(R.id.home_screen);
        appDrawer = findViewById(R.id.app_drawer);
        searchInput = findViewById(R.id.search_input);

        Button btnAddWidget = findViewById(R.id.btn_add_widget);
        Button btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        Button btnCloseDrawer = findViewById(R.id.btn_close_drawer);
        GridView appsGrid = findViewById(R.id.apps_grid);

        appAdapter = new AppAdapter(this);
        appsGrid.setAdapter(appAdapter);

        btnAddWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWidget();
            }
        });

        btnOpenDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer();
            }
        });

        btnCloseDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });

        appsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo app = appAdapter.getItem(position);
                launchApp(app);
            }
        });

        appsGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo app = appAdapter.getItem(position);
                showAppOptions(app);
                return true;
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        loadApps();
        restoreSavedWidgets();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            appWidgetHost.startListening();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            appWidgetHost.stopListening();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (appDrawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
        }
    }

    @Override
    public void onBackPressed() {
        if (appDrawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
        }
    }

    private void openDrawer() {
        appDrawer.setVisibility(View.VISIBLE);
        homeScreen.setVisibility(View.GONE);
    }

    private void closeDrawer() {
        hideKeyboard();
        searchInput.setText("");
        appDrawer.setVisibility(View.GONE);
        homeScreen.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchInput != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolvedList = pm.queryIntentActivities(mainIntent, 0);
        List<AppInfo> apps = new ArrayList<>();
        String myPackage = getPackageName();

        for (ResolveInfo ri : resolvedList) {
            String pkg = ri.activityInfo.packageName;
            if (myPackage.equals(pkg)) continue;
            String label = ri.loadLabel(pm).toString();
            apps.add(new AppInfo(
                    label,
                    pkg,
                    ri.activityInfo.name,
                    ri.loadIcon(pm)
            ));
        }

        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        appAdapter.setApps(apps);
    }

    private void launchApp(AppInfo app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(app.packageName, app.activityName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
            closeDrawer();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot launch " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppOptions(final AppInfo app) {
        String[] options = new String[]{
                getString(R.string.app_info),
                getString(R.string.uninstall)
        };
        new AlertDialog.Builder(this)
                .setTitle(app.label)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + app.packageName));
                            startActivity(intent);
                        } else if (which == 1) {
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.parse("package:" + app.packageName));
                            startActivity(intent);
                        }
                    }
                })
                .show();
    }

    private void selectWidget() {
        int appWidgetId = appWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, new ArrayList<AppWidgetProviderInfo>());
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, new ArrayList<Bundle>());
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        if (extras == null) return;
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo != null && appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        if (extras == null) return;
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) return;

        saveWidgetId(appWidgetId);
        attachWidgetView(appWidgetId);
    }

    private void attachWidgetView(final int appWidgetId) {
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            removeWidgetId(appWidgetId);
            return;
        }

        final AppWidgetHostView hostView = appWidgetHost.createView(getApplicationContext(), appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int minHeightPx = Math.max(appWidgetInfo.minHeight, (int) (72 * dm.density));

        final FrameLayout wrapper = new FrameLayout(this);
        wrapper.setBackgroundResource(R.drawable.bg_widget_card);
        int pad = (int) (6 * dm.density);
        wrapper.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                minHeightPx + pad * 2
        );
        wrapperParams.bottomMargin = (int) (10 * dm.density);
        wrapper.setLayoutParams(wrapperParams);

        FrameLayout.LayoutParams hostParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        wrapper.addView(hostView, hostParams);

        hostView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                promptRemoveWidget(appWidgetId, wrapper);
                return true;
            }
        });

        widgetContainer.addView(wrapper);
    }

    private void promptRemoveWidget(final int appWidgetId, final View wrapper) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_widget_title)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        widgetContainer.removeView(wrapper);
                        appWidgetHost.deleteAppWidgetId(appWidgetId);
                        removeWidgetId(appWidgetId);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveWidgetId(int id) {
        List<Integer> ids = getSavedWidgetIds();
        if (!ids.contains(id)) {
            ids.add(id);
            writeWidgetIds(ids);
        }
    }

    private void removeWidgetId(int id) {
        List<Integer> ids = getSavedWidgetIds();
        ids.remove(Integer.valueOf(id));
        writeWidgetIds(ids);
    }

    private List<Integer> getSavedWidgetIds() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(KEY_WIDGET_IDS, "");
        List<Integer> ids = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(",")) {
                try {
                    ids.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private void writeWidgetIds(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_WIDGET_IDS, sb.toString())
                .apply();
    }

    private void restoreSavedWidgets() {
        widgetContainer.removeAllViews();
        List<Integer> ids = getSavedWidgetIds();
        for (int id : ids) {
            attachWidgetView(id);
        }
    }
}
