package net.cloud.betterfog;


//todo fix sigmoid
public class Sigmoid {
    private float minx;
    private float maxx;
    private float sizex;
    private float miny;
    private float maxy;
    private float sizey;
    private float p;
    private float sp;
    Sigmoid(float minx, float maxx, float miny, float maxy, float p, float sx) {

        float ssx = (maxx-minx)*sx;

        this.minx = sx > 0 ? minx + ssx : minx;
        this.maxx = sx < 0 ? maxx + ssx : maxx;
        this.sizex = this.maxx - this.minx;

        this.miny = miny;
        this.maxy = maxy;
        this.sizey = this.maxy - this.miny;

        this.p = p;
        this.sp = (float) sigmoid(p);
    }

    public static Sigmoid sigmoid01(float minx, float maxx, float p, float sx) {
        return new Sigmoid(minx, maxx, 0, 1, p, sx);
    }
    public static Sigmoid sigmoid01(float minx, float maxx, float p) {
        return new Sigmoid(minx, maxx, 0, 1, p, 0);
    }

    public static Sigmoid smoothingSig(float p, float sx) {
        return new Sigmoid(0, 1, 0, 1, p, sx);
    }

    public float pickSig(float x) {
        if (p != 0)
            return (float) (sigmoid(p*(2*x-1)) / 2*sp + 0.5);
        else
            return x;
    }

    public float bs(float x) {
        if (sizex > 0) {
            if (x < minx) return miny;
            if (x > maxx) return maxy;
        } else {
            if (x < maxx) return maxy;
            if (x > minx) return miny;
        }

        return sizey*pickSig((x-minx)/sizex)+miny;
    }

    public static double sigmoid(double x) {
        return 2 / (1 + Math.pow(Math.E, -x)) - 1;
    }
}
