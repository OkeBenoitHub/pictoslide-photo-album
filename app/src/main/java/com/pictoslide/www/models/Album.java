package com.pictoslide.www.models;

import java.util.List;

public class Album {
    private String albumId;
    private String ownerId;
    private String name;
    private int category;
    private String description;
    private String sharedLink;
    private List<String> photoPathsList;
    private List<String> musicIdsList;
    private List<String> subOwnersId;
    private String createdAtTime;
    private String createdAtDate;

    public Album() {
    }

    public Album(String albumId,String ownerId, String name, int category, String description, String sharedLink, List<String> photoPathsList, List<String> musicIdsList,List<String> subOwnersId, String createdAtTime, String createdAtDate) {
        this.albumId = albumId;
        this.ownerId = ownerId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.sharedLink = sharedLink;
        this.photoPathsList = photoPathsList;
        this.musicIdsList = musicIdsList;
        this.subOwnersId = subOwnersId;
        this.createdAtTime = createdAtTime;
        this.createdAtDate = createdAtDate;
    }

    public Album(String albumId,String ownerId, List<String> photoPathsList, List<String> musicIdsList, List<String> subOwnersId) {
        this.albumId = albumId;
        this.ownerId = ownerId;
        this.photoPathsList = photoPathsList;
        this.musicIdsList = musicIdsList;
        this.subOwnersId = subOwnersId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public int getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPhotoPathsList() {
        return photoPathsList;
    }

    public List<String> getMusicIdsList() {
        return musicIdsList;
    }

    public String getCreatedAtTime() {
        return createdAtTime;
    }

    public String getAlbumId() {
        return albumId;
    }

    public List<String> getSubOwnersId() {
        return subOwnersId;
    }

    public String getSharedLink() {
        return sharedLink;
    }

    public String getCreatedAtDate() {
        return createdAtDate;
    }
}
