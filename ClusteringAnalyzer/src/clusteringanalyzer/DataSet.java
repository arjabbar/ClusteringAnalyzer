/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clusteringanalyzer;

import java.io.*;
import javax.swing.Timer;
import org.hsqldb.util.CSVWriter;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class DataSet {
    
    public Method method;
    public InitMethod initMethod;
    public ClusteringMethod clusteringMethod;
    public Point[] points, bestCentroids, initialCentroids;
    public Cluster[] clusters;
    public int numPoints, numAttributes, numClusters, currentRun, currentIteration, 
            maxIterations, maxRuns, runs, iterations, itersWithoutChange, fluctuated = 0, 
            maxIterWithoutChange = 0, numSubsets = 10;
    public boolean converged = false, fluctuating = false, lostCentroid = false, 
            verboseIterations = true, verboseRuns = false, verboseCentroids = true;
    public String status = "Created";
    public String header;
    public String csvColumnHeader = "";
    public double currentSSE, lastSSE, highSSE, stDevSSE, avgSSE, minimumSSE,
            maximumSSE, medianSSE, avgIters, minimumIters, maximumIters,
            medianIters, stDevIters, avgTimeTaken, minimumTimeTaken,
            maximumTimeTaken, medianTimeTaken, stDevTimeTaken,
            lowSSE = Double.MAX_VALUE, tolerance = 1e-6, scale = 1.5, startingScale;;
    public double[] runData;
    double[] finalSSEs;
    double[] finalIters;
    double[] finalTimes;
    public RunRecord[] runRecords;
    public File file;
    public Timer timer;
    public int timeTaken;
    
    public DataSet(File file) throws FileNotFoundException, IOException
    {
        status = "Reading File";
        String input;
        int slot = 0;
        this.file = file;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String[] fileTraits = br.readLine().split(" ");
        this.numPoints = Integer.valueOf(fileTraits[0]);
        this.numAttributes = Integer.valueOf(fileTraits[1]);
        this.points = new Point[numPoints];

        while((input = br.readLine()) != null)
        {
            String[] attributes = input.split(" ");
            double[] stringsToDoubles = new double[attributes.length];
            for (int attr = 0; attr < attributes.length; attr++)
            {
                if (attributes[attr]!=null) {
                    stringsToDoubles[attr] = Double.valueOf(attributes[attr]);
                }
            }
            points[slot] = new Point(stringsToDoubles);
            slot++;
        }
        fr.close();
        br.close();
    }

    public DataSet() {
    }

    public void runClusteringAlgorithm(InitMethod initMethod, ClusteringMethod clusteringMethod, int clusters, int maxIterations, int maxRuns, int numSubsets, double scale) throws FileNotFoundException, IOException
    {
        this.header = ("<div style=\"text-align: center; color:black; background-color:lightgray; border: solid red;\">" 
                + "Final Stats for " + this.file.getName() + " using " + initMethod + " initialization and " + clusteringMethod + " clustering method" + "</div>");
        this.status = "Running Algorithm using the " + initMethod + " initializing and " + clusteringMethod + " clustering method, " + clusters + " clusters " + maxRuns + " times";
        this.initMethod = initMethod;
        this.clusteringMethod = clusteringMethod;
        this.numSubsets = numSubsets;
        this.scale = scale;
        
        startingScale = this.scale;
        
        this.numClusters = clusters;
        this.maxIterations = maxIterations;
        if (initMethod==InitMethod.MAXIMIN)
        {
            maxRuns = 1;
        }
        this.maxRuns = maxRuns;
        this.clusters = new Cluster[numClusters];
        this.initialCentroids = new Point[this.numClusters];
        this.runRecords = new RunRecord[maxRuns];
        this.finalIters = new double[maxRuns];
        this.finalSSEs = new double[maxRuns];
        this.finalTimes = new double[maxRuns];
        long startTime = System.currentTimeMillis();
        while (runs < this.maxRuns) {
            
            this.runRecords[this.runs] = new RunRecord(this, runs + 1);
            long runTime = System.currentTimeMillis();
            this.runData = new double[maxIterations];
            initCentroids();
            runs++;
            ClusteringOps.initialize(this);
            while ((iterations < maxIterations) && !converged) {
                iterate();
                ClusteringOps.assignPointsToClusters(this);
                ClusteringOps.computeSSE(this);
                ClusteringOps.recomputeCentroids(this);
            }
            recordRun(runTime);
        }
        setRunStats();
        status = "Done Running";
        this.timeTaken = (int)(System.currentTimeMillis() - startTime);
    }
    
    //This method is only used if all the dependent variables are set before the data set is ran.
    public void runClusteringAlgorithm() throws FileNotFoundException, IOException
    {
        this.header = ("<div style=\"text-align: center; color:black; background-color:lightgray; border: solid red;\">" 
                + "Final Stats for " + this.file.getName() + " using " + initMethod + " initialization and " 
                + clusteringMethod + " clustering method, Scale=" + this.scale + ", Runs=" + maxRuns + "</div>");
        this.status = "Running Algorithm using the " + initMethod + " initializing and " + clusteringMethod + " clustering method, " + clusters + " clusters " + maxRuns + " times";
        
        startingScale = this.scale;
        
        if (initMethod==InitMethod.MAXIMIN)
        {
            maxRuns = 1;
        }
        
        this.clusters = new Cluster[numClusters];
        this.initialCentroids = new Point[this.numClusters];
        this.runRecords = new RunRecord[maxRuns];
        this.finalIters = new double[maxRuns];
        this.finalSSEs = new double[maxRuns];
        this.finalTimes = new double[maxRuns];
        long startTime = System.currentTimeMillis();
        while (runs < this.maxRuns) {
            
            this.runRecords[this.runs] = new RunRecord(this, runs + 1);
            long runTime = System.currentTimeMillis();
            this.runData = new double[maxIterations];
            initCentroids();
            runs++;
            ClusteringOps.initialize(this);
            while ((iterations < maxIterations) && !converged) {
                iterate();
                ClusteringOps.assignPointsToClusters(this);
                ClusteringOps.computeSSE(this);
                ClusteringOps.recomputeCentroids(this);
            }
            recordRun(runTime);
        }
        setRunStats();
        status = "Done Running";
        this.timeTaken = (int)(System.currentTimeMillis() - startTime);
    }

    public void recordRun(long runTime) {
        runRecords[this.runs - 1].timeTaken = (int)(System.currentTimeMillis() - runTime);
        runRecords[this.runs - 1].grabCentroids(runRecords[this.runs - 1].finishingCentroids);
        runRecords[this.runs - 1].finalIteration = this.iterations;
        runRecords[this.runs - 1].finalSSE = this.currentSSE;
        runRecords[this.runs - 1].setStats();
    }
    
    public void iterate()
    {
        this.iterations++;
        this.currentIteration = iterations;
    }
    public void initRandomCentroids()
    {
        for (int clusterNum = 0; clusterNum < numClusters; clusterNum++)
        {
            this.clusters[clusterNum] = new Cluster(points[ClusteringOps.r.nextInt(points.length)]);
            this.initialCentroids[clusterNum] = this.clusters[clusterNum].centroid;
        }
    }
    
    public void initCentroids()
    {
        for (int clusterNum = 0; clusterNum < numClusters; clusterNum++)
        {
            this.clusters[clusterNum] = new Cluster(new Point(new double[numAttributes]));
            this.initialCentroids[clusterNum] = this.clusters[clusterNum].centroid;
        }
    }
    
    @Override
    public String toString() {
        return "{File: " + file.getName() + ", Points: " + numPoints + ", Attributes: " + numAttributes + "}";
    }
    
    public String dataToString()
    {
        String dataString = "";
        for (int point = 0; point < numPoints; point++)
        {
            dataString += points[point].toString() + "\n";
        }
        return dataString;
    }
    
    public void setRunStats()
    {
        

        int run = 0;
        for (RunRecord rr: runRecords)
        {
            if (rr!=null) {
                finalSSEs[run] = rr.finalSSE;
                finalIters[run] = rr.itersToConverge;
                finalTimes[run] = rr.timeTaken;
                run++;
            } 
        }
        avgIters = ClusteringOps.getAvg(finalIters);
        stDevIters = ClusteringOps.getStDev(finalIters);
        minimumIters = ClusteringOps.getMinimum(finalIters);
        maximumIters = ClusteringOps.getMaximum(finalIters);
        medianIters = ClusteringOps.getMedian(finalIters);
        
        avgSSE = ClusteringOps.getAvg(finalSSEs);
        stDevSSE = ClusteringOps.getStDev(finalSSEs);
        minimumSSE = ClusteringOps.getMinimum(finalSSEs);
        maximumSSE = ClusteringOps.getMaximum(finalSSEs);
        medianSSE = ClusteringOps.getMedian(finalSSEs);
        
        avgTimeTaken = ClusteringOps.getAvg(finalTimes) / 1000;
        stDevTimeTaken = ClusteringOps.getStDev(finalTimes) / 1000;
        minimumTimeTaken = ClusteringOps.getMinimum(finalTimes) / 1000;
        maximumTimeTaken = ClusteringOps.getMaximum(finalTimes) / 1000;
        medianTimeTaken = ClusteringOps.getMedian(finalTimes) / 1000;
        
        
    }
    
    public String htmlSSEStats()
    {
        return (header + "<div><ul>" 
                + "<li>Minimum SSE: " + minimumSSE + "</li>" 
                + "<li>Maximum SSE: " + maximumSSE + "</li>" 
                + "<li>Median SSE: " + medianSSE + "</li>" 
                + "<li>Average SSE: " + avgSSE + "</li>" 
                + "<li>Standard Deviation of SSE: " + stDevSSE + "</li>" 
                + "<ul></div>");
    }
    
    public String htmlIterStats()
    {
        return (header + "<div><ul>" 
                + "<li>Minimum Iterations: " + minimumIters + "</li>" 
                + "<li>Maximum Iterations: " + maximumIters + "</li>" 
                + "<li>Median Iterations: " + medianIters + "</li>" 
                + "<li>Average Iterations: " + avgIters + "</li>" 
                + "<li>Standard Deviation of Iterations: " + stDevIters + "</li>" 
                + "<ul></div>");
    }

    public String htmlTimeStats()
    {
        return (header + "<div><ul>" 
                + "<li>Minimum Time: " + minimumTimeTaken + "</li>" 
                + "<li>Maximum Time: " + maximumTimeTaken + "</li>" 
                + "<li>Median Time: " + medianTimeTaken + "</li>" 
                + "<li>Average Time: " + avgTimeTaken + "</li>" 
                + "<li>Standard Deviation of Times: " + stDevTimeTaken + "</li>" 
                + "<ul></div>");
    }
    
    public String csvOverallStats()
    {
        return (initMethod + "," + clusteringMethod + "," + maxRuns + "," + maxIterations + "," + avgSSE + "," + avgTimeTaken + "," + avgIters + "\n");
    }
    
    public String csvStats()
    {
        String csvOut = "";
        for (RunRecord rr : runRecords)
        {
            if (rr!=null)
            {
                csvOut += file.toString().toUpperCase() + "," + initMethod + "," + clusteringMethod + "," + maxRuns + "," + rr.run 
                        + "," + maxIterations + "," + rr.finalIteration + "," + rr.initialSSE + "," + rr.finalSSE + "," + (rr.initialSSE - rr.finalSSE) + "\n";
            }
        }
        return csvOut;
    }
    
    public static String csvHeader()
    {
        return ("File,\"Initialization Method\",\"Clustering Method\",\"Max Runs\",Run,\"Max Iterations\",\"Final Iteration\",\"Initial SSE\",\"Final SSE\",\"Change in SSE\"" + "\n");
    }
}
