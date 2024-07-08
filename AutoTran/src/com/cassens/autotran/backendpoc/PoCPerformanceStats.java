package com.cassens.autotran.backendpoc;

public class PoCPerformanceStats {
    public double dispatchAvgTime;
    public double dispatchMaxTime;
    public double preloadAvgQueryTime;
    public double preloadMaxQueryTime;
    public double deliveryAvgQueryTime;
    public double deliveryMaxQueryTime;
    public double tabletStatsQueryTime;
    public double pruneLoadsQueryTime;
    public static PoCPerformanceStats sPerfStats;

    private PoCPerformanceStats() {};

    public static PoCPerformanceStats getPerformanceStats() {
        if (sPerfStats == null) {
            sPerfStats = new PoCPerformanceStats();
        }
        return sPerfStats;
    }

    public static void recordDispatchQueryTime(double elapsedTime) {
        PoCPerformanceStats perfStats = getPerformanceStats();
        if (elapsedTime > perfStats.dispatchMaxTime) {
            perfStats.dispatchMaxTime = elapsedTime;
        }
    }

    public static void recordPreloadQueryTime(double elapsedTime) {
        PoCPerformanceStats perfStats = getPerformanceStats();
        if (elapsedTime > perfStats.preloadMaxQueryTime) {
            perfStats.preloadMaxQueryTime = elapsedTime;
        }
    }

    public static void recordDeliveryQueryTime(double elapsedTime) {
        PoCPerformanceStats perfStats = getPerformanceStats();
        if (elapsedTime > perfStats.deliveryMaxQueryTime) {
            perfStats.deliveryMaxQueryTime = elapsedTime;
        }
    }

    public static void recordPruneLoadsQueryTime(double elapsedTime) {
        PoCPerformanceStats perfStats = getPerformanceStats();
        if (elapsedTime > perfStats.pruneLoadsQueryTime) {
            perfStats.pruneLoadsQueryTime = elapsedTime;
        }
    }
}
