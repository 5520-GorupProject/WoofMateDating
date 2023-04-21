package com.example.woofmatedating;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private cards cards_data[];
    //  private ArrayList<String> al;
    private arrayAdapter arrayAdapter;
    private int i;
    private FirebaseAuth mAuth;
    private String currentUId;

    //users
    private DatabaseReference usersDb;

    ListView listView;
    List<cards> rowItems;




   /* @InjectView(R.id.frame)
    SwipeFlingAdapterView flingContainer;*/


    BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ButterKnife.inject(this);
        nav = findViewById(R.id.navBar);
        nav.setSelectedItemId(R.id.home);
        nav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {

            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        return true;

                    case R.id.setting:
                        Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent1);
                        return true;

                    case R.id.chat:
                        Intent intent2 = new Intent(MainActivity.this, MatchesActivity.class);
                        startActivity(intent2);
                        return true;
                }

                return false;
            }
        });

        usersDb = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();
        currentUId = mAuth.getCurrentUser().getUid();

        checkUserSex();

        rowItems = new ArrayList<cards>();

        arrayAdapter = new arrayAdapter(this, R.layout.item, rowItems);

        SwipeFlingAdapterView flingContainer = (SwipeFlingAdapterView) findViewById(R.id.frame);

        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                rowItems.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            //store user's choice to the firebase
            @Override
            public void onLeftCardExit(Object dataObject) {
                cards obj = (cards) dataObject;
                String userId = obj.getUserId();
                usersDb.child(userId).child("connections").child("dislike").child(currentUId).setValue(true);
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                Toast.makeText(MainActivity.this, "Left Dislike", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                cards obj = (cards) dataObject;
                String userId = obj.getUserId();
                usersDb.child(userId).child("connections").child("like").child(currentUId).setValue(true);
                isConnectionMatch(userId);
                Toast.makeText(MainActivity.this, "Right Like", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
            }

            @Override
            public void onScroll(float scrollProgressPercent) {

                /*View view = flingContainer.getSelectedView();
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);*/
            }
        });


        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.image_popup_layout, null);

                ImageView popupImage = view.findViewById(R.id.popup_image);
                if (!rowItems.isEmpty()) {
                    cards currentCard = rowItems.get(0);
                    if (currentCard != null) {
                        switch (currentCard.getProfileImageUrl()) {
                            case "default":
                                Glide.with(MainActivity.this).load(R.mipmap.ic_launcher).into(popupImage);
                                break;
                            default:
                                Glide.with(MainActivity.this).load(currentCard.getProfileImageUrl()).into(popupImage);
                                break;
                        }
                    }
                }
                builder.setView(view)
                        .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                BitmapDrawable bitmapDrawable = (BitmapDrawable) popupImage.getDrawable();
                                Bitmap bitmap = bitmapDrawable.getBitmap();
                                String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(),bitmap,"some title", null);
                                Uri bitmapUri = Uri.parse(bitmapPath);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("image/*");
                                intent.putExtra(Intent.EXTRA_STREAM,bitmapUri);
                                startActivity(Intent.createChooser(intent,"Share Image"));
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                // User cancelled the dialog
                            }
                        });


                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
        });
    }

    private void isConnectionMatch(String userId) {
        DatabaseReference currentUserConnectionsDb = usersDb.child(currentUId).child("connections").child("like").child(userId);
        currentUserConnectionsDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Chat now!")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent2 = new Intent(MainActivity.this, MatchesActivity.class);
                                    startActivity(intent2);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    // User cancelled the dialog
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    String key = FirebaseDatabase.getInstance().getReference().child("Chat").push().getKey();

                    //usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).setValue(true);
                    usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).child("ChatId").setValue(key);

                    //usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).setValue(true);
                    usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).child("ChatId").setValue(key);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    /*public void logoutUser(){
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, ChooseLoginRegistrationActivity.class);
        startActivity(intent);
        finish();
    }*/

    private String userSex;
    private String oppositeUserSex;

    public void checkUserSex() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                DatabaseReference userDb = usersDb.child(user.getUid());
                userDb.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.child("sex").getValue() != null) {
                                userSex = dataSnapshot.child("sex").getValue().toString();
                                switch (userSex) {
                                    case "Male":
                                        oppositeUserSex = "Female";
                                        break;
                                    case "Female":
                                        oppositeUserSex = "Male";
                                        break;
                                }
                                getOppositeSexUsers();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        thread.start();
    }

    public void getOppositeSexUsers() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                usersDb.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if (snapshot.child("sex").getValue() != null) {
                            if (snapshot.exists() && !snapshot.child("connections").child("dislike").hasChild(currentUId) && !snapshot.child("connections").child("like").hasChild(currentUId) && snapshot.child("sex").getValue().toString().equals(oppositeUserSex)) {
                                String profileImageUrl = "default";
                                if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                    profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                }
                                String userId = snapshot.getKey();
                                String name = snapshot.child("name").getValue() != null ? snapshot.child("name").getValue().toString() : "";
                                String age = snapshot.child("age").getValue() != null ? snapshot.child("age").getValue().toString() : "";
                                String race = snapshot.child("race").getValue() != null ? snapshot.child("race").getValue().toString() : "";
                                String bio = snapshot.child("bio").getValue() != null ? snapshot.child("bio").getValue().toString() : "";
                                String location = snapshot.child("location").getValue() != null ? snapshot.child("location").getValue().toString() : "";

                                if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                    profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                }

                                cards item = new cards(userId, name, age, race, bio, location, profileImageUrl);

                                // cards item = new cards(snapshot.getKey(), snapshot.child("name").getValue().toString(), snapshot.child("age").getValue().toString(), snapshot.child("race").getValue().toString(), snapshot.child("bio").getValue().toString(), snapshot.child("location").getValue().toString(), profileImageUrl);
                                rowItems.add(item);
                                arrayAdapter.notifyDataSetChanged();
                            }
                        }
                    }


                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        thread.start();
    }


//    public void logoutUser(View view) {
//        mAuth.signOut();
//        Intent intent = new Intent(MainActivity.this, ChooseLoginRegistrationActivity.class);
//        startActivity(intent);
//        finish();
//    }

//    public void goToSettings(View view) {
//        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//        startActivity(intent);
//
//    }

//        public void Location(View view) {
//        Intent intent = new Intent(MainActivity.this, LocationL.class);
//        startActivity(intent);
//
//    }
//
//    public void goToMatches(View view) {
//        Intent intent = new Intent(MainActivity.this, MatchesActivity.class);
//        startActivity(intent);
//        return;
//    }

}