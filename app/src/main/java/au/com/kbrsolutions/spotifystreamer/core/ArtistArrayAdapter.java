package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

/**
 * Created by business on 9/06/2015.
 */
public class ArtistArrayAdapter<T> extends ArrayAdapter<ArtistDetails> {

    private List<ArtistDetails> objects;
    private ArtistsActivity mActivity;

    private final String LOG_TAG = ArtistArrayAdapter.class.getSimpleName();

//    public ArtistArrayAdapter(FragmentActivity activity, List<ArtistDetails> objects) {
    public ArtistArrayAdapter(ArtistsActivity activity, List<ArtistDetails> objects) {

    super(activity.getApplicationContext(), -1, objects);
        this.mActivity = activity;
        this.objects = objects;
		Log.i(LOG_TAG, "constructor - end - objects.size(): " + objects.size());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(LOG_TAG, "getView - start");
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.artists_listview_item, parent, false);
        }
        ImageView artistImage = (ImageView) v.findViewById(R.id.artistImageId);

        TextView artistName = (TextView) v.findViewById(R.id.artistNameId);

        ArtistDetails artistDetails = objects.get(position);

        if (artistDetails != null) {
            Log.i(LOG_TAG, "getView - artistName set to: " + artistDetails.name);
            artistName.setText(artistDetails.name);
        } else {
            Log.i(LOG_TAG, "getView - artistName is null");
        }

//        Picasso.with(mActivity.getApplication()).load("http://i.imgur.com/DvpvklR.png").into(artistImage);
        int px = convertDpToPx((int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) - 20);
        int px1 = dpToPx((int) mActivity.getResources().getDimension(R.dimen.artist_thumbnail_image_size) - 20);

        Log.i(LOG_TAG, "convertSpToPx - px/px1: " + px + "/" + px1);;
        Picasso.with(mActivity.getApplication()).load(artistDetails.thumbnailImageUrl).resize(px, px).centerCrop().into(artistImage);
//        Picasso.with(mActivity.getApplication()).load(artistDetails.thumbnailImageUrl).resize(140, 140).centerCrop().into(artistImage);
//        Picasso.with(mActivity.getApplication()).load(artistDetails.thumbnailImageUrl).onlyScaleDown().centerCrop().into(artistImage);

        return v;
    }

//    public int convertSpToPx(int sp) {
//        Resources r = mActivity.getResources();
//        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, r.getDisplayMetrics());
//        Log.i(LOG_TAG, "convertSpToPx - sp/px: " + sp + "/" + px);
//        return px;
//    }

    // todo: conversion is not working properly
    public int convertDpToPx(int dpStr) {
        int dp = Integer.valueOf(dpStr);
        Resources r = mActivity.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        Log.i(LOG_TAG, "convertSpToPx - dp/px: " + dpStr + "/" + px);
        return px;
    }

    private int dpToPx(int dp) {
        float density = mActivity.getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density) / 2;
    }


}
