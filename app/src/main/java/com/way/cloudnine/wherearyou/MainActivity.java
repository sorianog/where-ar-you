package com.way.cloudnine.wherearyou;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private ModelRenderable andyRenderable;
    private ArFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        ModelRenderable.builder()
                .setSource(this, Uri.parse("3D_Arrow(2).sfb"))
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                         //   Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;
                        });

        Node node = new Node();
        node.setParent(fragment.getArSceneView().getScene());
        node.setRenderable(andyRenderable);
    }
}
