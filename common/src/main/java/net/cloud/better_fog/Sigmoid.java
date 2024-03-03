package net.cloud.better_fog;

public class Sigmoid {
    interface funcs {
        double bettersig(double x);
        double mirrory(double x);
    }

    public static double betterSigmoid(double x, double minx, double maxx, double starty, double endy, double minpick, double maxpick) {
        funcs Funcs = new funcs() {
            @Override
            public double bettersig(double x) {
                return (Sigmoid(((Math.abs(maxpick-minpick))/(Math.abs(maxx-minx)))*(x-minx)+minpick)-Sigmoid(minpick))/(Sigmoid(maxpick)-Sigmoid(minpick))*Math.abs(endy-starty)+starty;
            }

            @Override
            public double mirrory(double x) {
                if (starty > endy) {
                    return -bettersig(x)+bettersig(minx)+starty;
                } else {
                    return bettersig(x);
                }
            }
        };
        return Funcs.mirrory(x);
    }

    public static double Sigmoid(double x) {
        return 1 / (1 + Math.pow(Math.E, (-1) * x));
    }
}
