package dk.dtmftrainer.dtmftrainer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    class Snyder {
        public int l = 0;
        public boolean play = true;
    }

    private char[] chars = new char[] {'1','2','3','4','5','6','7','8','9','0','*','#','A','B','C','D'};

    private HashMap<String, Integer> players = new HashMap<String, Integer>();

    private char[] getRandomDTMFString(int length) {
        SecureRandom sr = new SecureRandom();
        char[] res = new char[length];
        for (int i = 0; i < length; i++) {
            res[i] = chars[sr.nextInt(chars.length)];
        }
        return res;
    }

    private char[] secret_string;

    @Override
    protected void onPause() {
        super.onPause();
        if (mp != null) {
            mp.release();
            mp=null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        players.put("0", R.raw.dtmf0);
        players.put("1", R.raw.dtmf1);
        players.put("2", R.raw.dtmf2);
        players.put("3", R.raw.dtmf3);
        players.put("4", R.raw.dtmf4);
        players.put("5", R.raw.dtmf5);
        players.put("6", R.raw.dtmf6);
        players.put("7", R.raw.dtmf7);
        players.put("8", R.raw.dtmf8);
        players.put("9", R.raw.dtmf9);
        players.put("A", R.raw.dtmfa);
        players.put("B", R.raw.dtmfb);
        players.put("C", R.raw.dtmfc);
        players.put("D", R.raw.dtmfd);
        players.put("*", R.raw.star);
        players.put("#", R.raw.hash);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText solText = (EditText)findViewById(R.id.editTextTextPersonName2);

        Button showSolButton = (Button)findViewById(R.id.button4);
        showSolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solText.setVisibility(View.VISIBLE);
            }
        });

        final EditText lenText = (EditText) findViewById(R.id.editTextNumberSigned);
        secret_string = getRandomDTMFString(8);

        Button generateNewButton = (Button) findViewById(R.id.button2);
        generateNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int random_string_length;
                try {
                    random_string_length = Math.abs(Integer.parseInt(lenText.getText().toString()));
                } catch (Exception e) {
                    random_string_length = 8;
                }
                if (random_string_length > 100) {
                    random_string_length = 100;
                }
                secret_string = getRandomDTMFString(random_string_length);
                solText.setVisibility(View.INVISIBLE);
                solText.setText(new String(secret_string));
            }
        });
        final Context c = this;
        EditText guessText = (EditText)findViewById(R.id.editTextTextPersonName);
        Button verifyButton = (Button)findViewById(R.id.button3);
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String res = "Wrong. Try again";
                Pattern pattern = Pattern.compile("[^0-9A-D\\*#]*", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(guessText.getText().toString());
                String filtered = matcher.replaceAll("");
                char[] guessArr = filtered.toCharArray();
                guessText.setText(filtered);
                if (guessText.getText().toString().toUpperCase().equals(new String(secret_string))) {
                    res = "Correct. You are good";
                } else {
                    int ct = 0;
                    int k = guessArr.length;
                    if (k>secret_string.length) k=secret_string.length;
                    for (int i = 0; i < k; i++) {
                        Log.d("DTMFTRAINER",guessArr[i]+" "+secret_string[i]+" "+(guessArr[i]==secret_string[i]));
                        if (guessArr[i]==secret_string[i]) {
                            ct++;
                        }
                    }
                    res += " You got: "+ct+" correct.";
                }
                playTones(guessArr);
                Toast.makeText(c,res,Toast.LENGTH_LONG).show();
            }
        });

        Button playButton = (Button)findViewById(R.id.button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTones(secret_string);
            }
        });
    }

    private void playTones(char[] input) {
        final Snyder snyd = new Snyder();
        snyd.l = 0;
        snyd.play = true;

        //while (snyd.l < secret_string.length-2) {
        if (snyd.play) {
            snyd.play = false;
            Log.d("DTMFTRAINER","GETTING "+input[snyd.l]+" AT "+snyd.l);
            int tone = players.get(""+input[snyd.l]);
            initTone(tone);
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (snyd.l == input.length-1) {
                        mp.stop();
                        return;
                    }
                    snyd.l++;
                    snyd.play=true;
                    Log.d("DTMFTRAINER","GETTING "+input[snyd.l]+" AT "+snyd.l);
                    int tone = players.get(""+input[snyd.l]);
                    initTone(tone);
                    mp.start();
                }
            });
        }
        //}
    }

    private MediaPlayer mp;

    public Uri getUriToResource(int resId) throws Resources.NotFoundException {
        Resources res = this.getResources();

        Uri resUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + res.getResourcePackageName(resId)
                + '/' + res.getResourceTypeName(resId)
                + '/' + res.getResourceEntryName(resId));
        return resUri;
    }
    private void initTone(int index) {
        Log.d("DTMFTRAINER","Getting tone "+index);
        if (mp != null && mp.isPlaying()) {
            mp.stop();
            //mp.release();

        }
        try {
            if (mp == null) {
                mp = MediaPlayer.create(this, index);
            } else {
                Log.d("DTMFTRAINER",getUriToResource(index).toString());
                mp.reset();
                mp.setDataSource(this, getUriToResource(index));
                mp.prepare();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}