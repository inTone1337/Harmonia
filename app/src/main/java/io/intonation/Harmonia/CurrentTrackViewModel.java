package io.intonation.harmonia;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.jlubecki.soundcloud.webapi.android.models.Track;

public class CurrentTrackViewModel extends ViewModel {
    private MutableLiveData<Track> currentTrack;

    public MutableLiveData<Track> getCurrentTrack() {
        if (currentTrack == null) {
            currentTrack = new MutableLiveData<>();
        }
        return currentTrack;
    }
}