package ar.edu.itba.sds.algorithm;

import ar.edu.itba.sds.distance.DistanceCalculator;
import ar.edu.itba.sds.model.Particle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CellIndexMethod {
    private static final int[][] FORWARD_OFFSETS = {
            {0, 0},
            {1, 0},
            {0, 1},
            {1, 1},
            {-1, 1}
    };

    private CellIndexMethod() {
    }

    public static Map<Integer, Set<Integer>> findNeighbours(List<Particle> particles, double l, int m, double rc, boolean periodic) {
        List<Particle>[][] cells = buildCells(particles, l, m);
        Map<Integer, Set<Integer>> neighbours = new HashMap<>();
        for (Particle particle : particles) {
            neighbours.put(particle.id(), new HashSet<>());
        }

        for (int row = 0; row < m; row++) {
            for (int column = 0; column < m; column++) {
                for (int[] offset : FORWARD_OFFSETS) {
                    int nextColumn = column + offset[0];
                    int nextRow = row + offset[1];

                    if (periodic) {
                        nextColumn = wrap(nextColumn, m);
                        nextRow = wrap(nextRow, m);
                    } else if (nextColumn < 0 || nextColumn >= m || nextRow < 0 || nextRow >= m) {
                        continue;
                    }

                    compareCells(cells[row][column], cells[nextRow][nextColumn], neighbours, l, rc, periodic);
                }
            }
        }

        return neighbours;
    }

    @SuppressWarnings("unchecked")
    private static List<Particle>[][] buildCells(List<Particle> particles, double l, int m) {
        List<Particle>[][] cells = new List[m][m];
        for (int row = 0; row < m; row++) {
            for (int column = 0; column < m; column++) {
                cells[row][column] = new ArrayList<>();
            }
        }

        double cellLength = l / m;
        for (Particle particle : particles) {
            int column = Math.min((int) (particle.x() / cellLength), m - 1);
            int row = Math.min((int) (particle.y() / cellLength), m - 1);
            cells[row][column].add(particle);
        }
        return cells;
    }

    private static void compareCells(
            List<Particle> firstCell,
            List<Particle> secondCell,
            Map<Integer, Set<Integer>> neighbours,
            double l,
            double rc,
            boolean periodic
    ) {
        boolean sameCell = firstCell == secondCell;
        for (int i = 0; i < firstCell.size(); i++) {
            int start = sameCell ? i + 1 : 0;
            for (int j = start; j < secondCell.size(); j++) {
                Particle first = firstCell.get(i);
                Particle second = secondCell.get(j);
                if (first.id() == second.id()) {
                    continue;
                }
                if (DistanceCalculator.areNeighbours(first, second, rc, periodic, l)) {
                    neighbours.get(first.id()).add(second.id());
                    neighbours.get(second.id()).add(first.id());
                }
            }
        }
    }

    private static int wrap(int value, int size) {
        return Math.floorMod(value, size);
    }
}
