package com.aveo.launcher;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class AveoGLSurfaceView extends GLSurfaceView {
    private final AveoRenderer renderer;
    private final ScaleGestureDetector scaleDetector;
    private float lastX;
    private float lastY;
    private boolean dragging = false;

    public AveoGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new AveoRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                renderer.adjustZoom(detector.getScaleFactor());
                return true;
            }
        });
    }

    public AveoRenderer getAveoRenderer() {
        return renderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (event.getPointerCount() > 1) {
            dragging = false;
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    renderer.addRotation(dx * 0.4f, dy * 0.25f);
                    lastX = event.getX();
                    lastY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
