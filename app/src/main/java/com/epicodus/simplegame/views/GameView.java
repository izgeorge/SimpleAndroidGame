package com.epicodus.simplegame.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.epicodus.simplegame.R;
import com.epicodus.simplegame.models.Boat;
import com.epicodus.simplegame.models.Bubble;
import com.epicodus.simplegame.models.Dolphin;
import com.epicodus.simplegame.models.Harpoon;
import com.epicodus.simplegame.models.Player;
import com.epicodus.simplegame.models.Seaweed;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {
    public static final String TAG = GameView.class.getSimpleName();

    //Game State Constants
    public static final int GAME_START = 0;
    public static final int GAME_PLAYING = 1;
    public static final int GAME_UPGRADING = 2;
    public static final int GAME_OVER = 3;

    //Game Essentials
    Thread gameThread = null;
    SurfaceHolder ourHolder;
    volatile boolean playing;
    Canvas canvas;
    Paint paint;
    long fps;
    private
    float screenX;
    float screenY;
    int gameState;
    int score;
    Context mContext;

    //Joystick variables
    float pointerX;
    float pointerY;
    float circleXPosition;
    float circleYPosition;
    float circleDefaultX;
    float circleDefaultY;
    float deltaX;
    float deltaY;
    float distance;
    float theta;
    float joystickRadius;

    //Models
    Player player;
    ArrayList<Harpoon> harpoons = new ArrayList<>();
    ArrayList<Dolphin> dolphins = new ArrayList<>();
    ArrayList<Seaweed> seaweeds = new ArrayList<>();
    Bubble bubble;
    Boat boat;

    //Bitmaps/animation variables
    Bitmap fillBubbleMeter;
    Bitmap bubbleMeter;
    boolean isMoving = false;

    //Upgrade button positions
    float upgradeX;
    float upgradeHarpoonY;
    float upgradeOxygenY;
    float upgradeSpeedY;
    float upgradeLungsY;
    float upgradeButtonRadius;
    float doneUpgradingX;
    float doneUpgradingY;
    float doneUpgradingWidth;
    float doneUpgradingHeight;

    //Other
    float scrollSpeed;
    Random randomNumberGenerator;

    public GameView(Context context, float x, float y) {
        super(context);
        mContext = context;
        gameState = GAME_START;
        ourHolder = getHolder();
        paint = new Paint();
        screenX = x;
        screenY = y;

        //Setup Joystick
        circleDefaultX = (float) (0.15*screenX);
        circleDefaultY = (float) (0.78*screenY);
        joystickRadius = (float) .1*screenY;

        //Setup Upgrade Buttons
        upgradeX = 37*screenX/55;
        upgradeHarpoonY = 14*screenY/60;
        upgradeOxygenY = 20*screenY/60;
        upgradeSpeedY = 26*screenY/60;
        upgradeLungsY = 32*screenY/60;
        upgradeButtonRadius = 3*screenY/120;

        doneUpgradingX = 16*screenX/20;
        doneUpgradingY = 17*screenY/20;
        doneUpgradingHeight = 2*screenY/20;
        doneUpgradingWidth = 3*screenX/20;

        //Initialize camera movement
        scrollSpeed = screenX/20;
    }

    public void prepareLevel(Context context) {
        //clear variables
        dolphins.clear();
        harpoons.clear();
        seaweeds.clear();

        //Initialize Models
        player = new Player(context, screenX, screenY);

        for (int i=0; i < 10; i++) {
            seaweeds.add(new Seaweed(context, screenX, screenY));
            seaweeds.get(i).isVisible = false;
            seaweeds.get(i).resetX();
        }


        for (int i=0; i < 3; i++){
            harpoons.add(new Harpoon(context, screenX, screenY));
        }

        for (int i = 0; i < 4; i++) {
            dolphins.add(new Dolphin(context, screenX, screenY));
        }

        bubble = new Bubble(screenX, screenY, context);
        boat = new Boat(context, screenX, screenY);



        //Setup game variables
        randomNumberGenerator = new Random();
        score = 0;
        pointerX = circleDefaultX;
        pointerY = circleDefaultY;

        //Setup Bitmaps
        bubbleMeter = BitmapFactory.decodeResource(getResources(), R.drawable.bubblemeter);
        bubbleMeter = Bitmap.createScaledBitmap(bubbleMeter, (int) screenX/40, (int) screenY/30, false);
        fillBubbleMeter = BitmapFactory.decodeResource(getResources(), R.drawable.fillbubblemeter);
        fillBubbleMeter = Bitmap.createScaledBitmap(fillBubbleMeter, (int) screenX/40, (int) screenY/30, false);

    }

    @Override
    public void run() {
        long timeThisFrame;
        while(playing) {
            long startFrameTime = System.currentTimeMillis();
            update();
            draw();
            timeThisFrame = System.currentTimeMillis()-startFrameTime;
            if(timeThisFrame > 0) {
                fps = 1000/timeThisFrame;
            }
        }
    }

    public void update() {

        if(gameState == GAME_PLAYING) {

            //Update boat
            boat.getCurrentFrame();

            //Calculate joystick position
            deltaX = pointerX-circleDefaultX;
            deltaY = pointerY-circleDefaultY;
            distance = (float) Math.sqrt((deltaX*deltaX) + (deltaY*deltaY));
            theta = (float) Math.atan2(deltaY, deltaX);

            if(distance <= joystickRadius) {
                circleXPosition = pointerX;
                circleYPosition = pointerY;
            } else {
                circleXPosition = (float)(circleDefaultX + (joystickRadius)*Math.cos(theta));
                circleYPosition = (float)(circleDefaultY + (joystickRadius)*Math.sin(theta));
            }

            //Generate Dolphins
            int randomDolphinNumber = randomNumberGenerator.nextInt(250);
            if (randomDolphinNumber == 249) {
                for(int i = 0; i < dolphins.size(); i++) {
                    if(!dolphins.get(i).isVisible) {
                        float randomY = randomNumberGenerator.nextFloat()*(screenY-(screenY/10));
                        dolphins.get(i).generate(randomY);
                        dolphins.get(i).isDead = false;
                        Log.d("dolphin dead? ", dolphins.get(i).isDead+"");
                        break;
                    }
                }
            }

            //Generate Seaweed
            int randomSeaweedNumber = randomNumberGenerator.nextInt(350);
            if (randomSeaweedNumber == 349) {
                for(int i=0; i < seaweeds.size(); i++) {
                    if(!seaweeds.get(i).isVisible) {
                        seaweeds.get(i).generate();
                        Log.d("generated", "seaweed");
                        break;
                    }
                }
            }

            //Update player position
            player.update(fps, circleXPosition, circleYPosition, circleDefaultX, circleDefaultY, scrollSpeed);
            player.getCurrentFrame();

            //Check player oxygen level
            if(player.getOxygenLevel() == 0){
                gameState = GAME_OVER;
            }

            //Update harpoons
            for(int i = 0; i < harpoons.size(); i++){
                if(harpoons.get(i).isVisible){
                    harpoons.get(i).update(fps, scrollSpeed);

                    //Check for collision between player and harpoon
                    if (RectF.intersects(harpoons.get(i).getRect(), player.getRect())) {
                        if (!harpoons.get(i).isShot) {
                            harpoons.get(i).deadDolphin = null;
                            harpoons.get(i).isVisible = false;
                            harpoons.get(i).isAngled = false;
                        }
                    }

                    //Check for collision between harpoons and dolphins
                    for (int j=0; j<dolphins.size(); j++) {
                        if(dolphins.get(j).isVisible) {
                            if(RectF.intersects(dolphins.get(j).getHitbox(), harpoons.get(i).getRect())) {
                                if(!harpoons.get(i).isAHit) {
                                    if(!dolphins.get(j).isDead) {
                                        harpoons.get(i).isShot = false;
                                        dolphins.get(j).isDead = true;
                                        harpoons.get(i).isAHit = true;
                                        dolphins.get(j).killHarpoon = harpoons.get(i);
                                        harpoons.get(i).deadDolphin = dolphins.get(j);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Update dolphins
            for (int i = 0; i < dolphins.size(); i++) {
                if(dolphins.get(i).isVisible()) {
                    dolphins.get(i).update(fps, scrollSpeed);
                    dolphins.get(i).getCurrentFrame();

                    //Check for collision between player and dolphins
                    if(RectF.intersects(dolphins.get(i).getHitbox(), player.getRect())) {
                        if(!dolphins.get(i).isDead) {
                            gameState = GAME_OVER;
                        } else {
                            dolphins.get(i).isVisible = false;
                            dolphins.get(i).killHarpoon = null;
                            dolphins.get(i).isDead = false;
                            score++;
                        }
                    }
                }
            }

            //Update seaweeds
            for (int i=0; i<seaweeds.size(); i++) {
                if(seaweeds.get(i).isVisible) {
                    seaweeds.get(i).update(scrollSpeed, fps);
                    seaweeds.get(i).getCurrentFrame();
                }
            }

            //Update bubble
            if(bubble.isVisible){
                bubble.update(scrollSpeed, fps);
                bubble.getCurrentFrame();

                //Check for collision between player and bubble
                if(RectF.intersects(bubble.getRect(), player.getRect())){
                    bubble.setVisible(false);
                    if(player.getOxygenLevel() < 5){
                        player.setOxygenLevel();
                    }
                }
            } else {
                if(randomNumberGenerator.nextInt(1000) == 999){
                    float randomY = randomNumberGenerator.nextFloat()*(screenY-(screenY/10));
                    bubble.generate(randomY);
                }
            }

            //Check for collision between player and boat
            if(RectF.intersects(player.getRect(), boat.getRect())) {
                gameState = GAME_UPGRADING;
            }
        }
    }

    public void draw() {
        if (ourHolder.getSurface().isValid()) {
            canvas = ourHolder.lockCanvas();

            //Draw Start Screen
            if (gameState == GAME_START) {
                canvas.drawColor(Color.argb(255, 105, 255, 217));
                paint.setColor(Color.argb(255, 0, 29, 77));
                paint.setTextSize(100);
                canvas.drawText("DEEP FISH", screenX/2-230, screenY/2, paint);
                paint.setTextSize(60);
                canvas.drawText("touch screen to start", screenX/2-250, screenY/2+80, paint);

            //Draw Game
            } else if(gameState == GAME_PLAYING) {
                canvas.drawColor(Color.argb(255, 44, 94, 171));
                paint.setColor(Color.argb(255, 121, 192, 233));
                canvas.drawRect(-(screenY/4), 0, screenX, screenY/20, paint);

                //Draw Score
                paint.setColor(Color.argb(255, 249, 129, 0));
                paint.setTextSize(45);
                canvas.drawText("Score: " + score, 20, 40, paint);

                //Draw Boat
                canvas.drawBitmap(boat.getBitmap(), boat.getFrameToDraw(), boat.getRect(), paint);

                //Draw Oxygen Meter
                int bubbleMeterPosition = (int) screenX/9;
                int bubbleMeterSpacing = bubbleMeterPosition/5;
                for(int i = 0; i < (5-player.getOxygenLevel()); i++) {
                    canvas.drawBitmap(bubbleMeter, bubbleMeterPosition, screenY/15, paint);
                    bubbleMeterPosition -= bubbleMeterSpacing;
                }
                 for(int i = 0; i < player.getOxygenLevel(); i++){
                        canvas.drawBitmap(fillBubbleMeter, bubbleMeterPosition, screenY/15, paint);
                        bubbleMeterPosition -= bubbleMeterSpacing;
                 }

                //Draw Joystick
                canvas.drawCircle(circleDefaultX, circleDefaultY, joystickRadius, paint);
                paint.setColor(Color.argb(255, 37, 25, 255));
                canvas.drawCircle(circleXPosition, circleYPosition, (float) (.07 * screenY), paint);

                //Draw Player
                canvas.drawBitmap(player.getBitmap(), player.getFrameToDraw(), player.getRect(), paint);

                //Draw Harpoons
                for (int i = 0; i < harpoons.size(); i++) {
                    if (harpoons.get(i).isVisible) {
                        if (!harpoons.get(i).isAngled) {
                            canvas.drawBitmap(harpoons.get(i).getBitmap(), harpoons.get(i).getX(), harpoons.get(i).getY(), paint);
                        } else {
                            canvas.save();
                            canvas.rotate(45, harpoons.get(i).getX(), harpoons.get(i).getY());
                            canvas.drawBitmap(harpoons.get(i).getBitmap(), harpoons.get(i).getX(), harpoons.get(i).getY(), paint);
                            canvas.restore();
                        }
                    }
                }

                //Draw Dolphins
                paint.setColor(Color.argb(255, 255, 0, 234));
                for (int i = 0; i < dolphins.size(); i++) {
                    if (dolphins.get(i).isVisible) {
                        canvas.drawBitmap(dolphins.get(i).getBitmap(), dolphins.get(i).getFrameToDraw(), dolphins.get(i).getRect(), paint);
                    }
                }

                //Draw Seaweed
                for (int i = 0; i < seaweeds.size(); i++) {
                    if (seaweeds.get(i).isVisible) {
                        canvas.drawBitmap(seaweeds.get(i).getBitMap(), seaweeds.get(i).getFrameToDraw(), seaweeds.get(i).getRect(), paint);
                    }
                }

                //Draw Bubble
                if (bubble.isVisible) {
                    canvas.drawBitmap(bubble.getBitmap(), bubble.getFrameToDraw(), bubble.getRect(), paint);
                }

            //Draw upgrade screen
            } else if(gameState == GAME_UPGRADING) {
                canvas.drawColor(Color.argb(255, 46, 191, 188));
                paint.setColor(Color.argb(255, 191, 46, 49));
                paint.setTextSize(screenY/10);
                canvas.drawText("Upgrades", screenX/2-screenY/5, 2*screenY/20, paint);

                //Draw Upgrade Titles
                paint.setTextSize(screenY/14);
                canvas.drawText("Harpoons", screenX/25, (5*screenY)/20, paint);
                canvas.drawText("Oxygen Tank", screenX/25, 7*screenY/20, paint);
                canvas.drawText("Swim Speed", screenX/25, 9*screenY/20, paint);
                canvas.drawText("Lung Capacity", screenX/25, 11*screenY/20, paint);

                //Draw Upgrade Boxes
                paint.setColor(Color.argb(255, 114, 46, 191));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                for(int i = 0; i < 10; i++) {
                    canvas.drawRect(18*screenX/55+(i*screenY/19), (4*screenY)/20, 19*screenX/55+(i*screenY/19), (16*screenY)/60, paint);
                }
                for(int i = 0; i < 10; i++) {
                    canvas.drawRect(18*screenX/55+(i*screenY/19), (6*screenY)/20, 19*screenX/55+(i*screenY/19), (22*screenY)/60, paint);
                }
                for(int i = 0; i < 10; i++) {
                    canvas.drawRect(18*screenX/55+(i*screenY/19), (8*screenY)/20, 19*screenX/55+(i*screenY/19), (28*screenY)/60, paint);
                }
                for(int i = 0; i < 10; i++) {
                    canvas.drawRect(18*screenX/55+(i*screenY/19), (10*screenY)/20, 19*screenX/55+(i*screenY/19), (34*screenY)/60, paint);
                }

                //Draw Upgrade Buttons
                paint.setColor(Color.argb(255, 0, 0, 0));
                canvas.drawCircle(37*screenX/55, 14*screenY/60, 3*screenY/120, paint);
                Path path = new Path();
                path.moveTo(37*screenX/55, 15*screenY/60);
                path.lineTo(37*screenX/55, 13*screenY/60);
                path.moveTo(75*screenX/110, 14*screenY/60);
                path.lineTo(73*screenX/110, 14*screenY/60);
                canvas.drawCircle(37*screenX/55, 20*screenY/60, 3*screenY/120, paint);
                path.moveTo(37*screenX/55, 21*screenY/60);
                path.lineTo(37*screenX/55, 19*screenY/60);
                path.moveTo(75*screenX/110, 20*screenY/60);
                path.lineTo(73*screenX/110, 20*screenY/60);
                canvas.drawCircle(37*screenX/55, 26*screenY/60, 3*screenY/120, paint);
                path.moveTo(37*screenX/55, 27*screenY/60);
                path.lineTo(37*screenX/55, 25*screenY/60);
                path.moveTo(75*screenX/110, 26*screenY/60);
                path.lineTo(73*screenX/110, 26*screenY/60);
                canvas.drawCircle(37*screenX/55, 32*screenY/60, 3*screenY/120, paint);
                path.moveTo(37*screenX/55, 31*screenY/60);
                path.lineTo(37*screenX/55, 33*screenY/60);
                path.moveTo(75*screenX/110, 32*screenY/60);
                path.lineTo(73*screenX/110, 32*screenY/60);
                canvas.drawPath(path, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(255, 126, 194, 48));

                canvas.drawRect(16*screenX/20, 17*screenY/20, 19*screenX/20, 19*screenY/20, paint);

            //Draw game over screen
            } else if(gameState == GAME_OVER) {
                canvas.drawColor(Color.argb(255, 105, 255, 217));
                paint.setColor(Color.argb(255, 0, 29, 77));
                paint.setTextSize(100);
                canvas.drawText("GAME OVER", screenX/2-230, screenY/2, paint);
            }
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error: ", "joining thread");
        }
    }

    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (gameState == GAME_START) {
                        prepareLevel(mContext);
                        gameState = GAME_PLAYING;
                    } else if(gameState == GAME_PLAYING) {
                        int actionIndexDown = motionEvent.getActionIndex();
                        if(motionEvent.getX(actionIndexDown) < screenX/2) {

                            //Move joystick if touching left side of screen
                            isMoving = true;
                            player.setFrameLength(200);
                            pointerX = motionEvent.getX(actionIndexDown);
                            pointerY = motionEvent.getY(actionIndexDown);
                        } else {

                            //fire harpoon if touching right side of screen
                            for(int i = 0; i < harpoons.size(); i++){
                                if(!harpoons.get(i).isVisible) {
                                    harpoons.get(i).shoot(player.getX()+(player.getWidth()/3), player.getY()+player.getHeight()/2);
                                    Log.d("Is visible", harpoons.get(i).isAHit+"");
                                    break;
                                }
                            }
                        }
                    } else if(gameState == GAME_UPGRADING) {
                        if(motionEvent.getX()>(upgradeX-upgradeButtonRadius) && motionEvent.getX()< (upgradeX+upgradeButtonRadius)) {
                            if(motionEvent.getY()>(upgradeHarpoonY-upgradeButtonRadius) && motionEvent.getY() < (upgradeHarpoonY+upgradeButtonRadius)) {
                                Log.d(" upgrade", "harpoon");
                            } else if(motionEvent.getY()>(upgradeOxygenY-upgradeButtonRadius) && motionEvent.getY() < (upgradeOxygenY+upgradeButtonRadius)) {
                                Log.d(" upgrade", "oxygen");
                            } else if(motionEvent.getY()>(upgradeSpeedY-upgradeButtonRadius) && motionEvent.getY() < (upgradeSpeedY+upgradeButtonRadius)) {
                                Log.d(" upgrade", "speed");
                            } else if(motionEvent.getY()>(upgradeLungsY-upgradeButtonRadius) && motionEvent.getY() < (upgradeLungsY+upgradeButtonRadius)) {
                                Log.d(" upgrade", "lungs");
                            }
                        } else if(motionEvent.getX() > doneUpgradingX && motionEvent.getX() < doneUpgradingX+doneUpgradingWidth) {
                            if(motionEvent.getY() > doneUpgradingY && motionEvent.getY() < doneUpgradingY + doneUpgradingHeight) {
                                prepareLevel(mContext);
                                gameState = GAME_PLAYING;
                            }
                        }


                    } else if(gameState == GAME_OVER) {
                        gameState = GAME_START;
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    int count = motionEvent.getPointerCount();
                    for(int i = 0; i < count; i++) {
                        if(motionEvent.getX(i) < screenX/2) {
                            pointerX = motionEvent.getX(i);
                            pointerY = motionEvent.getY(i);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if(gameState == GAME_PLAYING) {
                        int actionIndexUp = motionEvent.getActionIndex();
                        if(motionEvent.getX(actionIndexUp) < screenX/2) {
                            isMoving = false;
                            player.setFrameLength(700);
                            pointerX = circleDefaultX;
                            pointerY = circleDefaultY;
                        }
                    }
                    break;
            }

        return true;


    }
}
