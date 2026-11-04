package com.aveo.launcher;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AveoRenderer implements GLSurfaceView.Renderer {
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] vp = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    private Program program;
    private Box body;
    private Box roof;
    private Box hood;
    private Box trunk;
    private Box wheel;
    private Box glass;
    private Box base;

    private float angleY = 25f;
    private float angleX = -10f;
    private float cameraDistance = 7.6f;
    private long lastFrameNs = 0L;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.05f, 0.10f, 0.18f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        program = new Program();
        body = new Box(3.6f, 0.70f, 1.55f, new float[]{0.96f, 0.96f, 0.98f, 1f});
        roof = new Box(1.8f, 0.55f, 1.40f, new float[]{0.97f, 0.97f, 0.99f, 1f});
        hood = new Box(0.9f, 0.18f, 1.48f, new float[]{0.94f, 0.94f, 0.96f, 1f});
        trunk = new Box(0.70f, 0.16f, 1.48f, new float[]{0.94f, 0.94f, 0.96f, 1f});
        wheel = new Box(0.42f, 0.42f, 0.22f, new float[]{0.10f, 0.11f, 0.13f, 1f});
        glass = new Box(1.55f, 0.42f, 1.30f, new float[]{0.55f, 0.72f, 0.86f, 0.92f});
        base = new Box(5.0f, 0.06f, 3.0f, new float[]{0.17f, 0.20f, 0.27f, 1f});
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 45f, ratio, 1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        if (lastFrameNs == 0L) lastFrameNs = now;
        float dt = (now - lastFrameNs) / 1_000_000_000f;
        lastFrameNs = now;
        angleY += dt * 10f; // rotation légère continue

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.setLookAtM(view, 0, 0f, 2.2f, cameraDistance, 0f, 0.5f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0);

        drawBase();
        drawCar();
    }

    public void addRotation(float dx, float dy) {
        angleY += dx;
        angleX += dy;
        if (angleX > 20f) angleX = 20f;
        if (angleX < -35f) angleX = -35f;
    }

    public void adjustZoom(float factor) {
        cameraDistance /= factor;
        if (cameraDistance < 4.8f) cameraDistance = 4.8f;
        if (cameraDistance > 11.5f) cameraDistance = 11.5f;
    }

    private void drawBase() {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0f, -0.65f, 0f);
        drawBox(base, model);
    }

    private void drawCar() {
        float[] car = new float[16];
        Matrix.setIdentityM(car, 0);
        Matrix.rotateM(car, 0, angleX, 1f, 0f, 0f);
        Matrix.rotateM(car, 0, angleY, 0f, 1f, 0f);

        drawPart(car, body, 0f, 0f, 0f);
        drawPart(car, roof, 0.15f, 0.58f, 0f);
        drawPartScaled(car, hood, -1.35f, 0.20f, 0f, 1f, 1f, 1f);
        drawPartScaled(car, trunk, 1.45f, 0.18f, 0f, 1f, 1f, 1f);
        drawPartScaled(car, glass, 0.10f, 0.68f, 0f, 1f, 1f, 1f);

        drawWheel(car, -1.1f, -0.45f, -0.78f);
        drawWheel(car, -1.1f, -0.45f, 0.78f);
        drawWheel(car, 1.15f, -0.45f, -0.78f);
        drawWheel(car, 1.15f, -0.45f, 0.78f);
    }

    private void drawWheel(float[] parent, float x, float y, float z) {
        drawPart(parent, wheel, x, y, z);
    }

    private void drawPart(float[] parent, Box box, float x, float y, float z) {
        drawPartScaled(parent, box, x, y, z, 1f, 1f, 1f);
    }

    private void drawPartScaled(float[] parent, Box box, float x, float y, float z, float sx, float sy, float sz) {
        float[] local = new float[16];
        System.arraycopy(parent, 0, local, 0, 16);
        Matrix.translateM(local, 0, x, y, z);
        Matrix.scaleM(local, 0, sx, sy, sz);
        drawBox(box, local);
    }

    private void drawBox(Box box, float[] modelMatrix) {
        Matrix.multiplyMM(mvp, 0, vp, 0, modelMatrix, 0);
        program.use();
        GLES20.glUniformMatrix4fv(program.uMvp, 1, false, mvp, 0);
        GLES20.glUniform4fv(program.uColor, 1, box.color, 0);
        GLES20.glVertexAttribPointer(program.aPosition, 3, GLES20.GL_FLOAT, false, 0, box.vertexBuffer);
        GLES20.glEnableVertexAttribArray(program.aPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, box.vertexCount);
    }

    private static class Program {
        final int program;
        final int aPosition;
        final int uMvp;
        final int uColor;

        Program() {
            String vs = "attribute vec4 aPosition; uniform mat4 uMvp; void main(){ gl_Position = uMvp * aPosition; }";
            String fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor = uColor; }";
            int vShader = compile(GLES20.GL_VERTEX_SHADER, vs);
            int fShader = compile(GLES20.GL_FRAGMENT_SHADER, fs);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vShader);
            GLES20.glAttachShader(program, fShader);
            GLES20.glLinkProgram(program);
            aPosition = GLES20.glGetAttribLocation(program, "aPosition");
            uMvp = GLES20.glGetUniformLocation(program, "uMvp");
            uColor = GLES20.glGetUniformLocation(program, "uColor");
        }

        void use() {
            GLES20.glUseProgram(program);
        }

        static int compile(int type, String code) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }

    private static class Box {
        final FloatBuffer vertexBuffer;
        final int vertexCount;
        final float[] color;

        Box(float width, float height, float depth, float[] color) {
            this.color = color;
            float hw = width / 2f;
            float hh = height / 2f;
            float hd = depth / 2f;
            float[] vertices = new float[]{
                    // Front
                    -hw,-hh, hd,  hw,-hh, hd,  hw, hh, hd,
                    -hw,-hh, hd,  hw, hh, hd, -hw, hh, hd,
                    // Back
                    hw,-hh,-hd, -hw,-hh,-hd, -hw, hh,-hd,
                    hw,-hh,-hd, -hw, hh,-hd,  hw, hh,-hd,
                    // Left
                    -hw,-hh,-hd, -hw,-hh, hd, -hw, hh, hd,
                    -hw,-hh,-hd, -hw, hh, hd, -hw, hh,-hd,
                    // Right
                    hw,-hh, hd, hw,-hh,-hd, hw, hh,-hd,
                    hw,-hh, hd, hw, hh,-hd, hw, hh, hd,
                    // Top
                    -hw, hh, hd, hw, hh, hd, hw, hh,-hd,
                    -hw, hh, hd, hw, hh,-hd, -hw, hh,-hd,
                    // Bottom
                    -hw,-hh,-hd, hw,-hh,-hd, hw,-hh, hd,
                    -hw,-hh,-hd, hw,-hh, hd, -hw,-hh, hd
            };
            vertexCount = vertices.length / 3;
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);
        }
    }
}
