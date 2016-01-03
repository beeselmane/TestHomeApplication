package com.beeselmane.testapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity implements SearchManager.OnDismissListener, SearchView.OnQueryTextListener
{
    private static boolean showAllBundles = false;

    private GlobalApplicationState applicationState = null;
    private List<AppPackage> currentPackageList = null;
    private MenuItem searchMenuItem = null;
    private ListView appListView = null;
    private HomeActivity self = this;

    private SharedPreferences preferences = null;
    private boolean useDarkItems = false;
    private int PICK_IMAGE_REQUEST = 1;

    public void reload()
    {
        if (this.searchMenuItem != null && this.searchMenuItem.isActionViewExpanded())
            this.searchMenuItem.collapseActionView();

        if (preferences == null) preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        this.useDarkItems = preferences.getBoolean("UseDarkBackground", false);

        if (this.useDarkItems) this.appListView.setDivider(new ColorDrawable(0x333333));
        this.updateAppListView(this.applicationState.installedApplications());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_applist);
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        this.appListView = (ListView)this.findViewById(R.id.app_list);
        this.applicationState = new GlobalApplicationState(this);
        this.updateAppListView(this.applicationState.installedApplications());
        this.setTitle(R.string.list_title);
        this.addClickListener();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        this.reload();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        this.reload();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SEARCH))
            this.onQueryTextChange(intent.getStringExtra(SearchManager.QUERY));
    }

    @Override
    public ActionBar getActionBar() {
        return super.getActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.getMenuInflater().inflate(R.menu.homebar, menu);
        this.searchMenuItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager)this.getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)this.searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
        searchManager.setOnDismissListener(this);
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        } else if (id == R.id.action_wallpaper) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            this.startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this.getApplicationContext());
            DisplayMetrics displayMetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                wallpaperManager.setBitmap(bitmap);
                wallpaperManager.suggestDesiredDimensions(displayMetrics.widthPixels, displayMetrics.heightPixels);
                Toast.makeText(this, "Successfully changed wallpaper!", Toast.LENGTH_SHORT).show();
            } catch (IOException ex) {
                Toast.makeText(this, "Error setting wallpaper!", Toast.LENGTH_SHORT).show();
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public boolean onQueryTextChange(String searchQuery)
    {
        List<AppPackage> installedApps = this.applicationState.installedApplications();
        List<AppPackage> results = new ArrayList<>();

        for (AppPackage app : installedApps)
        {
            String appLabel = app.label.toString().toLowerCase();
            if (appLabel.contains(searchQuery)) results.add(app);
        }

        this.updateAppListView(results);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.toggleKeyboard(false);
        return true;
    }

    @Override
    public void onDismiss() {
        this.updateAppListView(this.applicationState.installedApplications());
    }

    private void addClickListener()
    {
        this.appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppPackage application = self.currentPackageList.get(position);
                Intent intent = self.getPackageManager().getLaunchIntentForPackage(application.name.toString());
                self.startActivity(intent);
                self.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        });
    }

    private void updateAppListView(final List<AppPackage> apps)
    {
        this.appListView.setAdapter(new ArrayAdapter<AppPackage>(this, R.layout.list_item, apps) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) view = self.getLayoutInflater().inflate(R.layout.list_item, null);
                AppPackage representedApplication = apps.get(position);

                if (self.useDarkItems) view.setBackgroundColor(0x2F000000);
                else view.setBackgroundColor(Color.TRANSPARENT);

                ImageView iconView = (ImageView)view.findViewById(R.id.item_app_icon);
                iconView.setImageDrawable(representedApplication.icon);
                TextView labelView = (TextView)view.findViewById(R.id.item_app_label);
                labelView.setText(representedApplication.label);
                TextView nameView = (TextView)view.findViewById(R.id.item_app_name);
                nameView.setText(representedApplication.name);

                return view;
            }
        });

        this.currentPackageList = apps;
    }

    private void toggleKeyboard(boolean forceClose)
    {
        InputMethodManager keyboardManager = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (!forceClose) keyboardManager.toggleSoftInput(0, 0);
        else keyboardManager.hideSoftInputFromInputMethod(this.appListView.getWindowToken(), 0);
    }
}
