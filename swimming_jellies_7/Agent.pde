// Jelly umbrella main body
// draws a hemisphere

class Agent {
  
  float radius = 300.0;
  float ha = 200.0;
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
    fpsSpeedFactor = random(18.0, 20.0);
    
    textureMode(NORMALIZED);
  }
  
  
  void update() {
    // expand / contract motion
    // using sin & cos waves
    fpsSpeed = frameCount / fpsSpeedFactor;
    radius = (radius_ini * 0.5) +  (1.0 + sin(fpsSpeed)) * (radius_ini * 0.2);
    ha     = (ha_ini * 0.9)     +  (1.0 + cos(fpsSpeed)) * (ha_ini * 0.1);
  }
  
  
  
  void display() {
    //noStroke();
    //noFill();
    
    // builds hemisphere, using bezier points
    for (int i = 0; i < segments; i++) {
      beginShape(TRIANGLE_STRIP);
      texture(skin);
      for (int j = 0; j < steps + 1; j++) {
        float t = (float)j / steps;
        float theta = TWO_PI / segments;
        float a = i * theta;
        
        // bezier control points
        float cp1 = 1.0 + cos(fpsSpeed) * 0.3;
        float cp2 = 1.0 + cos(fpsSpeed) * 0.2;
        
        // UV texture map coordinates
        float u = (float)i / segments;
        float bu = (float)(i + 1) / segments;
        
        
        x = cos(a) * radius;
        y = ha * t;
        z = sin(a) * radius;
        float vx = bezierPoint(0.0, x * cp1, x * cp2, x, t);
        float vy = bezierPoint(0.0,       y,       y, y, t);
        float vz = bezierPoint(0.0, z * cp1, z * cp2, z, t);
        if (isVertexNormalActive) normal(vx, vy, vz);
        vertex(vx, vy, vz, u, t);
        
        x = cos(a + theta) * radius;
        y = ha * t;
        z = sin(a + theta) * radius;
        vx = bezierPoint(0.0, x * cp1, x * cp2, x, t);
        vy = bezierPoint(0.0,       y,       y, y, t);
        vz = bezierPoint(0.0, z * cp1, z * cp2, z, t);
        if (isVertexNormalActive) normal(vx, vy, vz);
        vertex(vx, vy, vz, bu, t);
      }
      endShape();
    }
    
    // Draws XYZ axis for reference only
    if (drawAxis) {
      float axisRadius = 60.0;
      stroke(255, 0, 0);
      line(0.0, 0.0, axisRadius, 0.0);
      stroke(0, 255, 0);
      line(0.0, 0.0, 0.0, axisRadius);
      stroke(0, 0, 255);
      line(0.0, 0.0, 0.0, 0.0, 0.0, axisRadius);
    }
  }
  
}

