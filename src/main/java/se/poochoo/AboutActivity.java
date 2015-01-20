package se.poochoo;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends Activity {
    private View background;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The activity is being created.
        setContentView(R.layout.activity_about);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int versionNumber = pInfo.versionCode;
            TextView versionText = (TextView)findViewById(R.id.versionName);
            versionText.setText(versionNumber + " " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void trainPressed(View view) {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        ImageView poochooTrain = (ImageView)findViewById(R.id.versionImage);

        ObjectAnimator leftToRightAnim= ObjectAnimator.ofFloat(poochooTrain, "translationX", 0, screenWidth-100);
        leftToRightAnim.setDuration(1500);
        ObjectAnimator rotateWall= ObjectAnimator.ofFloat(poochooTrain, "rotation" , 0, -90);
        rotateWall.setDuration(1000);
        ObjectAnimator rightToTop= ObjectAnimator.ofFloat(poochooTrain, "translationY", 0, -screenHeight+250);
        rightToTop.setDuration(3000);
        ObjectAnimator fallingRotation= ObjectAnimator.ofFloat(poochooTrain, "rotation" , -90, -720);
        fallingRotation.setDuration(1000);
        ObjectAnimator fallingDown= ObjectAnimator.ofFloat(poochooTrain, "translationY", -screenHeight+250, 0);
        fallingDown.setDuration(1000);
        ObjectAnimator fallingSide= ObjectAnimator.ofFloat(poochooTrain, "translationX", screenWidth-100, 0);
        fallingSide.setDuration(1000);

        AnimatorSet bouncer = new AnimatorSet();

        bouncer.play(leftToRightAnim).before(rotateWall);
        bouncer.play(rotateWall).before(rightToTop);
        bouncer.play(rightToTop).before(fallingRotation);
        bouncer.play(rightToTop).before(fallingDown);
        bouncer.play(rightToTop).before(fallingSide);

        bouncer.start();

    }

    public void privacyPolicyButtonClicked (View view){
        String url  = getString(R.string.privacyPolicyUrl);
        Intent policyIntent = new Intent(Intent.ACTION_VIEW);
        policyIntent.setData(Uri.parse(url));
        startActivity(policyIntent);
    }

}
