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
    velocity = new Vec3D(random(-1.0, 1.0), random(-1.0, 1.0), random(-1.0, 1.0));
    acceleration = new Vec3D(0.0, 0.0, 0.0);
    maxForce = _maxForce;
    maxSpeed = _maxSpeed;
    
    // setup agent and tails
    // agent(radius, height, horizontal resolution, vertical resolution)
    agent = new Agent(100.0, 60.0, 18, 8);
    NUM_TAILS = agent.segments;
    tails = new Tail[NUM_TAILS];
    for (int i = 0; i < NUM_TAILS; i++) tails[i]  = new Tail(i + (id * NUM_TAILS), location);
    
  }
  

  // Steer
  Vec3D steer(Vec3D target, boolean slowdown) {
    Vec3D steer;
    Vec3D desired = new Vec3D(target.subSelf(location));
    float d = desired.magnitude();

    if (d > 0.0) {
      desired.normalize();

      if (slowdown && d < 100.0) desired.scaleSelf(maxSpeed * (d / 100.0));
      else desired.scaleSelf(maxSpeed);

      steer = desired.subSelf(velocity);
      steer.limit(maxForce);
    }
    else steer = new Vec3D(0.0, 0.0, 0.0);

    return steer;
  }
  
  
  // Seek
  void seek(Vec3D target) {
    acceleration.addSelf(steer(target, false));
  }
  
  
  // Arrive
  void arrive(Vec3D target) {
    acceleration.addSelf(steer(target, true));
  }
  
  
  // Flee
  void flee(Vec3D target) {
    acceleration.subSelf(steer(target, false));
  }
  
  
  // Wander
  void wander() {
    float wanderR = 5.0;
    float wanderD = 100.0;
    float change = 0.025;

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
  Vec3D separate (ArrayList boids) {
    float desiredseparation = 25.0;
    Vec3D steer = new Vec3D(0.0, 0.0, 0.0);
    int count = 0;
    // For every boid in the system, check if it's too close
    for (int i = 0 ; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      // If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
      if ((d > 0.0) && (d < desiredseparation)) {
        // Calculate vector pointing away from neighbor
        Vec3D diff = location.sub(other.location);
        diff.normalize();
        diff.scaleSelf(1.0 / d);        // Weight by distance
        steer.addSelf(diff);
        count++;            // Keep track of how many
      }
    }
    // Average -- divide by how many
    if (count > 0) steer.scaleSelf(1.0 / (float)count);
    

    // As long as the vector is greater than 0
    if (steer.magnitude() > 0.0) {
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
  Vec3D align (ArrayList boids) {
    float neighbordist = 50.0;
    Vec3D steer = new Vec3D(0.0, 0.0, 0.0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      if ((d > 0.0) && (d < neighbordist)) {
        steer.addSelf(other.velocity);
        count++;
      }
    }
    if (count > 0) steer.scaleSelf(1.0 / (float)count);

    // As long as the vector is greater than 0
    if (steer.magnitude() > 0.0) {
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
  Vec3D cohesion (ArrayList boids) {
    float neighbordist = 50.0;
    Vec3D sum = new Vec3D(0.0, 0.0, 0.0);   // Start with empty vector to accumulate all locations
    int count = 0;
    for (int i = 0 ; i < boids.size(); i++) {
      Boid other = (Boid) boids.get(i);
      float d = location.distanceTo(other.location);
      if ((d > 0.0) && (d < neighbordist)) {
        sum.addSelf(other.location); // Add location
        count++;
      }
    }
    if (count > 0) {
      sum.scaleSelf(1.0 / (float)count);
      return steer(sum, false);  // Steer towards the location
    }
    return sum;
  }
  
  
  // Flock
  // We accumulate a new acceleration each time based on three rules
  void flock(ArrayList boids) {
    Vec3D sep = separate(boids);   // Separation
    Vec3D ali = align(boids);      // Alignment
    Vec3D coh = cohesion(boids);   // Cohesion
    // Arbitrarily weight these forces
    sep.scaleSelf(1.5);
    ali.scaleSelf(1.0);
    coh.scaleSelf(1.0);
    // Add the force vectors to acceleration
    acceleration.add(sep);
    acceleration.add(ali);
    acceleration.add(coh);
  }
  
  
  // Avoid walls
  Vec3D avoidWall(Vec3D target) { 
    Vec3D new_steer = new Vec3D();
    Vec3D new_location = new Vec3D(location);
    new_steer = new_location.subSelf(target);
    new_steer.scaleSelf(1.0 / sq(location.distanceTo(target)));

    return new_steer; 
  }

  void checkWalls() {
    Vec3D fleeVec;
    float fleeFactor = 1.0;
    
    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, height + 200.0, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, -300.0, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);
    
    fleeVec = new Vec3D(avoidWall(new Vec3D(width, location.y, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(-200.0, location.y, location.z)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, location.y, 300.0)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);

    fleeVec = new Vec3D(avoidWall(new Vec3D(location.x, location.y, -300.0)));
    fleeVec.scaleSelf(fleeFactor);
    acceleration.addSelf(fleeVec);
  }
  
  
  void run(ArrayList boids) {
    flock(boids);
    update();
    checkWalls();
    display();
  }
  
  
  void update() {
    velocity.addSelf(acceleration);
    velocity.limit(maxSpeed);
    location.addSelf(velocity);
    acceleration.scaleSelf(0.0);

    agent.update();
  }
  
  
  void display() {
    noStroke();
    
    // Rotation vectors
    // use to perform orientation to velocity vector
    Vec3D new_dir = new Vec3D(velocity);
    new_dir.normalize();
    
    Vec3D new_up = new Vec3D(0.0, 1.0, 0.0);
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
      float y =          agent.ha + 5.0;
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




