package com.example.harmonia;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "harmonia_user_credentials")
public class HarmoniaUserCredentials implements Parcelable {
    public static final Creator<HarmoniaUserCredentials> CREATOR = new Creator<HarmoniaUserCredentials>() {
        @Override
        public HarmoniaUserCredentials createFromParcel(Parcel in) {
            return new HarmoniaUserCredentials(in);
        }

        @Override
        public HarmoniaUserCredentials[] newArray(int size) {
            return new HarmoniaUserCredentials[size];
        }
    };

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "soundcloud_user_id")
    private String soundCloudUserId;
    @ColumnInfo(name = "spotify_access_token")
    private String spotifyAccessToken;

    public HarmoniaUserCredentials(String soundCloudUserId, String spotifyAccessToken) {
        this.soundCloudUserId = soundCloudUserId;
        this.spotifyAccessToken = spotifyAccessToken;
    }

    protected HarmoniaUserCredentials(Parcel in) {
        id = in.readInt();
        soundCloudUserId = in.readString();
        spotifyAccessToken = in.readString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSoundCloudUserId() {
        return soundCloudUserId;
    }

    public void setSoundCloudUserId(String soundCloudUserId) {
        this.soundCloudUserId = soundCloudUserId;
    }

    public String getSpotifyAccessToken() {
        return spotifyAccessToken;
    }

    public void setSpotifyAccessToken(String spotifyAccessToken) {
        this.spotifyAccessToken = spotifyAccessToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(soundCloudUserId);
        dest.writeString(spotifyAccessToken);
    }
}
