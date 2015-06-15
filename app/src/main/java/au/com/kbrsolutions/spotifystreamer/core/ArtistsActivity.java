package au.com.kbrsolutions.spotifystreamer.core;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class ArtistsActivity extends AppCompatActivity implements ArtistListFragment. ArtistSelectable {

    private boolean mTestMode;
    private ArtistListFragment artistListFragment;
    private ArtistArrayAdapter artistArrayAdapter;
    private TextView sarchText;
    private InputMethodManager imm;
//    private ProgressBarHandler mProgressBarHandler;

    public final static String SUMMARY_TAG = "summary_tag";

    @Override
    public void handleSelectedArtist(String artistId) {
        Log.v(LOG_TAG, "handleSelectedArtist - artistId: " + artistId);
    }

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
        Log.v(LOG_TAG, "onCreate - start");
        setContentView(R.layout.activity_artists);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        sarchText = (TextView) findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });

        setFragment(FragmentsEnum.SUMMARY_FRAGMENT, getString(R.string.title_fragment_summary), true, null);
//        mProgressBarHandler = new ProgressBarHandler(this);
//        getActionBar().setTitle(getResources().getString(R.string.artist_activity_title));
    }

    private boolean handleSearchButtonClicked(int actionId) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = sarchText.getText().toString();
            if (artistName.trim().length() > 0 && artistListFragment != null) {
//                sendArtistsDataRequestToSpotify(artistName);
                artistListFragment.sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            }
        }
        Log.v(LOG_TAG, "handleSearchButtonClicked");
        return handled;
    }
    private void hideKeyboard() {
        // A_MUST: during monkey test got NullPointer Exception
//        View view = getView();
        if (sarchText != null && sarchText.getWindowToken() != null && imm != null) {
            imm.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
        }
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
                List<ArtistDetails> summaryItemsList = new ArrayList<>();
//                summaryItemsList.add(new ArtistDetails("name0", "id0", "thumbImage0"));
//                summaryItemsList.add(new ArtistDetails("name1", "id1", "thumbImage1"));
//                summaryItemsList.add(new ArtistDetails("name2", "id2", "thumbImage2"));

                artistArrayAdapter = new ArtistArrayAdapter<ArtistDetails>(this, summaryItemsList);
                artistListFragment.setListAdapter(artistArrayAdapter);
                artistArrayAdapter.notifyDataSetChanged();

    			fragmentManager.beginTransaction().replace(R.id.fragments_frame, artistListFragment).commit();
//                fragmentTransaction = fragmentManager.beginTransaction();
//                fragmentTransaction.replace(R.id.fragments_frame, artistListFragment, SUMMARY_TAG);
//                fragmentTransaction.commit();
//                Fragment f1 = fragmentManager.findFragmentByTag(SUMMARY_TAG);
//                Log.i(LOG_TAG, "setFragment - ##FOLDER_TAG before executePendingTransactions fragment: " + f1);
//                fragmentManager.executePendingTransactions();				// will wait until the replace and commit are done
//                f1 = fragmentManager.findFragmentByTag(SUMMARY_TAG);
//                Log.i(LOG_TAG, "setFragment - ##FOLDER_TAG after  executePendingTransactions fragment: " + f1);

//				Log.i(LOG_TAG, "setFragment - FOLDER_FRAGMENT isRobotiomTestInProgress: " + isRobotiumTestInProgress);
                break;

            default:
                if (!mTestMode) {
                    throw new RuntimeException(LOG_TAG + " - setFragment - no code to handle fragmentId: " + fragmentId);
                }
        }
//        Log.i(LOG_TAG, "@#setFragment - end - getFolderFragmentCount/addFragmentToStack/fragmentsArrayDeque: " + fragmentsStack.getFolderFragmentCount() + "/" + addFragmentToStack + "/" + fragmentsStack.toString());
    }

    public  void processClick(int position) {
        Log.v(LOG_TAG, "processClick - start");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "onDestroy - done");
    }
}
