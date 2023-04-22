package com.example.woofmatedating;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField, mRaceField, mAgeField, mBioField;

    private Button mConfirm;

    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    private String userId, name, phone, profileImageUrl, userSex, race, age, bio, location;

    private Uri resultUri;
    BottomNavigationView nav;


    private TextView realLocation;
    private TextView mLocationField;
    private SwitchMaterial getLocation;

    private boolean isConfirmed = false;


    FusedLocationProviderClient fusedLocationProviderClient;

    private Uri imageUri;

    // Declare ActivityResultLauncher
    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        mProfileImage.setImageBitmap(bitmap);
                        resultUri = imageUri;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // String userSex = getIntent().getExtras().getString("userSex");
        mNameField = (EditText) findViewById(R.id.name);
        mPhoneField = (EditText) findViewById(R.id.phone);
        mAgeField = (EditText) findViewById(R.id.age);
        mRaceField = (EditText) findViewById(R.id.race);
        mBioField = (EditText) findViewById(R.id.bio);
        mLocationField = (TextView) findViewById(R.id.location);

        //realLocation = (TextView) findViewById(R.id.cityLocation);
        getLocation = findViewById(R.id.GetLocation);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mProfileImage = (ImageView) findViewById(R.id.profileImage);

        mConfirm = (Button) findViewById(R.id.confirm);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

        getUserInfo();

        nav = findViewById(R.id.navBar);
        nav.setSelectedItemId(R.id.setting);
        nav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {

            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (!isConfirmed) {
                    Toast.makeText(SettingsActivity.this, "Please fill in all fields and click confirm", Toast.LENGTH_SHORT).show();
                    return false;
                }
                switch (item.getItemId()) {
                    case R.id.setting:
                        return true;

                    case R.id.chat:
                        Intent intent3 = new Intent(getApplicationContext(), MatchesActivity.class);
                        startActivity(intent3);
                        return true;

                    case R.id.home:
                        Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent2);
                        return true;
                }

                return false;
            }
        });


        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);*/
                // resultLauncher.launch(intent);
                showPhotoAccessWarning();
                isConfirmed = false;
            }
        });




        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (areAllFieldsFilled()) {
                    saveUserInformation();

                    isConfirmed = true;

//                    Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
//                    startActivity(intent);
                    Toast.makeText(SettingsActivity.this, "Settings done, Swipe for puppies!", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(SettingsActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        getLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()){
                    getLocation();
                } else {
                    //realLocation.setVisibility(View.GONE);
                }
            }
        });
        
        final ActivityResultLauncher<Intent> cameraActivityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            mProfileImage.setImageBitmap(bitmap);
                            resultUri = imageUri;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });


        addTextWatchers();

    }

    private void addTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isConfirmed = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        mNameField.addTextChangedListener(textWatcher);
        mPhoneField.addTextChangedListener(textWatcher);
        mRaceField.addTextChangedListener(textWatcher);
        mAgeField.addTextChangedListener(textWatcher);
        mBioField.addTextChangedListener(textWatcher);
        mLocationField.addTextChangedListener(textWatcher);

        


    }


    private boolean areAllFieldsFilled() {
        return !TextUtils.isEmpty(mNameField.getText().toString()) &&
                !TextUtils.isEmpty(mPhoneField.getText().toString()) &&
                !TextUtils.isEmpty(mRaceField.getText().toString()) &&
                !TextUtils.isEmpty(mAgeField.getText().toString()) &&
                !TextUtils.isEmpty(mBioField.getText().toString()) &&
                !TextUtils.isEmpty(mLocationField.getText().toString());
    }

    ///////////
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to get the location
                getLocation();
            } else {
                // Permission denied, display a message or disable the switch
                getLocation.setChecked(false);
                //realLocation.setVisibility(View.GONE);
                // You can show a message to the user to inform them that the permission is required
            }
        }
    }


    private void getLocation(){
        mLocationField.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // here we first check location permission
                if (ActivityCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //  ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                    fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if(location != null){
                                try {
                                    Geocoder geocoder = new Geocoder(SettingsActivity.this, Locale.getDefault());
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    String locationl = addresses.get(0).getAdminArea() + ", " + addresses.get(0).getLocality();
                                    mLocationField.setText(locationl);

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    });

                }else{
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
            }
        }).start();

    }



    private void showPhotoAccessWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Photos")
                .setMessage("WoofMate would like to access your photos or use the camera. Do you want to allow access?")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Show another dialog to choose between Gallery and Camera
                        choosePhotoSource();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }


    private void choosePhotoSource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Photo Source")
                .setItems(new CharSequence[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Open the gallery
                                openGallery();
                                break;
                            case 1:
                                // Open the camera
                                openCamera();
                                break;
                        }
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }



    private File photoFile;

    private void openCamera() {
        Dexter.withContext(SettingsActivity.this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {

                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.Images.Media.TITLE, "New Pic");
                            contentValues.put(MediaStore.Images.Media.DESCRIPTION, "New Pic");
                            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                            // Use the ActivityResultLauncher instead of the deprecated method
                            cameraActivityResultLauncher.launch(intent);
                        } else {
                            Toast.makeText(SettingsActivity.this, "Need permission", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest(); // Continue asking for permissions
                    }
                }).check();
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void getUserInfo() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                            if (map.get("name") != null) {
                                name = map.get("name").toString();
                                mNameField.setText(name);
                            }
                            if (map.get("location") != null) {
                                location = map.get("location").toString();
                                mLocationField.setText(location);
                            }
                            if (map.get("phone") != null) {
                                phone = map.get("phone").toString();
                                mPhoneField.setText(phone);
                            }
                            if (map.get("age") != null) {
                                age = map.get("age").toString();
                                mAgeField.setText(age);
                            }
                            if (map.get("race") != null) {
                                race = map.get("race").toString();
                                mRaceField.setText(race);
                            }
                            if (map.get("bio") != null) {
                                bio = map.get("bio").toString();
                                mBioField.setText(bio);
                            }
                            if (map.get("sex") != null) {
                                userSex = map.get("sex").toString();
                            }
                            if (map.get("profileImageUrl") != null) {
                                profileImageUrl = map.get("profileImageUrl").toString();
                                switch (profileImageUrl) {
                                    case "default":
                                        Glide.with(getApplication()).load(R.mipmap.ic_launcher).into(mProfileImage);
                                        break;
                                    default:
                                        Glide.with(getApplication()).load(profileImageUrl).into(mProfileImage);
                                        break;
                                }
                            }

                            if (areAllFieldsFilled()) {
                                isConfirmed = true;
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void saveUserInformation() {
        name = mNameField.getText().toString();
        phone = mPhoneField.getText().toString();
        age = mAgeField.getText().toString();
        race = mRaceField.getText().toString();
        bio = mBioField.getText().toString();
        location = mLocationField.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name", name);
        userInfo.put("phone", phone);
        userInfo.put("age", age);
        userInfo.put("race", race);
        userInfo.put("bio", bio);
        userInfo.put("location", location);
        mUserDatabase.updateChildren(userInfo);

        if(resultUri != null){
            // 在单独的线程中上传图像
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profileImages").child(userId);
                    Bitmap bitmap = null;

                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                    byte[] data = baos.toByteArray();
                    UploadTask uploadTask = filepath.putBytes(data);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finish();
                            return;
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
                            firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Map newImage = new HashMap();
                                    newImage.put("profileImageUrl", uri.toString());
                                    mUserDatabase.updateChildren(newImage);
                                    finish();
                                    return;
                                }
                            });
                        }
                    });
                }
            }).start();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
            Log.d("SettingsActivity", "Gallery image selected");
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            resultUri = Uri.fromFile(photoFile);
            mProfileImage.setImageURI(resultUri);
            Log.d("SettingsActivity", "Camera image captured");
        } else {
            Log.e("SettingsActivity", "Failed to get image: requestCode=" + requestCode + ", resultCode=" + resultCode);
        }
    }


    public void logoutUser(View view) {
        if (areAllFieldsFilled()){
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(SettingsActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (isConfirmed) {
            super.onBackPressed();
        } else {
            Toast.makeText(SettingsActivity.this, "Please fill in all fields and confirm the information", Toast.LENGTH_SHORT).show();
        }
    }
}