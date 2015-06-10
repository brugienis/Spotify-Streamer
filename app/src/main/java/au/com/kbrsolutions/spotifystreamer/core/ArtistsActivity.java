package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class ArtistsActivity extends AppCompatActivity {

    private boolean mTestMode;
    private ArtistListFragment artistListFragment;
    private ArtistArrayAdapter artistArrayAdapter;
    private TextView sarchText;
    public final static String SUMMARY_TAG = "summary_tag";

    public enum FragmentsEnum {
        SUMMARY_FRAGMENT,
        DETAILS_FRAGMENT

    }
    enum FragmentsCallingSourceEnum {
        UPDATE_SUMMARY_LIST_ADAPTER
    }

    private final static String LOG_TAG = ArtistsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists);
        sarchText = (TextView) findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });
        setFragment(FragmentsEnum.SUMMARY_FRAGMENT, getString(R.string.title_fragment_summary), true, null);
    }

    private boolean handleSearchButtonClicked(int actionId) {
        Log.v(LOG_TAG, "handleSearchButtonClicked");
        return true;
    }

    private void setFragment(FragmentsEnum fragmentId, String titleText, boolean addFragmentToStack, FragmentsCallingSourceEnum callingSource) {
//        Log.i(LOG_TAG, "@#setFragment - start - fragmentId/callingSource/titleText/addFragmentToStack/fragmentsArrayDeque: " + fragmentId + "/" + callingSource + "/" + titleText + "/" + addFragmentToStack + "/" + fragmentsStack.toString());
        //		Log.i(LOG_TAG, "setFragment - start - ##fragmentsArrayDeque: " + fragmentsStack.toString());
        //		android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction;
        switch (fragmentId) {

            case SUMMARY_FRAGMENT:
//				String folderFragmentTag = FOLDER_TAG;
                Log.i(LOG_TAG, "setFragment - ##FOLDER_TAG folderFragmentTag: " + SUMMARY_TAG);
//                Log.i(LOG_TAG, "r##setFragment - callingSource/currFolderTitleIdx: " + callingSource + "/" + currFolderTitleIdx);
//                Log.i(LOG_TAG, "r##setFragment - list/folderItemsList: " + list.size() + "/" + folderItemsList.size());
                if (artistListFragment == null) {
                    Log.i(LOG_TAG, "setFragment - artistListFragment is null");
                    artistListFragment = new ArtistListFragment();
                }
                List<ArtistDetails> summaryItemsList = new ArrayList<ArtistDetails>();
                summaryItemsList.add(new ArtistDetails("name0", "id0", "thumbImage0"));
                summaryItemsList.add(new ArtistDetails("name1", "id1", "thumbImage1"));
                summaryItemsList.add(new ArtistDetails("name2", "id2", "thumbImage2"));

                artistArrayAdapter = new ArtistArrayAdapter<ArtistDetails>(this, summaryItemsList);
                artistListFragment.setListAdapter(artistArrayAdapter);
                artistArrayAdapter.notifyDataSetChanged();

                //			fragmentManager.beginTransaction().replace(R.id.fragments_frame, artistListFragment).commit();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragments_frame, artistListFragment, SUMMARY_TAG);
                fragmentTransaction.commit();
                Fragment f1 = fragmentManager.findFragmentByTag(SUMMARY_TAG);
                Log.i(LOG_TAG, "setFragment - ##FOLDER_TAG before executePendingTransactions fragment: " + f1);
                fragmentManager.executePendingTransactions();				// will wait until the replace and commit are done
                f1 = fragmentManager.findFragmentByTag(SUMMARY_TAG);
                Log.i(LOG_TAG, "setFragment - ##FOLDER_TAG after  executePendingTransactions fragment: " + f1);

//				Log.i(LOG_TAG, "setFragment - FOLDER_FRAGMENT isRobotiomTestInProgress: " + isRobotiumTestInProgress);
                break;

            default:
                if (!mTestMode) {
                    throw new RuntimeException(LOG_TAG + " - setFragment - no code to handle fragmentId: " + fragmentId);
                }
        }
//        Log.i(LOG_TAG, "@#setFragment - end - getFolderFragmentCount/addFragmentToStack/fragmentsArrayDeque: " + fragmentsStack.getFolderFragmentCount() + "/" + addFragmentToStack + "/" + fragmentsStack.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artists, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
