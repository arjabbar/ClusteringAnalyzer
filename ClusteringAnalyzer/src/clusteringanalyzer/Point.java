/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clusteringanalyzer;

import java.util.Arrays;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class Point {
    
    public Point(double[] attributes)
    {
        this.attributes = attributes;
        numAttributes = attributes.length;
    }

    public Point() {
    }
    
    public double distanceFromItsCentroid()
    {
        return ClusteringOps.getAbsDistance(this, this.attachedCentroid);
    }
    
    public double getDistanceFrom(Point point)
    {
        return ClusteringOps.getAbsDistance(this, point);
    }
    
    public Point getFarthestPoint(Point[] pointList)
    {
        double longestDist = 0;
        int farthestPoint = 0;
        for (int point = 0; point < pointList.length; point++)
        {
            double dist = ClusteringOps.getAbsDistance(this, pointList[point]);
            if (dist > longestDist)
            {
                longestDist = dist;
                farthestPoint = point;
            }
        }
        return pointList[farthestPoint];
    }
    
    public boolean isCentroid(DataSet dataset)
    {
        for (int centroid = 0; centroid < dataset.numClusters; centroid++)
        {
            if (dataset.clusters[centroid]!=null)
            {
            if (this.attributes==dataset.clusters[centroid].centroid.attributes)
            {
                return true;
            }
            }
        }
        return false;
    }

    public void setCluster(int cluster) {
        if (this.cluster == cluster)
        {
            amountReassigned++;
        } else {
            this.cluster = cluster;
        }
        
    }

    public void print()
    {
        System.out.println(toString());
    }
        
    @Override
    public String toString() {
        return Arrays.toString(attributes);
    }

    public double[] attributes;
    public boolean isLoner;
    public int cluster, numAttributes, amountReassigned;
    public Point attachedCentroid;
}
