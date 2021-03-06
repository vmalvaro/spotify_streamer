/**
 *  Copyright (C) 2015 AlvaroVM.com
 */

package com.alvarovm.android.spotifystreamer.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.alvarovm.android.spotifystreamer.R;
import com.alvarovm.android.spotifystreamer.adapter.CustomAdapterTopTracks;
import com.alvarovm.android.spotifystreamer.model.MyArtist;
import com.alvarovm.android.spotifystreamer.model.MyTopTrack;
import com.alvarovm.android.spotifystreamer.util.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private CustomAdapterTopTracks customAdapterTopTracks;
    private ArrayList<MyTopTrack> myTopTrackListQuery;;
    public static final String TOP_TRACK_LIST = "TOP_TRACK_LIST";
    public static final String TRACK_POSITION = "TRACK_POSITION";

    private final int FIRST_LIST_ITEM = 0;

    public DetailActivityFragment() {
    }

    public interface Callback {
        public void onItemSelected(ArrayList topTrackList,
                                   int pos);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        ListView listViewTopTracks = (ListView) rootView.findViewById(R.id.listview_toptracks);
        List<MyTopTrack> listAdapter;

        /**
         * To do:Change saveinstance check to onCreate method
         */

        if(savedInstanceState == null || !savedInstanceState.containsKey("myTopTrackListQuery")) {
            listAdapter = new ArrayList<MyTopTrack>();
        } else {
            listAdapter = savedInstanceState.getParcelableArrayList("myTopTrackListQuery");
            myTopTrackListQuery = (ArrayList) listAdapter;
        }


        //Initialization of Custom Track ArrayAdapter
        customAdapterTopTracks = new CustomAdapterTopTracks(
                                     getActivity(),
                                     listAdapter);

        listViewTopTracks.setAdapter(customAdapterTopTracks);

        listViewTopTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                MyTopTrack myTopTrack = customAdapterTopTracks.getItem(position);
                ((Callback) getActivity()).onItemSelected(myTopTrackListQuery,position);

            }
        });

        return rootView;
    }

    /**
     *  Initializate Activity and populate with top tracks
     */
    @Override
    public void onStart() {
        super.onStart();
        Bundle arguments = getArguments();
        String id = null;
        if (arguments != null) {
            id = arguments.getString(MyArtist.ID);
            if(id != null) { updateTopTracks(id); }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private void updateTopTracks(String id) {
        Context context = getActivity().getApplicationContext();

        if( Helper.isOnline(context) ){
            FetchTopTracks fetchTopTracks = new FetchTopTracks();
            fetchTopTracks.execute(id);
        }else {
            Toast.makeText(context, "No network connection available.", Toast.LENGTH_LONG).show();
        }


    }

    private String getArtistNameFromTrack(Track track){

       if(!track.artists.isEmpty()){
           return track.artists.get(FIRST_LIST_ITEM).name;
       }
        return null;

    }



    private class FetchTopTracks extends AsyncTask<String, Void, Tracks> {
        Resources res = getActivity().getResources();

        @Override
        protected Tracks doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();;
            SpotifyService spotify = null;
            HashMap<String, Object> countryMap = new HashMap<>();
            countryMap.put(res.getString(R.string.country), res.getString(R.string.country_code));

            //Search for top tracks
            try {
                if(params.length == 0){ return null; }
                spotify = api.getService();
                Log.v(LOG_TAG, "Top tracks from artist " + params[0]);

                return spotify.getArtistTopTrack(params[0],countryMap);

            } catch (RetrofitError ex) {
                Log.e(LOG_TAG, "Error at retrieving top tracks data " + ex.getMessage());
                Toast.makeText(getActivity().getApplicationContext(),
                        ex.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            return null;
        }

        /**
         * Update ArrayAdapter and refresh UI
         * @param listTracks
         */
        @Override
        protected void onPostExecute(Tracks listTracks) {
            customAdapterTopTracks.clear();
            MyTopTrack myTopTrack;

            if(listTracks != null && !listTracks.tracks.isEmpty()) {
                myTopTrackListQuery = new ArrayList<MyTopTrack>();

               for (Track track : listTracks.tracks) {
                   List<Image> listImages = track.album.images;
                   String artistName = getArtistNameFromTrack(track);

                   if(!listImages.isEmpty()) {
                       myTopTrack = new MyTopTrack(track.name,
                               track.album.name,
                               listImages.get(FIRST_LIST_ITEM).url,
                               listImages.get( listImages.size()-1 ).url,
                               track.preview_url,
                               artistName);
                   }else {
                       myTopTrack = new MyTopTrack(track.name,
                               track.album.name,
                               null,
                               null,
                               track.preview_url,
                               artistName);
                   }

                   //Add in list to saveOnInstance
                    myTopTrackListQuery.add(myTopTrack);
                   //add To adapter
                    customAdapterTopTracks.add(myTopTrack);
                }
            }else {
                Toast.makeText(getActivity().getApplicationContext(),
                               res.getString(R.string.no_top_tracks),
                               Toast.LENGTH_SHORT).show();
            }
        }


    }



}
