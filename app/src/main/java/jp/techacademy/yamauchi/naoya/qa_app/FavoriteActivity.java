package jp.techacademy.yamauchi.naoya.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Toolbar mToolbar;
    private int mGenre = 0;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private Question mQuestion;
    private boolean mFirst = true;
    private ArrayList<Question> mQuestionArrayList;
    private ArrayList<String> mFavoriteArrayList;
    private QuestionsListAdapter mAdapter;
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            String questionUid = dataSnapshot.getKey();

            HashMap map = (HashMap) dataSnapshot.getValue();
            String title = (String) map.get("title");
            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            String imageString = (String) map.get("image");
            byte[] bytes;
            if (imageString != null) {
                bytes = Base64.decode(imageString, Base64.DEFAULT);
            } else {
                bytes = new byte[0];
            }

            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
            HashMap answerMap = (HashMap) map.get("answers");
            if (answerMap != null) {
                for (Object key : answerMap.keySet()) {
                    HashMap temp = (HashMap) answerMap.get((String) key);
                    String answerBody = (String) temp.get("body");
                    String answerName = (String) temp.get("name");
                    String answerUid = (String) temp.get("uid");
                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                    answerArrayList.add(answer);
                }
            }
            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), mGenre, bytes, answerArrayList);


            //多分ここに書く
            for (int i = 0; i < mFavoriteArrayList.size(); i++) {
                if (mFavoriteArrayList.get(i).equals(questionUid)) {
                    mQuestionArrayList.add(question);
                }
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            //変更があったQuestionを探す
            for (Question question : mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
    private ChildEventListener mFavoriteListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            String favoriteQuestionUid = dataSnapshot.getKey();
            mFavoriteArrayList.add(favoriteQuestionUid);


            //選択したジャンルにListenerを登録
            if (mGenreRef != null) {
                mGenreRef.removeEventListener(mEventListener);
            }

            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
            mGenreRef.addChildEventListener(mEventListener);


        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        //ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Questionのインスタンスを渡して質問詳細画面を起動
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();


        //1を規定の選択
        if (mGenre == 0) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_hobby) {
            mToolbar.setTitle("趣味");
            mGenre = 1;
        } else if (id == R.id.nav_life) {
            mToolbar.setTitle("生活");
            mGenre = 2;
        } else if (id == R.id.nav_health) {
            mToolbar.setTitle("健康");
            mGenre = 3;
        } else if (id == R.id.nav_compter) {
            mToolbar.setTitle("コンピューター");
            mGenre = 4;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        //質問リストのクリアしてから再度Adapterにセット
        //ListViewにセットしなおす
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

        mFavoriteArrayList = new ArrayList<String>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference favoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(user.getUid());

        favoriteRef.addChildEventListener(mFavoriteListener);

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //ここから
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);
        mFavoriteArrayList = new ArrayList<String>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference favoriteRef = mDatabaseReference.child(Const.FavoritePATH).child(user.getUid());

        favoriteRef.addChildEventListener(mFavoriteListener);
        //ここまで

        Log.d("result", String.valueOf(resultCode));
    }
}
