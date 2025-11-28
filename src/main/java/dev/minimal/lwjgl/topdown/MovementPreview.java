package dev.minimal.lwjgl.topdown;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

final class MovementPreview {
    private final Map<GridPosition, Integer> distances;
    private final int maxDistance;

    private MovementPreview(Map<GridPosition, Integer> distances, int maxDistance) {
        this.distances = distances;
        this.maxDistance = maxDistance;
    }

    static MovementPreview calculate(Level level, Unit selected, List<Unit> friendlies, List<Unit> enemies, int maxDistance) {
        if (level == null || selected == null || !selected.isPlaced() || maxDistance <= 0) {
            return null;
        }
        GridPosition start = selected.position();
        Map<GridPosition, Integer> distances = new HashMap<>();
        distances.put(start, 0);
        Queue<PreviewNode> queue = new ArrayDeque<>();
        queue.add(new PreviewNode(start, 0));

        Set<GridPosition> blocked = new HashSet<>();
        addUnitPositions(blocked, friendlies, selected);
        addUnitPositions(blocked, enemies, null);

        while (!queue.isEmpty()) {
            PreviewNode node = queue.poll();
            if (node.distance >= maxDistance) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                GridPosition neighbor = new GridPosition(node.position.col() + direction.dx, node.position.row() + direction.dy);
                if (distances.containsKey(neighbor)) {
                    continue;
                }
                if (!level.isInside(neighbor.col(), neighbor.row()) || !level.isWalkable(neighbor.col(), neighbor.row())) {
                    continue;
                }
                if (blocked.contains(neighbor)) {
                    continue;
                }
                int nextDistance = node.distance + 1;
                distances.put(neighbor, nextDistance);
                queue.add(new PreviewNode(neighbor, nextDistance));
            }
        }

        return new MovementPreview(distances, maxDistance);
    }

    private static void addUnitPositions(Set<GridPosition> blocked, List<Unit> units, Unit exception) {
        if (units == null) {
            return;
        }
        for (Unit unit : units) {
            if (unit == null || !unit.isPlaced()) {
                continue;
            }
            if (exception != null && Objects.equals(unit.id(), exception.id())) {
                continue;
            }
            blocked.add(unit.position());
        }
    }

    boolean isReachable(GridPosition position) {
        return position != null && distances.containsKey(position);
    }

    int distanceTo(GridPosition position) {
        return distances.getOrDefault(position, Integer.MAX_VALUE);
    }

    List<GridPosition> reachableTiles() {
        return new ArrayList<>(distances.keySet());
    }

    int maxDistance() {
        return maxDistance;
    }

    private record PreviewNode(GridPosition position, int distance) {
    }
}
