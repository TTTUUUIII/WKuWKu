package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PoissonDiskSampler {
    private final double mWidth, mHeight;
    private final double mMinDist;
    private final int mMaxAttempts;
    private final double mCellSize;
    private final int mGridWidth, mGridHeight;
    private final List<Point> mPoints;
    private final List<Point> mActiveList;
    private final Point[][] mGrid;

    public static class Point {
        public final double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public PoissonDiskSampler(double width, double height, double minDist, int maxAttempts) {
        this.mWidth = width;
        this.mHeight = height;
        this.mMinDist = minDist;
        this.mMaxAttempts = maxAttempts;
        this.mCellSize = minDist / Math.sqrt(2);
        this.mGridWidth = (int)(width / mCellSize) + 1;
        this.mGridHeight = (int)(height / mCellSize) + 1;
        this.mGrid = new Point[mGridWidth][mGridHeight];
        this.mPoints = new ArrayList<>();
        this.mActiveList = new ArrayList<>();
    }

    public List<Point> generatePoints() {
        Point firstPoint = new Point(
                Math.random() * mWidth,
                Math.random() * mHeight
        );
        addPoint(firstPoint);

        while (!mActiveList.isEmpty()) {
            int randomIndex = (int)(Math.random() * mActiveList.size());
            Point point = mActiveList.get(randomIndex);
            boolean found = false;

            for (int i = 0; i < mMaxAttempts; i++) {
                Point newPoint = generateRandomPointAround(point);
                if (isValid(newPoint)) {
                    addPoint(newPoint);
                    found = true;
                    break;
                }
            }

            if (!found) {
                mActiveList.remove(randomIndex);
            }
        }

        return mPoints;
    }

    private Point generateRandomPointAround(Point point) {
        double radius = mMinDist * (1 + Math.random());
        double angle = 2 * Math.PI * Math.random();
        double newX = point.x + radius * Math.cos(angle);
        double newY = point.y + radius * Math.sin(angle);
        return new Point(newX, newY);
    }

    private boolean isValid(Point point) {
        if (point.x < 0 || point.x >= mWidth || point.y < 0 || point.y >= mHeight) {
            return false;
        }

        int cellX = (int)(point.x / mCellSize);
        int cellY = (int)(point.y / mCellSize);

        int searchStartX = Math.max(0, cellX - 2);
        int searchEndX = Math.min(mGridWidth - 1, cellX + 2);
        int searchStartY = Math.max(0, cellY - 2);
        int searchEndY = Math.min(mGridHeight - 1, cellY + 2);

        for (int x = searchStartX; x <= searchEndX; x++) {
            for (int y = searchStartY; y <= searchEndY; y++) {
                Point gridPoint = mGrid[x][y];
                if (gridPoint != null) {
                    double distance = Math.sqrt(
                            Math.pow(point.x - gridPoint.x, 2) +
                                    Math.pow(point.y - gridPoint.y, 2)
                    );
                    if (distance < mMinDist) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void addPoint(@NonNull Point point) {
        mPoints.add(point);
        mActiveList.add(point);
        int cellX = (int)(point.x / mCellSize);
        int cellY = (int)(point.y / mCellSize);
        mGrid[cellX][cellY] = point;
    }
}
