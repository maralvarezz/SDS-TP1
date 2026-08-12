package ar.edu.itba.sds.algorithm;

import ar.edu.itba.sds.model.Particle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellIndexMethodTest {

    @Test
    void findsCloseParticlesAsNeighbours() {
        List<Particle> particles = List.of(
                new Particle(1, 5.0, 5.0, 0.1, 0),
                new Particle(2, 5.5, 5.0, 0.1, 0),
                new Particle(3, 15.0, 15.0, 0.1, 0)
        );

        Map<Integer, Set<Integer>> neighbours = CellIndexMethod.findNeighbours(particles, 20.0, 5, 1.0, false);

        assertTrue(neighbours.get(1).contains(2));
        assertTrue(neighbours.get(2).contains(1));
        assertFalse(neighbours.get(1).contains(3));
        assertFalse(neighbours.get(3).contains(1));
    }

    @Test
    void considersParticleRadiusInDistance() {
        // centros a distancia 1.5, radios 0.3 c/u => borde-borde = 1.5 - 0.6 = 0.9 < rc=1.0
        List<Particle> particles = List.of(
                new Particle(1, 5.0, 5.0, 0.3, 0),
                new Particle(2, 6.5, 5.0, 0.3, 0)
        );

        Map<Integer, Set<Integer>> neighbours = CellIndexMethod.findNeighbours(particles, 20.0, 5, 1.0, false);

        assertTrue(neighbours.get(1).contains(2));
    }

    @Test
    void periodicBoundaryWrapsAroundEdges() {
        List<Particle> particles = List.of(
                new Particle(1, 0.2, 10.0, 0.1, 0),
                new Particle(2, 19.8, 10.0, 0.1, 0)
        );

        Map<Integer, Set<Integer>> nonPeriodic = CellIndexMethod.findNeighbours(particles, 20.0, 5, 1.0, false);
        assertFalse(nonPeriodic.get(1).contains(2));

        Map<Integer, Set<Integer>> periodic = CellIndexMethod.findNeighbours(particles, 20.0, 5, 1.0, true);
        assertTrue(periodic.get(1).contains(2));
    }

    @Test
    void everyParticleHasAnEntryEvenWithoutNeighbours() {
        List<Particle> particles = List.of(
                new Particle(1, 1.0, 1.0, 0.1, 0),
                new Particle(2, 19.0, 19.0, 0.1, 0)
        );

        Map<Integer, Set<Integer>> neighbours = CellIndexMethod.findNeighbours(particles, 20.0, 5, 1.0, false);

        assertEquals(2, neighbours.size());
        assertTrue(neighbours.get(1).isEmpty());
        assertTrue(neighbours.get(2).isEmpty());
    }
}
