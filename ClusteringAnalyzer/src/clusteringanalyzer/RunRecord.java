/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clusteringanalyzer;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class RunRecord
    {

        public RunRecord(DataSet dataset, int run) {
            this.dataset = dataset;
            this.run = run;
            this.header = "<div style=\"text-align: center; color:white; background-color:#336600; border: solid black;\">" 
                + dataset.file.getName().toUpperCase() + " using " + dataset.initMethod.name().toUpperCase() + " : " 
                    + dataset.clusteringMethod.name().toUpperCase() + ", Run : " + run + "</div>";
            init();
        }
        
        private void init()
        {
            sseArray = new ArrayList<>();
            startingCentroids = new Point[dataset.numClusters];
            for (Point point: startingCentroids)
            {
                point = new Point();
            }
            finishingCentroids = new Point[dataset.numClusters];
        }
        
        public void grabCentroids(Point[] centroidArray)
        {
            for (int cluster = 0; cluster < dataset.numClusters; cluster++)
            {
                centroidArray[cluster] = dataset.clusters[cluster].centroid;
            }
        }
        
        public void setStats()
        {
            itersToConverge = this.finalIteration - dataset.maxIterWithoutChange;
            Arrays.sort(sseArray.toArray());
            minimumSSE = sseArray.get(sseArray.size() - 1);
            maximumSSE = sseArray.get(0);
            int mid_num = sseArray.size() / 2;
        
            if ((sseArray.size() % 2) == 0) {
                medianSSE = ((sseArray.get(mid_num)) + sseArray.get(mid_num - 1)) / 2;
            } else {
                medianSSE = sseArray.get(mid_num);
            }
            setAverageSSE();
            setCentMovement();
            
        }
        
        public void setAverageSSE()
        {
            
            double sum = 0;
            for (double sse:sseArray)
            {
                sum += sse;
            }
            averageSSE = sum / sseArray.size();
        }
        
        public void setFluctuatingInstance(DataSet ds)
        {
            Object [] fluctData = {ds.currentIteration, ds.currentSSE, ds.lastSSE, Math.abs(ds.currentSSE - ds.lastSSE)};
            fluctuatingInstances.add(fluctData);
        }
        
        public void setCentMovement()
        {
            centroidMovement = 0;
            for (int centroid = 0; centroid < finishingCentroids.length; centroid++)
            {
                centroidMovement += ClusteringOps.getAbsDistance(startingCentroids[centroid], finishingCentroids[centroid]);
            }
        }
        
        public String pointArrayToHTML(Point[] pointArray)
        {
            String pointString = "<ol>";
            for (Point point: pointArray)
            {
                pointString += "<li>" + point.toString() + "</li>";
            }
            pointString += "</ol>";
            return pointString;
        }
        
        public String htmlCentroidInfo()
        {
        
            return (header + "<div>" + "<h3 style=\"color:red;\">Starting Centroids</h3>" + pointArrayToHTML(startingCentroids) + 
                    "<h3 style=\"color:blue;\">Finishing Centroids</h3>"  + pointArrayToHTML(finishingCentroids) + 
                    "<p style=\"color:green\">Total Centroid Movement : " + centroidMovement + "</p>" + "</div>");
        }
        
        public String htmlSSEInfo()
        {
            return (header + "<div><ul>" + "<li>Initial SSE : " + initialSSE + "</li>"
                    + "<li>Final SSE : " + finalSSE + "</li>"
                    + "<li>Maximum SSE : " + maximumSSE + "</li>"
                    + "<li>Minimum SSE : " + minimumSSE + "</li>" 
                    + "<li>SSE Change : " + (maximumSSE - minimumSSE) + "</li>" 
                    +  "</ul></div>");
        }
        
        public String htmlRunData()
        {
        
            return (header + "<div><ul>" + "<li>Time Taken : " + millisToSec(timeTaken) + " seconds </li>" 
                    + "<li>Total Iterations : " + this.finalIteration + "</li>" 
                    + "<li>Converged at Iteration : " + (itersToConverge) + "</li>" + "</ul></div>");
        }
        
        public double millisToSec(int millis)
        {
            return (double)millis/1000;
        }
        
        int run, finalIteration, averageIterations, timeTaken, itersToConverge, numFluctuated = 0;
        ArrayList<Object> fluctuatingInstances = new ArrayList<>();
        double initialSSE, finalSSE, averageSSE, medianSSE, minimumSSE, maximumSSE, centroidMovement;
        boolean fluctuated;
        ArrayList<Double> sseArray;
        Point[] startingCentroids, finishingCentroids;
        DataSet dataset;
        String header;
    }