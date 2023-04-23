package com.example.woofmatedating;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class arrayAdapter extends ArrayAdapter<cards> {

    Context context;

    public arrayAdapter(Context context, int resourceId, List<cards> items) {
        super(context, resourceId, items);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        cards card_item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView age = (TextView) convertView.findViewById(R.id.age);
        TextView ageT = (TextView) convertView.findViewById(R.id.ageT);
        TextView race = (TextView) convertView.findViewById(R.id.race);
        TextView raceT = (TextView) convertView.findViewById(R.id.raceT);
        TextView bio = (TextView) convertView.findViewById(R.id.bio);
        TextView location = (TextView) convertView.findViewById(R.id.location);
        ImageView image = (ImageView) convertView.findViewById(R.id.image);

        name.setText(card_item.getName());
        age.setText(card_item.getAge());
        ageT.setText("Age:");
        race.setText(card_item.getRace());
        raceT.setText("Race:   ");
        bio.setText("\""+card_item.getBio()+"\"");
        location.setText(card_item.getLocation());
        switch(card_item.getProfileImageUrl()){
            case "default":
                Glide.with(convertView.getContext()).load(R.mipmap.ic_launcher).into(image);
                break;
            default:
                // Glide.clear(image);
                Glide.with(convertView.getContext()).load(card_item.getProfileImageUrl()).into(image);
                break;
        }

        return convertView;

    }
}