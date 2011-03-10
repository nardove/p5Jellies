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
      VerletParticle particle = new VerletParticle(location, random(0.2, 0.6));
      physics.addParticle(particle);
      nodes[i] = particle;
      if (prevParticle != null) {
        VerletSpring spring = new VerletSpring(prevParticle, particle, restLength, 0.5);
        physics.addSpring(spring);
        sticks[i - 1] = spring;
      }
      prevParticle = particle;
    }
    
    // get the top particle for evey indepentent tail
    head = physics.particles.get(id * NUM_PARTICLES);
    head.lock();
  }
  
  
  void display() {
    // draw springs
    noFill();
    
    float scRatio = sticks.length * 0.75;
    
    beginShape(LINES);
    for (int i = 0; i < sticks.length; i++) {
      if (i < scRatio) {
        int sc = int((127 / scRatio) * i);
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


