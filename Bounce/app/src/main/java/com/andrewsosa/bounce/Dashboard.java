package com.andrewsosa.bounce;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import java.util.GregorianCalendar;


public class Dashboard extends Activity implements Toolbar.OnMenuItemClickListener,
        DatePickerReceiver {


    // Actionbar and Navdrawer nonsense
    ActionBarDrawerToggle mDrawerToggle;

    // Recyclerview things
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private static TaskRecyclerAdapter mAdapter;
    static EditText editText;

    // Data sources
    private static TaskDataSource taskDataSource;
    private static ListDataSource listDataSource;

    // Temp holder for a picked date
    private int tempDay;
    private int tempMonth;
    private int tempYear;

    // Toggle view for the add menu
    private boolean showingInput = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Toolbar craziness
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        toolbar.setTitle("Dashboard");
        toolbar.inflateMenu(R.menu.menu_dashboard);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setTitleTextColor(getResources().getColor(R.color.abc_primary_text_material_dark));

        // Other toolbar craziness
        /* Toolbar sidebar = (Toolbar) findViewById(R.id.nav_toolbar);
        sidebar.inflateMenu(R.menu.menu_nav_drawer);
        sidebar.setOnMenuItemClickListener(this); */

        // Open datasources
        taskDataSource = new TaskDataSource(this);
        taskDataSource.open();
        listDataSource = new ListDataSource(this);
        listDataSource.open();

        // Drawer craziness
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.my_drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.setDrawerListener(mDrawerToggle);

        // Add button stuff
        final FloatingActionButton actionButton = (FloatingActionButton) findViewById(R.id.add_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LinearLayout addBox = (LinearLayout) findViewById(R.id.add_box);
                editText = (EditText) findViewById(R.id.task_name_edittext);

                if(showingInput) {
                    addBox.setVisibility(View.GONE);
                    actionButton.setImageResource(R.drawable.ic_add_white_24dp);
                } else {
                    addBox.setVisibility(View.VISIBLE);
                    actionButton.setImageResource(R.drawable.ic_close_white_24dp);
                    editText.requestFocus();
                }

                showingInput = !showingInput;
            }
        });


        // Things for recyclerviews
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, null, false, true));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter 
        mAdapter = new TaskRecyclerAdapter(taskDataSource.getAllTasks(), this);
        mRecyclerView.setAdapter(mAdapter);


        // Load the lists
        ListView listView = (ListView) findViewById(R.id.drawer_list);
        listView.setAdapter(new SimpleCursorAdapter(this, R.layout.drawer_item_view,
                listDataSource.getListCursor(), new String[]{ListOpenHelper.COLUMN_NAME},
                new int[]{R.id.list_name}, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER));
        Log.d("Bounce", "We loaded the lists successfully");




        // Header view
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.drawer_header, listView, false);
        listView.addHeaderView(header, null, false);

        // Footer view
        ViewGroup footer = (ViewGroup) inflater.inflate(R.layout.drawer_footer, listView, false);
        listView.addFooterView(footer, null, false);
        footer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewListDialog();
            }
        });


        // Things for adding tasks
        EditText editText = (EditText) findViewById(R.id.task_name_edittext);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    if(v.getText().toString().length() > 0) {
                        mAdapter.addElement(taskDataSource.createTask(v.getText().toString()));
                        v.setText("");

                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                        LinearLayout addBox = (LinearLayout) findViewById(R.id.add_box);
                        addBox.setVisibility(View.GONE);

                        LinearLayout decoy  = (LinearLayout) findViewById(R.id.decoy);
                        decoy.requestFocus();

                        actionButton.show();
                        //actionButton.setVisibility(View.VISIBLE);

                        return true;
                    }
                }

                return false;
            }
        });

        ImageButton calendarButton = (ImageButton) findViewById(R.id.calendar_button);
        calendarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = new DatePickerFragment();
                newFragment.assignMethod(Dashboard.this);
                newFragment.show(getFragmentManager(), "timePicker");
            }
        });

    }

    /* @Override
    protected void onResume() {
        super.onResume();
        //mAdapter = new TaskRecyclerAdapter(taskDataSource.getAllTasks(), this);
        //mAdapter.notifyItemChanged(mAdapter.getActiveItemNumber());
    } */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("Bounce", "On Activity Result");
        if (resultCode == RESULT_OK) {
            Log.d("Bounce", "Result Ok");
            if((data.getStringExtra("Action") != null) && (data.getStringExtra("Action").equals("delete"))) {
                Log.d("Bounce", "Action == Delete");
                mAdapter.removeActiveElement();
            }
            else {
                Task temp = taskDataSource.getTask(mAdapter.getActiveItem().getId());
                mAdapter.changeElement(mAdapter.getActiveItemNumber(), temp);
                mAdapter.notifyItemChanged(mAdapter.getActiveItemNumber());
            }
        }


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Toast.makeText(this, "Hello, world!", Toast.LENGTH_SHORT).show();
            //taskDataSource.deleteTask(mAdapter.getItem(0));
            //mAdapter.removeElementAt(0);
            return true;
        }
        if (id == R.id.action_create_list) {
            createNewListDialog();
        }

        return super.onOptionsItemSelected(item);
    }


    public void receiveDate(int year, int month, int day) {

        GregorianCalendar temp = new GregorianCalendar(year, month, day);
        Log.d("Bounce", "The date created was: " + TaskDataSource.dateToString(temp));

        if (editText.getText() != null) {
            mAdapter.addElement(taskDataSource.createTask(editText.getText().toString(),
                    TaskDataSource.dateToString(temp)));
        }
    }

    private EditText nameInput;
    private View positiveAction;
    private void createNewListDialog() {

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .customView(R.layout.create_dialog_view, true)
                .title("List Name")
                .positiveText("Create")
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (nameInput != null) {
                            //Toast.makeText(getApplicationContext(), "Password: " + nameInput.getText().toString(), Toast.LENGTH_SHORT).show();
                            listDataSource.createList(nameInput.getText().toString());
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                    }
                }).build();

        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        nameInput = (EditText) dialog.getCustomView().findViewById(R.id.list_name_input);
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.show();
        positiveAction.setEnabled(false); // disabled by default
    }

}
