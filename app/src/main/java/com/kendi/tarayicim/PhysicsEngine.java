package com.kendi.tarayicim;

public class PhysicsEngine {

    public static class Body {
        public float x, y;           // mevcut konum
        public float velX, velY;     // hız
        public float baseX, baseY;   // ev
        public float mass;           // kütle
        public float radius;         // çarpışma yarıçapı
        public boolean pinned;       // sürükleniyor mu

        // Elastik dönüş katsayısı
        public float springK  = 0.08f;
        public float damping  = 0.75f;
        public float friction = 0.88f;

        public Body(float bx, float by, float mass, float radius) {
            this.x = this.baseX = bx;
            this.y = this.baseY = by;
            this.mass   = mass;
            this.radius = radius;
        }
    }

    private final Body[] bodies;

    public PhysicsEngine(Body[] bodies) {
        this.bodies = bodies;
    }

    // Her frame'de çağrılır
    public void step(float dt) {
        for (Body b : bodies) {
            if (b.pinned) continue;
            applySpring(b);
            integrate(b, dt);
        }
        resolveCollisions();
    }

    // Elastik ip — eve çeker
    private void applySpring(Body b) {
        float dx = b.baseX - b.x;
        float dy = b.baseY - b.y;
        b.velX += dx * b.springK;
        b.velY += dy * b.springK;
    }

    // Euler integrasyonu
    private void integrate(Body b, float dt) {
        b.velX *= b.friction;
        b.velY *= b.friction;
        b.velY += 0.4f * dt; // hafif yerçekimi
        b.x += b.velX * dt;
        b.y += b.velY * dt;
    }

    // Çarpışma çözümü
    private void resolveCollisions() {
        for (int i = 0; i < bodies.length; i++) {
            for (int j = i + 1; j < bodies.length; j++) {
                Body a = bodies[i], b = bodies[j];
                float dx  = b.x - a.x;
                float dy  = b.y - a.y;
                float dist = (float) Math.sqrt(dx*dx + dy*dy);
                float minD = a.radius + b.radius;
                if (dist < minD && dist > 0.01f) {
                    // İtme
                    float nx = dx / dist, ny = dy / dist;
                    float overlap = minD - dist;
                    float totalMass = a.mass + b.mass;
                    float pushA = overlap * (b.mass / totalMass);
                    float pushB = overlap * (a.mass / totalMass);

                    if (!a.pinned) { a.x -= nx * pushA; a.y -= ny * pushA; }
                    if (!b.pinned) { b.x += nx * pushB; b.y += ny * pushB; }

                    // Hız transferi
                    float relVX = b.velX - a.velX;
                    float relVY = b.velY - a.velY;
                    float dot   = relVX * nx + relVY * ny;
                    if (dot < 0) {
                        float impulse = dot * 1.4f; // biraz sekme
                        if (!a.pinned) { a.velX += impulse * nx * (b.mass/totalMass); }
                        if (!b.pinned) { b.velX -= impulse * nx * (a.mass/totalMass); }
                        if (!a.pinned) { a.velY += impulse * ny * (b.mass/totalMass); }
                        if (!b.pinned) { b.velY -= impulse * ny * (a.mass/totalMass); }
                    }
                }
            }
        }
    }

    // Sürükleme
    public void startDrag(int idx, float x, float y) {
        bodies[idx].pinned = true;
        bodies[idx].x = x;
        bodies[idx].y = y;
    }

    public void moveDrag(int idx, float x, float y) {
        if (!bodies[idx].pinned) return;
        float prevX = bodies[idx].x, prevY = bodies[idx].y;
        bodies[idx].x = x;
        bodies[idx].y = y;
        bodies[idx].velX = (x - prevX) * 0.6f;
        bodies[idx].velY = (y - prevY) * 0.6f;
    }

    public void endDrag(int idx, float vx, float vy) {
        bodies[idx].pinned = false;
        bodies[idx].velX = vx * 1.2f; // fling boost
        bodies[idx].velY = vy * 1.2f;
    }

    public boolean isColliding(int i, int j) {
        Body a = bodies[i], b = bodies[j];
        float dx = b.x - a.x, dy = b.y - a.y;
        return Math.sqrt(dx*dx + dy*dy) < (a.radius + b.radius);
    }

    public Body getBody(int idx) { return bodies[idx]; }
}
