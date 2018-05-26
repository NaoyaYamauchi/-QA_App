package jp.techacademy.yamauchi.naoya.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

import static android.view.View.GONE;

public class QuestionDetailActivity extends AppCompatActivity {

    FloatingActionButton like;
    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private ArrayList<String> mFavoriteArrayList;
    private DatabaseReference mAnswerRef;
    private boolean mAlreadyLike = false;
    private boolean mFirst = true;
    private ChildEventListener mFavoriteListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            String favoriteQuestionUid = dataSnapshot.getKey();
            mFavoriteArrayList.add(favoriteQuestionUid);

            for (int i = 0; i < mFavoriteArrayList.size(); i++) {
                if (mQuestion.getQuestionUid().equals(mFavoriteArrayList.get(i))) {
                    mAlreadyLike = true;
                    like.setImageResource(R.drawable.sudenilike);
                    break;
                } else {
                    mAlreadyLike = false;
                    like.setImageResource(R.drawable.like);
                }
            }
            Log.d("root", mQuestion.getQuestionUid());
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

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();
            for (Answer answer : mQuestion.getAnswers()) {
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
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
        setContentView(R.layout.activity_question_detail);

        //渡ってきたQuestionオブジェクトの保持
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        //ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    //Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = mDatabaseReference.child(Const.ContentsPATH)
                .child(String.valueOf(mQuestion.getGenre()))
                .child(mQuestion.getQuestionUid())
                .child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        like = (FloatingActionButton) findViewById(R.id.like);
        try {
            Log.d("root", user.getUid());

        } catch (RuntimeException e) {
            like.setVisibility(GONE);
        }

        //firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        DatabaseReference favoriteRef =
                mDatabaseReference
                        .child(Const.FavoritePATH)
                        .child(user.getUid())
                        .child(mQuestion.getQuestionUid())
                        .child(String.valueOf(mQuestion.getGenre()));

        mFavoriteArrayList = new ArrayList<String>();
        DatabaseReference favoriteDatabase = mDatabaseReference.child(Const.FavoritePATH).child(user.getUid());
        favoriteDatabase.addChildEventListener(mFavoriteListener);


        final DatabaseReference favoriteRefData =
                mDatabaseReference
                        .child(Const.FavoritePATH)
                        .child(user.getUid())
                        .child(mQuestion.getQuestionUid())
                        .child(String.valueOf(mQuestion.getGenre()));


        final DatabaseReference finalMDatabaseReference = mDatabaseReference;
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


                if (!mAlreadyLike) {

                    Log.d("root", "未登録");
                    favoriteRefData.setValue(mQuestion.getQuestionUid());
                    //ログインしているときしか表示されないはずなので
                    Snackbar.make(v, "お気に入りに追加しました", Snackbar.LENGTH_LONG).show();
                    mAlreadyLike = true;
                    like.setImageResource(R.drawable.sudenilike);
                } else {
                    DatabaseReference favoriteRef =
                            finalMDatabaseReference
                                    .child(Const.FavoritePATH)
                                    .child(user.getUid())
                                    .child(mQuestion.getQuestionUid());

                    favoriteRef.removeValue();
                    Snackbar.make(v, "お気に入りから削除しました", Snackbar.LENGTH_LONG).show();
                    Log.d("root", "登録済み");
                    mAlreadyLike = false;
                    like.setImageResource(R.drawable.like);
                }

            }


        });
    }
}