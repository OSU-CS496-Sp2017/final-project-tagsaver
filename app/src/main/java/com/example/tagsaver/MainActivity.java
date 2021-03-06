package com.example.tagsaver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

// InstagramAuthentication
import com.example.tagsaver.utils.CategoriesContract;
import com.example.tagsaver.utils.CategoryObj;
import com.example.tagsaver.utils.InstagramAuthentication;
import com.example.tagsaver.utils.InstagramAuthentication.OAuthAuthenticationListener;
import com.example.tagsaver.utils.ApplicationData;
import com.example.tagsaver.utils.TagsDBHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends AppCompatActivity {


    private RecyclerView mSearchResultsRV;
    private EditText mSearchBoxET;
    private TextView mLoadingErrorMessageTV;
    private boolean isErrorVisible = false;
    private catRecyclerAdapter mCategoriesAdapter;
    private SQLiteDatabase mDBread;
    private Button cpyBtn;
    private SQLiteDatabase mDBwrite;
    private Button editCat;
    private String[] whereCondition = {""};
    private static final String TAG = MainActivity.class.getSimpleName();

    public static ArrayList<CategoryObj> userCatList = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    // ** Instagram Authenication *** //
    private InstagramAuthentication mApp;
    private Button btnConnect;


    private HashMap<String, String> userInfoHashmap = new HashMap<String, String>();
    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == InstagramAuthentication.WHAT_FINALIZE) {
                userInfoHashmap = mApp.getUserInfo();
            } else if (msg.what == InstagramAuthentication.WHAT_FINALIZE) {
                Toast.makeText(MainActivity.this, "Check your network.",
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    });

    // **** end Instagram Authentication **** //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ArrayList<String> alreadyDisp = new ArrayList<>();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView mainTitle = (TextView) findViewById(R.id.catTitle);
        mainTitle.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mLoadingErrorMessageTV = (TextView) findViewById(R.id.tv_loading_error_message);

        TagsDBHelper dbHelper = new TagsDBHelper(this);

        mDBread = dbHelper.getReadableDatabase();

        String sortOrder =
                CategoriesContract.FavoriteRepos.COLUMN_FULL_NAME + " DESC";
        String[] proj = {CategoriesContract.FavoriteRepos.COLUMN_FULL_NAME, CategoriesContract.FavoriteRepos._ID};

        String whereClauseArgs = "";

        //Recycler view
        mSearchResultsRV = (RecyclerView) findViewById(R.id.rv_categories);
        mSearchResultsRV.setLayoutManager(new LinearLayoutManager(this));
        mSearchResultsRV.setHasFixedSize(true);
        mCategoriesAdapter = new catRecyclerAdapter();
        mSearchResultsRV.setAdapter(mCategoriesAdapter);


        Cursor cursor = mDBread.query(
                CategoriesContract.FavoriteRepos.TABLE_NAME, // The table to query
                null,                               // The columns to return
                null,                               // The columns for the WHERE clause
                null,                               // The values for the WHERE clause
                null,                               // don't group the rows
                null,                               // don't filter by row groups
                sortOrder                           // The sort order
        );

        Log.d(TAG, "cursor created");
        while (cursor.moveToNext()) {

             byte[] blob = cursor.getBlob(cursor.getColumnIndex(CategoriesContract.FavoriteRepos.COLUMN_DESCRIPTION));
            if(blob!=null){
                String json = new String(blob);
                Gson gson = new Gson();
                userCatList = gson.fromJson(json, new TypeToken<ArrayList<CategoryObj>>(){}.getType());
            }

        }

        for(int i = 0; i < userCatList.size(); i++) {
            if(!alreadyDisp.contains(userCatList.get(i).toString())) {
                alreadyDisp.add(userCatList.get(i).catName);
                mCategoriesAdapter.addTag(userCatList.get(i).catName);
            }
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mSearchBoxET, 0);


        // Instagram Authentication
        mApp = new InstagramAuthentication(this, ApplicationData.CLIENT_ID,
                ApplicationData.CLIENT_SECRET, ApplicationData.CALLBACK_URL);
        mApp.setListener(new OAuthAuthenticationListener() {

            @Override
            public void onSuccess() {
                btnConnect.setText("Disconnect");
                mApp.fetchUserName(handler);
            }

            @Override
            public void onFail(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT)
                        .show();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), Adder.class);
                startActivity(intent);
            }
        });


        // Connect Button Click Event
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectOrDisconnectUser();
            }
        });



        if (mApp.hasAccessToken()) {
            btnConnect.setText("Disconnect");
            mApp.fetchUserName(handler);

        }

    }

    public static ArrayList<CategoryObj> getCatList()
    {
        return userCatList;
    }


    @Override
    protected void onResume() {
        super.onResume();
        String sortOrder =
                CategoriesContract.FavoriteRepos.COLUMN_FULL_NAME + " DESC";
        mSearchResultsRV = (RecyclerView) findViewById(R.id.rv_categories);
        mSearchResultsRV.setLayoutManager(new LinearLayoutManager(this));
        mSearchResultsRV.setHasFixedSize(true);
        mCategoriesAdapter = new catRecyclerAdapter();
        mSearchResultsRV.setAdapter(mCategoriesAdapter);
        ArrayList<String> alreadyDisp = new ArrayList<>();

        Cursor cursor = mDBread.query(
                CategoriesContract.FavoriteRepos.TABLE_NAME, // The table to query
                null,                               // The columns to return
                null,                               // The columns for the WHERE clause
                null,                               // The values for the WHERE clause
                null,                               // don't group the rows
                null,                               // don't filter by row groups
                sortOrder                           // The sort order
        );

        Log.d(TAG, "cursor created");
        while (cursor.moveToNext()) {

            byte[] blob = cursor.getBlob(cursor.getColumnIndex(CategoriesContract.FavoriteRepos.COLUMN_DESCRIPTION));
            if(blob!=null){
                String json = new String(blob);
                Gson gson = new Gson();
                userCatList = gson.fromJson(json, new TypeToken<ArrayList<CategoryObj>>(){}.getType());

            }

        }

        int i = 0;
        for(i = 0; i < userCatList.size(); i++) {
            if(!alreadyDisp.contains(userCatList.get(i).toString())) {
                alreadyDisp.add(userCatList.get(i).catName);
                mCategoriesAdapter.addTag(userCatList.get(i).catName);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    // Connects or Disconnects Instagram Session
    private void connectOrDisconnectUser() {
        if (mApp.hasAccessToken()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this);
            builder.setMessage("Disconnect from Instagram?")
                    .setCancelable(false)
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    mApp.resetAccessToken();
                                    btnConnect.setText("Connect");
                                }
                            })
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            });
            final AlertDialog alert = builder.create();
            alert.show();
        } else {
            mApp.authorize();
        }
    }
}
