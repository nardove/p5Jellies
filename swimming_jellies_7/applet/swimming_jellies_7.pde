// 3D STEERING BEHAVIORS
// by Ricardo Sanchez, Feb 2010
//
// You are free to use this code, but if you do it will be nice to let me know.
//
// Note: toxiclibs use in this build is the feb 2010 previous version.
//

import processing.opengl.*;
import javax.media.opengl.*;

import toxi.physics.*;
import toxi.geom.*;


PGraphicsOpenGL pgl;
GL gl;

VerletPhysics physics;

int NUM_BOIDS = 3;
ArrayList boids;
PImage skin, reef;



void setup() {
  size(640, 780, OPENGL);
  hint(ENABLE_DEPTH_SORT);
  hint(DISABLE_OPENGL_2X_SMOOTH);
  //smooth();

  // setup opengl
  pgl = (PGraphicsOpenGL) g;
  
  // setup physics
  physics = new VerletPhysics();
  physics.gravity = Vec3D.Y_AXIS.scale(0.025);
  physics.friction = 0.05;
  physics.timeStep = 0.8;
  
  // load images
  skin = loadImage("jelly_texture.png");
  reef = loadImage("reef.jpg");
  
  // create jellies
  boids = new ArrayList();
  for (int i = 0; i < NUM_BOIDS; i++) {
    Vec3D location = new Vec3D(width * 0.5, height * 0.65, -100.0);
    float maxSpeed = random(0.5, 1.5);
    float maxForce = random(0.1, 0.3);
    
    boids.add(new Boid(i, location, maxSpeed, maxForce));
  }
  
}


void draw() {
  background(0);
  
  //directionalLight(20, 20, 200, 0, 1, -1);
  //ambientLight(50, 70, 252);
  
  physics.update();
     
  
  gl = pgl.beginGL();
  gl.glDisable(GL.GL_DEPTH_TEST);
  gl.glEnable(GL.GL_BLEND);
  gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
  //gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
  
  // render background image
  image(reef, 0, 0);
  
  // render jellies
  for (int i = 0; i < NUM_BOIDS; i++) {
    Boid b = (Boid)boids.get(i);
    b.run(boids);
  }
  
  pgl.endGL();
  
  //println("fps: " + frameRate);
}

