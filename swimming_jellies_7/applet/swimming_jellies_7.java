import processing.core.*; 
import processing.xml.*; 

import processing.opengl.*; 
import javax.media.opengl.*; 
import toxi.physics.*; 
import toxi.geom.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class swimming_jellies_7 extends PApplet {

// 3D STEERING BEHAVIORS
// by Ricardo Sanchez, Feb 2010
//
// You are free to use this code, but if you do it will be nice to let me know.
//
// Note: toxiclibs use in this build is the feb 2010 previous version.
//








PGraphicsOpenGL pgl;
GL gl;

VerletPhysics physics;

int NUM_BOIDS = 3;
ArrayList boids;
PImage skin, reef;



public void setup() {
  size(640, 780, OPENGL);
  hint(ENABLE_DEPTH_SORT);
  hint(DISABLE_OPENGL_2X_SMOOTH);
  //smooth();

  // setup opengl
  pgl = (PGraphicsOpenGL) g;
  
  // setup physics
  physics = new VerletPhysics();
  physics.gravity = Vec3D.Y_AXIS.scale(0.025f);
  physics.friction = 0.05f;
  physics.timeStep = 0.8f;
  
  // load images
  skin = loadImage("jelly_texture.png");
  reef = loadImage("reef.jpg");
  
  // create jellies
  boids = new ArrayList();
  for (int i = 0; i < NUM_BOIDS; i++) {
    Vec3D location = new Vec3D(width * 0.5f, height * 0.65f, -100.0f);
    float maxSpeed = random(0.5f, 1.5f);
    float maxForce = random(0.1f, 0.3f);
    
    boids.add(new Boid(i, location, maxSpeed, maxForce));
  }
  
}


public void draw() {
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

// Jelly umbrella main body
// draws a hemisphere

class Agent {
  
  float radius = 100.0f;
  float ha = 100.0f;
  float radius_ini;
  float ha_ini;
  float x, y, z;
  float fpsSpeed;
  float fpsSpeedFactor;
  
  int segments;
  int steps;
  
  boolean isVertexNormalActive = false;
  boolean drawAxis = false;
  
  
  Agent(float _radius, float _height, int _numSegments, int _steps) {
    radius = _radius;
    ha = -_height;
    radius_ini = radius;
    ha_ini = ha;
    
    segments = _numSegments;
    steps = _steps;
    
    // controls expand/contract motion speed
    fpsSpeedFactor = random(18.0f, 20.0f);
    
    textureMode(NORMALIZED);
  }
  
  
  public void update() {
    // expand / contract motion
    // using sin & cos waves
    fpsSpeed = frameCount / fpsSpeedFactor;
    radius = (radius_ini * 0.5f) +  (1.0f + sin(fpsSpeed)) * (radius_ini * 0.2f);
    ha     = (ha_ini * 0.9f)     +  (1.0f + cos(fpsSpeed)) * (ha_ini * 0.1f);
  }
  
  
  
  public void display() {
    noStroke();
    noFill();
    
    // builds hemisphere, using bezier points
    for (int i = 0; i < segments; i++) {
      beginShape(TRIANGLE_STRIP);
      texture(skin);
      for (int j = 0; j < steps + 1; j++) {
        float t = (float)j / steps;
        float theta = TWO_PI / segments;
        float a = i * theta;
        
        // bezier control points
        float cp1 = 1.0f + cos(fpsSpeed) * 0.3f;
        float cp2 = 1.0f + cos(fpsSpeed) * 0.2f;
        
        // UV texture map coordinates
        float u = (float)i / segments;
        float bu = (float)(i + 1) / segments;
        
        
        x = cos(a) * radius;
        y = ha * t;
        z = sin(a) * radius;
        float vx = bezierPoint(0.0f, x * cp1, x * cp2, x, t);
        float vy = bezierPoint(0.0f,       y,       y, y, t);
        float vz = bezierPoint(0.0f, z * cp1, z * cp2, z, t);
        if (isVertexNormalActive) normal(vx, vy, vz);
        vertex(vx, vy, vz, u, t);
        
        x = cos(a + theta) * radius;
        y = ha * t;
        z = sin(a + theta) * radius;
        vx = bezierPoint(0.0f, x * cp1, x * cp2, x, t);
        vy = bezierPoint(0.0f,       y,       y, y, t);
        vz = bezierPoint(0.0f, z * cp1, z * cp2, z, t);
        if (isVertexNormalActive) normal(vx, vy, vz);
        vertex(vx, vy, vz, bu, t);
      }
      endShape();
    }
    
    // Draws XYZ axis for reference only
    if (drawAxis) {
      float axisRadius = 60.0f;
      stroke(255, 0, 0);
      line(0.0f, 0.0f, axisRadius, 0.0f);
      stroke(0, 255, 0);
      line(0.0f, 0.0f, 0.0f, axisRadius);
      stroke(0, 0, 255);
      line(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, axisRadius);
    }
  }
  
}

// Steering Behaviors
// Based on Daniel Shiffman Nature Of Code tutorial site
// http://www.shiffman.net/teaching/nature/steering/
// 
// 3D dimension (Z) added plus toxiclib Vec3D class
// 

class Boid {

  Vec3D location;
  Vec3D velocity;
  Vec3D acceleration;

  float r;
  float maxForce;
  float maxSpeed;
  float wandertheta;

  int id;
  int NUM_TAILS;
  Tail[] tails;
  Agent agent;
  
  
  
  Boid (int _id, Vec3D _location, float _maxSpeed, float _maxForce) {
    id = _id;
    location = new Vec3D(_location);
    velocity = new Vec3D(random(-1.0f, 1.0f), random(-1.0f, 1.0f), random(-1.0f, 1.0f));
    acceleration = new Vec3D(0.0f, 0.0f, 0.0f);
    maxForce = _maxForce;
    maxSpeed = _maxSpeed;
    
    // setup agent and tails
    // agent(radius, height, horizontal resolution, vertical resolution)
    agent = new Agent(70.0f, 40.0f, 18, 8);
    NUM_TAILS = agent.segments;
    tails = new Tail[NUM_TAILS];
    for (int i = 0; i < NUM_TAILS; i++) tails[i]  = new Tail(i + (id * NUM_TAILS), location);
    
  }
  

  // Steer
  public Vec3D steer(Vec3D target, boolean slowdown) {
    Vec3D steer;
    Vec3D desired = new Vec3D(target.subSelf(location));
    float d = desired.magnitude();

    if (d > 0.0f) {
      desired.normalize();

      if (slowdown && d < 100.0f) desired.scaleSelf(maxSpeed * (d / 100.0f));
      else desired.scaleSelf(maxSpeed);

      steer = desired.subSelf(velocity);
      steer.limit(maxForce);
    }
    else steer = new Vec3D(0.0f, 0.0f, 0.0f);

    return steer;
  }
  
  
  // Seek
  public void seek(Vec3D target) {
    acceleration.addSelf(steer(target, false));
  }
  
  
  // Arrive
  public void arrive(Vec3D target) {
    acceleration.addSelf(steer(target, true));
  }
  
  
  // Flee
  public void flee(Vec3D target) {
    acceleration.subSelf(steer(target, false));
  }
  
  
  // Wander
  public void wander() {
    float wanderR = 5.0f;
    float wanderD = 100.0f;
    float change = 0.025f;

    wandertheta += random(-change, change);

    Vec3D circleLocation = new Vec3D(velocity);
    circleLocation.normalize();
    circleLocation.scaleSelf(wanderD);
    circleLocation.addSelf(location);

    Vec3D circleOffset = new Vec3D(wanderR * cos(wandertheta),
    wanderR * sin(wandertheta),
    wanderR * tan(wandertheta));

    Vec3D target = new Vec3D(circleLocation.addSelf(circleOffset));

    seek(target);
  }
  
  
  // Separation
  public Vec3D separate (ArrayList boids) {
    float desiredseparation = 25.0f;
    Vec3D steer = new Vec3D(0.0f, 0.0f, 0.0f);
    int count = 0;
    // For every boid in the system, check if it's too close
    for (int i = 0 ; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      // If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
      if ((d > 0.0f) && (d < desiredseparation)) {
        // Calculate vector pointing away from neighbor
        Vec3D diff = location.sub(other.location);
        diff.normalize();
        diff.scaleSelf(1.0f / d);        // Weight by distance
        steer.addSelf(diff);
        count++;            // Keep track of how many
      }
    }
    // Average -- divide by how many
    if (count > 0) steer.scaleSelf(1.0f / (float)count);
    

    // As long as the vector is greater than 0
    if (steer.magnitude() > 0.0f) {
      // Implement Reynolds: Steering = Desired - Velocity
      steer.normalize();
      steer.scaleSelf(maxSpeed);
      steer.subSelf(velocity);
      steer.limit(maxForce);
    }
    return steer;
  }
  
  
  // Alignment
  // For every nearby boid in the system, calculate the average velocity
  public Vec3D align (ArrayList boids) {
    float neighbordist = 50.0f;
    Vec3D steer = new Vec3D(0.0f, 0.0f, 0.0f);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      if ((d > 0.0f) && (d < neighbordist)) {
        steer.addSelf(other.velocity);
        count++;
      }
    }
    if (count > 0) steer.scaleSelf(1.0f / (float)count);

    // As long as the vector is greater than 0
    if (steer.magnitude() > 0.0f) {
      // Implement Reynolds: Steering = Desired - Velocity
      steer.normalize();
      steer.scaleSelf(maxSpeed);
      steer.subSelf(velocity);
      steer.limit(maxForce);
    }
    return steer;
  }
  
  
  // Cohesion
  // For the average location (i.e. center) of all nearby boids, calculate steering vector towards that location
  public Vec3D cohesion (ArrayList boids) {
    float neighbordist = 50.0f;
    Vec3D sum = new Vec3D(0.0f, 0.0f, 0.0f);   // Start with empty vector to accumulate all locations
    int count = 0;
    for (int i = 0 ; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      if ((d > 0.0f) && (d < neighbordist)) {
        sum.addSelf(other.location); // Add location
        count++;
      }
    }
    if (count > 0) {
      sum.scaleSelf(1.0f / (float)count);
      return steer(sum, false);  // Steer towards the location
    }
    return sum;
  }
  
  
  // Flock
  // We accumulate a new acceleration each time based on three rules
  public void flock(ArrayList boids) {
    Vec3D sep = separate(boids);   // Separation
    Vec3D ali = align(boids);      // Alignment
    Vec3D coh = cohesion(boids);   // Cohesion
    // Arbitrarily weight these forces
    sep.scaleSelf(1.5f);
    ali.scaleSelf(1.0f);
    coh.scaleSelf(1.0f);
    // Add the force vectors to acceleration
    acceleration.add(sep);
    acceleration.add(ali);
    acceleration.add(coh);
  }
  
  
  // Avoid walls
  public Vec3D avoidWall(Vec3D target) { 
    Vec3D new_steer = new Vec3D();
    Vec3D new_location = new Vec3D(location);
    new_steer = new_location.subSelf(target);
    new_steer.scaleSelf(1.0f / sq(location.distanceTo(target)));

    return new_steer; 
  }

  public void checkWalls() {
    Vec3D fleeVec;
    float fleeFactor = 1.0f;
    
    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, height + 200.0f, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, -300.0f, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);
    
    fleeVec = new Vec3D(avoidWall(new Vec3D(width, location.y, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(-200.0f, location.y, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, location.y, 300.0f)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, location.y, -300.0f)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);
  }
  
  
  public void run(ArrayList boids) {
    flock(boids);
    update();
    checkWalls();
    display();
  }
  
  
  public void update() {
    velocity.addSelf(acceleration);
    velocity.limit(maxSpeed);
    location.addSelf(velocity);
    acceleration.scaleSelf(0.0f);

    agent.update();
  }
  
  
  public void display() {
    noStroke();
    
    // Rotation vectors
    // use to perform orientation to velocity vector
    Vec3D new_dir = new Vec3D(velocity);
    new_dir.normalize();
    
    Vec3D new_up = new Vec3D(0.0f, 1.0f, 0.0f);
    new_up.normalize();
    
    Vec3D new_side = new_dir.cross(new_up);
    new_side.normalize();
    
    float dotP  = new_dir.dot(new_up);
    float angle = new_dir.angleBetween(new_up, true); 
    
    pushMatrix();
    // update location
    translate(location.x, location.y, location.z);
    // orientation to velocity
    rotate(-angle, new_side.x, new_side.y, new_side.z);
    agent.display();
    popMatrix();
    
    // attach head particle to agent base
    float theta = TWO_PI / (float)NUM_TAILS;
    for (int i = 0; i < NUM_TAILS; i++) {
      float a = i * theta;
      float x = cos(a) * agent.radius;
      float y =          agent.ha + 5.0f;
      float z = sin(a) * agent.radius;
      
      // rotate tail head particles, so they align to the
      // base of the umbrella aperture
      Vec3D c = new Vec3D(x, y, z);
      c.rotateAroundAxis(new_side, -angle);
      tails[i].head.set(location.x + c.x, location.y + c.y, location.z + c.z);
      
      tails[i].display();
    }
  }
  
}




// Jelly tendrils, build using toxiclibs
// http://toxiclibs.org/
//
// A tail (tendril) is a set os verlet springs
// They all have 20 particles (joints) but they have random length
// so every tail have a different height

class Tail {
  
  int id;
  int NUM_PARTICLES = 20;
  int restLength;
  
  VerletParticle head;
  VerletParticle[] nodes;
  VerletSpring[] sticks;
  
  
  Tail(int _id, Vec3D _initLocation) {
    id = _id;
    
    // Create different size tails
    restLength = (int)random(8, 20);
    
    nodes = new VerletParticle[NUM_PARTICLES];
    sticks = new VerletSpring[NUM_PARTICLES - 1];
    
    VerletParticle prevParticle = null;
    for(int i = 0; i < NUM_PARTICLES; i++) {
      // create particles
      Vec3D location = new Vec3D(_initLocation);
      VerletParticle particle = new VerletParticle(location, random(0.2f, 0.6f));
      physics.addParticle(particle);
      nodes[i] = particle;
      if (prevParticle != null) {
        VerletSpring spring = new VerletSpring(prevParticle, particle, restLength, 0.5f);
        physics.addSpring(spring);
        sticks[i - 1] = spring;
      }
      prevParticle = particle;
    }
    
    // get the top particle for evey indepentent tail
    head = physics.particles.get(id * NUM_PARTICLES);
    head.lock();
  }
  
  
  public void display() {
    // draw springs
    noFill();
    
    float scRatio = sticks.length * 0.75f;
    
    beginShape(LINES);
    for (int i = 0; i < sticks.length; i++) {
      if (i < scRatio) {
        int sc = PApplet.parseInt((127 / scRatio) * i);
        stroke(255, sc);
      }
      //stroke(255);
      
      VerletSpring s = sticks[i];  
      vertex(s.a.x, s.a.y, s.a.z);
      vertex(s.b.x, s.b.y, s.b.z);
    }
    endShape();
  }
  
}



  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "swimming_jellies_7" });
  }
}
