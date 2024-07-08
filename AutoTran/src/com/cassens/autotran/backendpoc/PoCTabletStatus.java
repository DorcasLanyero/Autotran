package com.cassens.autotran.backendpoc;

public class PoCTabletStatus {
    public String tabletId;
    public String userId;
    public boolean needsAttention = false;
    public String message;
    public int numUsers;  // Change name to driverCount
    public String updateTime;
    public Double loadStatsQueryTime;
    public PoCLoadStats loadStats = new PoCLoadStats();
    public PoCPerformanceStats performanceStats = PoCPerformanceStats.getPerformanceStats();

    @Override
    public String toString() {
        return String.format(
                "       Driver #: %s\n" +
                "Tablet serial #: %s\n\n" +
                "Load Statistics\n" +
                "       total: %d\n" +
                "  in transit: %d\n" +
                "     pending: %d\n" +
                "   completed: %d\n" +
                "      oldest: %s\n" +
                "\nPerformance Statistics\n" +
                "Max preload query: %.2f secs\n" +
                "  Max deliv query: %.2f secs\n" +
                "  Max stats query: %.2f secs\n", userId, tabletId,
                loadStats.total, loadStats.inTransit, loadStats.pending,
                loadStats.completed, loadStats.oldest,
                performanceStats.preloadMaxQueryTime,
                performanceStats.deliveryMaxQueryTime,
                performanceStats.tabletStatsQueryTime);
    }
}
