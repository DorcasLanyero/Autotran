package com.cassens.autotran.backendpoc;

public class PoCLoadStats {
    public int total = 0;
    public int completed = 0;
    public int inTransit = 0;
    public int pending = 0;
    public int hidden = -1;
    public String oldest = ""; // change to oldestCompletedLoad
    public String statusMsg = "";
}
