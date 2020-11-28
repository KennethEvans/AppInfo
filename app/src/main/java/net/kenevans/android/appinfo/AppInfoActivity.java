package net.kenevans.android.appinfo;

//Copyright (c) 2011 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to display information about installed apps.
 */
public class AppInfoActivity extends AppCompatActivity implements IConstants {
    /**
     * Template for the name of the file written to the root of the SD card
     */
    private static final String sdCardFileNameTemplate = "ApplicationInfo.%s" +
            ".txt";
    private static final String SD_CARD_SUGGESTED_DIR = "Application Info";

    private TextView mTextView;
    public boolean mDoBuildInfo = false;
    public boolean mDoMemoryInfo = false;
    public boolean mDoNonSystemApps = true;
    public boolean mDoSystemApps = false;
    public boolean mDoPreferredApplications = false;
    public boolean mDoPermissions = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Get the TextView
        mTextView = findViewById(R.id.textview);
        // Make it scroll

        // refresh will be called in onResume
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_app_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refresh) {
            refresh();
            return true;
        } else if (id == R.id.copy) {
            copyToClipboard();
            return true;
        } else if (id == R.id.save) {
            save();
            return true;
        }
        if (id == R.id.settings) {
            setOptions();
            return true;
        } else if (id == R.id.help) {
            // DEBUG install issue
            // test();
            showHelp();
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putBoolean("mDoBuildInfo", mDoBuildInfo);
        editor.putBoolean("mDoMemoryInfo", mDoMemoryInfo);
        editor.putBoolean("mDoPreferredApplications", mDoPreferredApplications);
        editor.putBoolean("mDoNonSystemApps", mDoNonSystemApps);
        editor.putBoolean("mDoSystemApps", mDoSystemApps);
        editor.putBoolean("mDoPermissions", mDoPermissions);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mDoBuildInfo = prefs.getBoolean("mDoBuildInfo", mDoBuildInfo);
        mDoMemoryInfo = prefs.getBoolean("mDoMemoryInfo", mDoMemoryInfo);
        mDoPreferredApplications = prefs.getBoolean("mDoPreferredApplications",
                mDoPreferredApplications);
        mDoNonSystemApps = prefs.getBoolean("mDoNonSystemApps",
                mDoNonSystemApps);
        mDoSystemApps = prefs.getBoolean("mDoSystemApps", mDoSystemApps);
        mDoPermissions = prefs.getBoolean("mDoPermissions", mDoPermissions);

        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // DEBUG
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onActivityResult: requestCode=" + requestCode
                + " resultCode=" + resultCode);
        if (requestCode == CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (intent != null) {
                uri = intent.getData();
                List<String> segments = uri.getPathSegments();
                Uri.Builder builder = new Uri.Builder();
                for (int i = 0; i < segments.size() - 1; i++) {
                    builder.appendPath(segments.get(i));
                }
                Uri parent = builder.build();
                Log.d(TAG, "uri=" + uri + " parent=" + parent);
                doSave(uri);
            }
        }
    }

    /**
     * Asks for the name of the save file
     */
    private void save() {
        try {
            Date now = new Date();
            String format = "yyyy-MM-dd-HHmmss";
            SimpleDateFormat formatter = new SimpleDateFormat(format,
                    Locale.US);
            String fileName = String.format(sdCardFileNameTemplate,
                    formatter.format(now));

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            // Set initial directory
            if (Build.VERSION.SDK_INT >= 28) {
                File sdCardRoot = Environment.getExternalStorageDirectory();
                File dir = new File(sdCardRoot, SD_CARD_SUGGESTED_DIR);
                Uri.Builder builder = new Uri.Builder();
                builder.path(sdCardRoot.getPath())
                        .appendPath(SD_CARD_SUGGESTED_DIR);
                Uri uri = builder.build();
                Uri uriFile = Uri.fromFile(dir);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        uri);
                intent.putExtra(DocumentsContract.EXTRA_PROMPT,
                        "This is a prompt");
                intent.putExtra(DocumentsContract.EXTRA_INFO,
                        "Some extra info yu should know");
                Log.d(TAG, this.getClass().getSimpleName()
                        + ".save: uri=" + uri
                        + " uriFile=" + uriFile);
            }
            startActivityForResult(intent, CREATE_DOCUMENT);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error requesting saving to SD card", ex);
        }
    }

    /**
     * Saves the info to the SD card
     *
     * @param uri The Uri to use for writing.
     */
    private void doSave(Uri uri) {
        FileOutputStream writer = null;
        try {
            Charset charset = StandardCharsets.UTF_8;
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "w");
            writer =
                    new FileOutputStream(pfd.getFileDescriptor());
            CharSequence charSeq = mTextView.getText();
            byte[] bytes = charSeq.toString().getBytes(charset);
            writer.write(bytes);
            if (charSeq.length() == 0) {
                Utils.warnMsg(this, "The file written is empty");
            }
            Utils.infoMsg(this, "Wrote " + uri.getPath());
        } catch (Exception ex) {
            Utils.excMsg(this, "Error saving to SD card", ex);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception ex) {
                // Do nothing
            }
        }
    }

    /**
     * Updates the application information. First sets a processing message,
     * then calls an asynchronous task to get the information. Getting the
     * information can take a long time, and there is no user indication
     * something is happening otherwise.
     */
    private void refresh() {
        try {
            mTextView.setText(R.string.processing_msg);
            new RefreshTask().execute();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error in Refresh", ex);
        }
    }

    /**
     * Method that gets the application info to be displayed.
     */
    private String getText() {
        String info;
        try {
            info = getAppsInfo();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error in asyncRefresh", ex);
            info = "Error in asyncRefresh\n" + ex.getMessage();
        }
        return info;
    }

    /**
     * An asynchronous task to get the application info, which can take a long
     * time.
     */
    private class RefreshTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return getText();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mTextView.setText(result);
            // DEBUG
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            Utils.infoMsg(AppInfoActivity.this, dm.toString() + "\n" +
// mTextView.getWidth() + " x " + mTextView.getHeight());
        }
    }

    /**
     * Copies the contents of the application information view to the clipboard.
     */
    private void copyToClipboard() {
        try {
            android.content.ClipboardManager cm = (android.content
                    .ClipboardManager) getSystemService
                    (CLIPBOARD_SERVICE);
            TextView tv = findViewById(R.id.textview);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("AppInfo", tv.getText());
            cm.setPrimaryClip(clip);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error setting Clipboard", ex);
        }
    }

    /**
     * Bring up a dialog to change the options.
     */
    private void setOptions() {
        final CharSequence[] items = {"Build Information",
                "Memory Information", "Preferred Applications",
                "Downloaded Applications", "System Applications",
                "Permissions"};
        boolean[] states = {mDoBuildInfo, mDoMemoryInfo,
                mDoPreferredApplications, mDoNonSystemApps, mDoSystemApps,
                mDoPermissions};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setMultiChoiceItems(items, states,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialogInterface,
                                        int item, boolean state) {
                    }
                });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SparseBooleanArray checked = ((AlertDialog) dialog)
                        .getListView().getCheckedItemPositions();
                mDoBuildInfo = checked.get(0);
                mDoMemoryInfo = checked.get(1);
                mDoPreferredApplications = checked.get(2);
                mDoNonSystemApps = checked.get(3);
                mDoSystemApps = checked.get(4);
                mDoPermissions = checked.get(5);
                refresh();
            }
        });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    /**
     * Show the help.
     */
    private void showHelp() {
        try {
            // Start theInfoActivity
            Intent intent = new Intent();
            intent.setClass(this, InfoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(INFO_URL, "file:///android_asset/appinfo.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error showing Help", ex);
        }
    }

    /**
     * Gets the Build information.
     *
     * @return The build information.
     */
    private String getBuildInfo() {
        AtomicReference<String> info = new AtomicReference<>(("VERSION" +
                ".RELEASE=" + Build.VERSION.RELEASE + "\n") +
                "VERSION.INCREMENTAL=" + Build.VERSION.INCREMENTAL + "\n" +
                "VERSION.SDK=" + Build.VERSION.SDK_INT + "\n" +
                "BOARD=" + Build.BOARD + "\n" +
                "BRAND=" + Build.BRAND + "\n" +
                "DEVICE=" + Build.DEVICE + "\n" +
                "FINGERPRINT=" + Build.FINGERPRINT + "\n" +
                "HOST=" + Build.HOST + "\n" +
                "ID=" + Build.ID + "\n");
        return info.get();
    }

    /**
     * Gets the Memory information.
     *
     * @return The memory information.
     */
    public String getMemoryInfo() {
        StringBuilder builder = new StringBuilder();

        // Internal Memory
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;
        double total, available, free;
        blockSize = stat.getBlockSizeLong();
        total = (double) stat.getBlockCountLong() * blockSize;
        available = (double) stat.getAvailableBlocksLong() * blockSize;
        free = (double) stat.getFreeBlocksLong() * blockSize;
        double used = total - available;
        String format = ": %.0f KB = %.2f MB = %.2f GB\n";
        builder.append("Internal Memory\n");
        builder.append(String.format(Locale.US, "  Total" + format, total *
                        KB, total * MB,
                total * GB));
        builder.append(String.format(Locale.US, "  Used" + format, used * KB,
                used * MB,
                used
                        * GB));
        builder.append(String.format(Locale.US, "  Available" + format,
                available * KB,
                available * MB, available * GB));
        builder.append(String.format(Locale.US, "  Free" + format, free * KB,
                free * MB,
                free
                        * GB));
        builder.append(String.format(Locale.US, "  Block Size: %d Bytes\n",
                blockSize));

        // External Memory
        builder.append("\nExternal Memory\n");
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            builder.append("  No External Memory\n");
        } else {
            path = Environment.getExternalStorageDirectory();
            stat = new StatFs(path.getPath());
            blockSize = stat.getBlockSizeLong();
            total = (double) stat.getBlockCountLong() * blockSize;
            available = (double) stat.getAvailableBlocksLong() * blockSize;
            free = (double) stat.getFreeBlocksLong() * blockSize;
            used = total - available;
            builder.append(String.format(Locale.US, "  Total" + format, total
                            * KB,
                    total * MB, total * GB));
            builder.append(String.format(Locale.US, "  Used" + format, used *
                            KB, used *
                            MB,
                    used * GB));
            builder.append(String.format(Locale.US, "  Available" + format,
                    available * KB,
                    available * MB, available * GB));
            builder.append(String.format(Locale.US, "  Free" + format, free *
                            KB, free *
                            MB,
                    free * GB));
            builder.append(String.format(Locale.US, "  Block Size: %d Bytes\n",
                    blockSize));
        }

        // RAM
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService
                (ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        available = (double) mi.availMem;
        double threshold = (double) mi.threshold;
        boolean low = mi.lowMemory;
        builder.append("\nRAM\n");
        Long ram = getTotalRAM();
        if (ram == null) {
            builder.append("  Total: Not found\n");
        } else {
            total = ram / KB;
            used = total - available;
            builder.append(String.format(Locale.US, "  Total" + format,
                    total * KB,
                    total * MB, total * GB));
            builder.append(String.format(Locale.US, "  Used" + format,
                    used * KB, used *
                            MB,
                    used * GB));
        }
        builder.append(String.format(Locale.US, "  Available" + format,
                available * KB,
                available * MB, available * GB));
        builder.append(String.format(Locale.US, "  Threshold" + format,
                threshold * KB,
                threshold * MB, threshold * GB));
        if (low) {
            builder.append("  Memory is low\n");
        }

        return builder.toString();
    }

    /**
     * Gets the total ram by parsing /proc/meminfo.
     *
     * @return The memory in KB or null on failure.
     */
    public static Long getTotalRAM() {
        String path = "/proc/meminfo";
        String[] tokens;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            // Assume it is in the first line and assume it is in kb
            String line = br.readLine();
            tokens = line.split("\\s+");
            return Long.parseLong(tokens[1]);
        } catch (Exception ex) {
            return null;
        }
        // Do nothing
    }

    // /**
    // * Get info on the preferred (launch by default) applications.
    // *
    // * @return
    // */
    // public String getPreferredAppInfo() {
    // List<PackageInfo> packages = getPackageManager()
    // .getInstalledPackages(0);
    // List<IntentFilter> filters = new ArrayList<IntentFilter>();
    // List<ComponentName> activities = new ArrayList<ComponentName>();
    // String info = "";
    // int nPref = 0, nFilters = 0, nActivities = 0;
    // PackageInfo pkg = null;
    // for (int i = 0; i < packages.size(); i++) {
    // pkg = packages.get(i);
    // nPref = getPackageManager().getPreferredActivities(filters,
    // activities, pkg.packageName);
    // nFilters = filters.size();
    // nActivities = activities.size();
    // if (nPref > 0 || nFilters > 0 || nActivities > 0) {
    // // This is a launch by default package
    // info += "\n" + pkg.packageName + "\n";
    // for (IntentFilter filter : filters) {
    // info += "IntentFilter:\n";
    // for (int j = 0; j < filter.countActions(); j++) {
    // info += "    action: " + filter.getAction(j) + "\n";
    // }
    // for (int j = 0; j < filter.countCategories(); j++) {
    // info += "    category: " + filter.getCategory(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataTypes(); j++) {
    // info += "    type: " + filter.getDataType(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataAuthorities(); j++) {
    // info += "    data authority: "
    // + filter.getDataAuthority(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataPaths(); j++) {
    // info += "    data path: " + filter.getDataPath(j)
    // + "\n";
    // }
    // for (int j = 0; j < filter.countDataSchemes(); j++) {
    // info += "    data path: " + filter.getDataScheme(j)
    // + "\n";
    // }
    // // for (ComponentName activity : activities) {
    // // info += "activity="
    // // + activity.flattenToString() + "\n";
    // // }
    // }
    // }
    // }
    // return info;
    // }

    /**
     * Return whether the given PackageInfo represents a system package or not.
     * User-installed packages (Market or otherwise) should not be denoted as
     * system packages.
     *
     * @param pi The PackageInfo.
     * @return If the PackageInfo is a system package.
     */
    private boolean isSystemPackage(PackageInfo pi) {
        return (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                != 0;
    }

    /**
     * Gets a List of PInfo's for the installed packages.
     *
     * @param getSysPackages Whether to use system packages.
     * @return List of installed apps.
     */
    private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
        ArrayList<PInfo> res = new ArrayList<>();
        List<PackageInfo> packageInfos = getPackageManager()
                .getInstalledPackages(0);
        PInfo newPi;
        PackageInfo pi;
        for (int i = 0; i < packageInfos.size(); i++) {
            pi = packageInfos.get(i);
            if ((!getSysPackages) && (pi.versionName == null)) {
                continue;
            }
            newPi = new PInfo(pi, mDoPermissions);
            res.add(newPi);
        }
        Collections.sort(res);
        return res;
    }

    /**
     * Gets a List of preferred packages.
     *
     * @return The list of preferred packages.
     */
    private ArrayList<PInfo> getPreferredApps() {
        ArrayList<PInfo> res = new ArrayList<>();
        // This returns nothing
        // List<PackageInfo> packageInfos = getPackageManager()
        // .getPreferredPackages(0);
        List<PackageInfo> packageInfos = getPackageManager()
                .getInstalledPackages(0);
        Log.d(TAG,
                this.getClass().getSimpleName()
                        + ".getPreferredApps: installed packageInfos size="
                        + packageInfos.size());

        List<IntentFilter> filters = new ArrayList<>();
        List<ComponentName> activities = new ArrayList<>();
        PInfo newPi;
        PackageInfo pi;
        int nPref, nFilters, nActivities;
        for (int i = 0; i < packageInfos.size(); i++) {
            pi = packageInfos.get(i);
            if (pi.versionName == null) {
                continue;
            }
            nPref = getPackageManager().getPreferredActivities(filters,
                    activities, pi.packageName);
            nFilters = filters.size();
            nActivities = activities.size();
            Log.d(TAG, pi.packageName + " nPref=" + nPref + " nFilters="
                    + nFilters + " nActivities=" + nActivities);
            if (nPref > 0 || nFilters > 0 || nActivities > 0) {
                // Don't do permissions for these
                newPi = new PInfo(pi, false);
                newPi.setInfo("");
                // newPi.setInfo(pkg.packageName + " nPref=" + nPref
                // + " nFilters=" + nFilters + " nActivities="
                // + nActivities + "\n");
                for (IntentFilter filter : filters) {
                    // newPi.appendInfo("IntentFilter: " + " actions="
                    // + filter.countActions() + " categories="
                    // + filter.countCategories() + " types="
                    // + filter.countDataTypes() + " authorities="
                    // + filter.countDataAuthorities() + " paths="
                    // + filter.countDataPaths() + " schemes="
                    // + filter.countDataSchemes() + "\n");
                    newPi.appendInfo("IntentFilter:\n");
                    for (int j = 0; j < filter.countActions(); j++) {
                        newPi.appendInfo("    action: " + filter.getAction(j)
                                + "\n");
                    }
                    for (int j = 0; j < filter.countCategories(); j++) {
                        newPi.appendInfo("    category: "
                                + filter.getCategory(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataTypes(); j++) {
                        newPi.appendInfo("    type: " + filter.getDataType(j)
                                + "\n");
                    }
                    for (int j = 0; j < filter.countDataAuthorities(); j++) {
                        newPi.appendInfo("    data authority: "
                                + filter.getDataAuthority(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataPaths(); j++) {
                        newPi.appendInfo("    data path: "
                                + filter.getDataPath(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataSchemes(); j++) {
                        newPi.appendInfo("    data path: "
                                + filter.getDataScheme(j) + "\n");
                    }
                    // for (ComponentName activity : activities) {
                    // newPi.appendInfo("activity="
                    // + activity.flattenToString() + "\n");
                    // }
                }
                res.add(newPi);
            }
        }
        Collections.sort(res);
        return res;
    }

    // private void testReader() {
    // // Open a file with Adobe Reader
    // File file = new File("/storage/extSdCard/PDF/Images Book.pdf");
    // Intent intent = new Intent(Intent.ACTION_VIEW);
    // intent.setDataAndType(Uri.fromFile(file), "application/pdf");
    // intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    // // startActivity(intent);
    //
    // Log.d(TAG, this.getClass().getSimpleName() + ".testReader: "
    // + "intent: " + intent);
    //
    // // Log.d(TAG, "intent URI: " + intent.toURI());
    // final List<ResolveInfo> list = getPackageManager()
    // .queryIntentActivities(intent, 0);
    // Log.d(TAG, "Packages:");
    // for (ResolveInfo rInfo : list) {
    // String pkgName = rInfo.activityInfo.applicationInfo.packageName;
    // Log.d(TAG, "  " + pkgName);
    // }
    //
    // ResolveInfo rDefault = getPackageManager().resolveActivity(intent,
    // PackageManager.MATCH_DEFAULT_ONLY);
    // if (rDefault == null) {
    // Log.d(TAG, " Default=null");
    // } else {
    // Log.d(TAG, " Default="
    // + rDefault.activityInfo.applicationInfo.packageName);
    // }
    // }

    /**
     * Gets information about the applications.
     *
     * @return Information about the applications.
     */
    private String getAppsInfo() {
        String info = "Application Information\n";

        // DEBUG
        // info +=
        // "\n\n------------------------------------------------------\n";
        // info += getPreferredAppInfo();
        // info += "------------------------------------------------------\n\n";

        // Date
        Date now = new Date();
        // This has the most hope of giving a result that is locale dependent.
        // At one time it had the UTC offset wrong, but seems to work now.
        info += now + "\n\n";
        // This allows more explicit formatting.
        // SimpleDateFormat formatter = new SimpleDateFormat(
        // "MMM dd, yyyy HH:mm:ss z");
        // info += formatter.format(now) + "\n\n";

        // Build information
        if (mDoBuildInfo) {
            info += "Build Information\n\n";
            info += getBuildInfo() + "\n";
        }

        // Memory information
        if (mDoMemoryInfo) {
            info += "Memory Information\n\n";
            info += getMemoryInfo() + "\n";
        }

        if (mDoPreferredApplications) {
            info += "Preferred Applications (Launch by Default)\n\n";
            try {
                // false = no system packages
                ArrayList<PInfo> apps = getPreferredApps();
                final int max = apps.size();
                PInfo app;
                for (int i = 0; i < max; i++) {
                    app = apps.get(i);
                    // No "\n" here
                    info += app.prettyPrint();
                }
            } catch (Exception ex) {
                info += "Error gettingPreferred Applications:\n\n";
                info += ex.getMessage() + "\n\n";
                Log.d(TAG, "Error gettingPreferred Applications:", ex);
            }
        }

        // Non-system applications information
        if (mDoNonSystemApps) {
            info += "Downloaded Applications\n\n";
            try {
                // false = no system packages
                ArrayList<PInfo> apps = getInstalledApps(false);
                final int max = apps.size();
                PInfo app;
                for (int i = 0; i < max; i++) {
                    app = apps.get(i);
                    if (!app.isSystem) {
                        info += app.prettyPrint() + "\n";
                    }
                }
            } catch (Exception ex) {
                info += "Error getting Application Information:\n";
                info += ex.getMessage() + "\n\n";
            }
        }

        // System applications information
        if (mDoSystemApps) {
            info += "System Applications\n\n";
            try {
                // false = no system packages
                ArrayList<PInfo> apps = getInstalledApps(false);
                final int max = apps.size();
                PInfo app;
                for (int i = 0; i < max; i++) {
                    app = apps.get(i);
                    if (app.isSystem) {
                        info += app.prettyPrint() + "\n";
                    }
                }
            } catch (Exception ex) {
                info += "Error getting System Application Information:\n";
                info += ex.getMessage() + "\n\n";
            }
        }

        // setProgressBarIndeterminateVisibility(false);

        return info;
    }

    /**
     * Class to manage a single PackageInfo.
     */
    class PInfo implements Comparable<PInfo> {
        private final String appname;
        private final String pname;
        private final String versionName;
        private String permissionsInfo = "";
        private String info = null;
        private final boolean isSystem;

        // private int versionCode = 0;
        // private Drawable icon;

        private PInfo(PackageInfo pi, boolean doPermissions) {
            appname = pi.applicationInfo.loadLabel(getPackageManager())
                    .toString();
            pname = pi.packageName;
            versionName = pi.versionName;
            // versionCode = pkg.versionCode;
            isSystem = isSystemPackage(pi);
            // icon = pkg.applicationInfo.loadIcon(getPackageManager());
            if (doPermissions) {
                permissionsInfo = "Permissions:\n";
                try {
                    // Permissions is not filled in for the incoming PackageInfo
                    // Need to get a new PackageInfo asking for permissions
                    PackageInfo pi1 = getPackageManager().getPackageInfo
                            (pname, PackageManager.GET_PERMISSIONS);
                    String[] permissions = pi1.requestedPermissions;
                    // Note: permissions seems to be  null rather than a
                    // zero-length  array if there are no permissions
                    if (permissions != null) {
                        boolean granted;
                        for (int i = 0; i < permissions.length; i++) {
                            granted = (pi1.requestedPermissionsFlags[i] &
                                    PackageInfo
                                            .REQUESTED_PERMISSION_GRANTED) != 0;
                            permissionsInfo += "  " + (granted ? "" : "X ") +
                                    permissions[i] + "\n";
                        }
                    }
                } catch (Exception ex) {
                    permissionsInfo += "  Error: " + ex.toString() + "\n";
                }
            }
        }

        private String prettyPrint() {
            String info = "";
            info += appname + "\n";
            info += pname + "\n";
            info += "Version: " + versionName + "\n";
            info += permissionsInfo;
            if (this.info != null) {
                info += this.info + "\n";
            }
            // info += "Version code: " + versionCode + "\n";
            // DEBUG
            // Log.d(TAG, info);
            return info;
        }

        public boolean isSystem() {
            return isSystem;
        }

        @Override
        public int compareTo(@NonNull PInfo another) {
            return this.appname.compareTo(another.appname);
        }

        public String getInfo() {
            return info;
        }

        private void setInfo(String info) {
            this.info = info;
        }

        private void appendInfo(String info) {
            this.info += info;
        }

    }

}
