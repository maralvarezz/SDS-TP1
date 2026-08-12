package ar.edu.itba.sds.distance;

import ar.edu.itba.sds.model.Particle;

public final class DistanceCalculator {
    private DistanceCalculator() {
    }

    public static double centerDistance(Particle first, Particle second, boolean periodic, double l) {
        double dx = Math.abs(first.x() - second.x());
        double dy = Math.abs(first.y() - second.y());

        if (periodic) {
            dx = Math.min(dx, l - dx);
            dy = Math.min(dy, l - dy);
        }

        return Math.hypot(dx, dy);
    }

    public static boolean areNeighbours(Particle first, Particle second, double rc, boolean periodic, double l) {
        return centerDistance(first, second, periodic, l) < rc + first.radius() + second.radius();
    }
}
