package kr.osam2020.sum;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import kr.osam2020.sum.Fragments.ChatlistFragment;
import kr.osam2020.sum.Fragments.ProfileFragment;
import kr.osam2020.sum.Fragments.UsersFragment;
import kr.osam2020.sum.Model.Users;
import kr.osam2020.sum.Notification.Token;

public class MainActivity extends AppCompatActivity {

    final static String[] PERMISSIONS = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    final static int REQUEST_PERMISSION = 1;
    FirebaseUser firebaseUser;
    DatabaseReference myRef;
    private FirebaseFunctions mFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermissions() == false) {
                ActivityCompat.requestPermissions(this,
                        PERMISSIONS,
                        REQUEST_PERMISSION);
            }
        }

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        myRef = FirebaseDatabase.getInstance().getReference("MyUsers").child(firebaseUser.getUid());

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users users = snapshot.getValue(Users.class);
                Toast.makeText(MainActivity.this, users.getUsername() + "님 환영합니다.", Toast.LENGTH_LONG).show();
                // For Debugging Function
                // updateAllUser();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager viewPager = findViewById(R.id.viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new UsersFragment(), "사용자찾기");
        viewPagerAdapter.addFragment(new ChatlistFragment(), "대화방");
        viewPagerAdapter.addFragment(new ProfileFragment(), "프로필");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setInlineLabel(true);
        tabLayout.getTabAt(0).setIcon(R.drawable.icon_find_focus);
        tabLayout.getTabAt(1).setIcon(R.drawable.icon_chatlist);
        tabLayout.getTabAt(2).setIcon(R.drawable.icon_profile);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {

                    case 0:
                        tab.setIcon(R.drawable.icon_find_focus);
                        tabLayout.getTabAt(1).setIcon(R.drawable.icon_chatlist);
                        tabLayout.getTabAt(2).setIcon(R.drawable.icon_profile);

                        break;
                    case 1:
                        tab.setIcon(R.drawable.icon_chatlist_focus);
                        tabLayout.getTabAt(0).setIcon(R.drawable.icon_find);
                        tabLayout.getTabAt(2).setIcon(R.drawable.icon_profile);

                        break;
                    case 2:
                        tab.setIcon(R.drawable.icon_profile_focus);
                        tabLayout.getTabAt(0).setIcon(R.drawable.icon_find);
                        tabLayout.getTabAt(1).setIcon(R.drawable.icon_chatlist);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.d("BSJ", "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        // Log and toast
                        updateToken(token);
                    }
                });
        mFunctions = FirebaseFunctions.getInstance();
        updateUserIndex("addIndexIntimacy")
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "네트워크 문제가 생겼습니다.", Toast.LENGTH_LONG);
                        }
                    }
                });
        updateUserIndex("addIndexExpert")
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "네트워크 문제가 생겼습니다.", Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    private Task<String> updateUserIndex(String uri) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        return mFunctions
                .getHttpsCallable(uri)
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }


    private void updateToken(String refreshToken) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tokens");

        Token token = new Token(refreshToken);
        databaseReference.child(firebaseUser.getUid()).setValue(token);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.logoutMenu:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
        }
        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }


        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private boolean checkPermissions() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean isPermitted = true;

        for (int result : grantResults)
            if (result == PackageManager.PERMISSION_DENIED)
                isPermitted = false;

        if (requestCode == REQUEST_PERMISSION && isPermitted == false) {
            Toast.makeText(this, "권한을 허가해주십시오.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // For Debugging
    public void updateAllUser() {
        // final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("MyUsers");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    Users user = s.getValue(Users.class);

                    assert user != null;

                    String uid = user.getId();
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("IndexExpert").child(uid);
                    reference.removeValue();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("id", uid);
                    hashMap.put("language_position", randomNum(0, 25));
                    hashMap.put("language_edu", randomNum(0, 25));
                    hashMap.put("language_career", randomNum(0, 25));
                    hashMap.put("language_performance", randomNum(0, 25));
                    hashMap.put("combat_position", randomNum(0, 25));
                    hashMap.put("combat_edu", randomNum(0, 25));
                    hashMap.put("combat_career", randomNum(0, 25));
                    hashMap.put("combat_performance", randomNum(0, 25));
                    hashMap.put("computer_position", randomNum(0, 25));
                    hashMap.put("computer_edu", randomNum(0, 25));
                    hashMap.put("computer_career", randomNum(0, 25));
                    hashMap.put("computer_performance", randomNum(0, 25));
                    hashMap.put("admin_position", randomNum(0, 25));
                    hashMap.put("admin_edu", randomNum(0, 25));
                    hashMap.put("admin_career", randomNum(0, 25));
                    hashMap.put("admin_performance", randomNum(0, 25));
                    hashMap.put("law_position", randomNum(0, 25));
                    hashMap.put("law_edu", randomNum(0, 25));
                    hashMap.put("law_career", randomNum(0, 25));
                    hashMap.put("law_performance", randomNum(0, 25));
                    reference.setValue(hashMap);

                    reference = FirebaseDatabase.getInstance().getReference().child("IndexIntimacy").child(uid);
                    reference.removeValue();
                    HashMap<String, Object> hashMap2 = new HashMap<>();
                    hashMap2.put("id", uid);
                    hashMap2.put("position1", randomNum(1, 20));
                    hashMap2.put("position2", randomNum(1, 20));
                    hashMap2.put("milEdu1", randomNum(1, 20));
                    hashMap2.put("milEdu2", randomNum(1, 20));
                    hashMap2.put("priEdu1", randomNum(1, 20));
                    hashMap2.put("priEdu2", randomNum(1, 20));
                    hashMap2.put("milCareer1", randomNum(1, 20));
                    hashMap2.put("milCareer2", randomNum(1, 20));
                    hashMap2.put("privacy1", randomNum(1, 20));
                    hashMap2.put("privacy2", randomNum(1, 20));
                    reference.setValue(hashMap2);
                }

                Log.d("BSJ", "All Users Updated");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String randomNum(int min, int max) {
        int rand = new Random().nextInt(max+1) + min;
        return String.valueOf(rand);
    }
}
