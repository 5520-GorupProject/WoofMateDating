package com.example.woofmatedating;

public class cards {
    private String userId;
    private String name, age, race, bio;
    private String profileImageUrl;

    public cards (String userId, String name, String age, String race, String bio, String profileImageUrl){
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.age = age;
        this.race = race;
        this.bio = bio;
    }

    public String getUserId(){
        return userId;
    }
    public void setUserID(String userID){
        this.userId = userId;
    }

    public String getAge(){
        return age;
    }

    public String getRace(){
        return race;
    }

    public String getBio(){
        return bio;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public String getProfileImageUrl(){
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl){
        this.profileImageUrl = profileImageUrl;
    }
}
