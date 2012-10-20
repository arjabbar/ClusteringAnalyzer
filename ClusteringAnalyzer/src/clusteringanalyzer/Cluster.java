/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clusteringanalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class Cluster{

    public Cluster(Point centroid)
    {
        points = new ArrayList<>(20000);
        this.centroid = centroid;
    }
    
    public Cluster()
    {
        points = new ArrayList<>(20000);
    }
    
    public void addPoint(Point point)
    {
        boolean added = points.add(point);
        if (centroid!=null)
        {
            point.attachedCentroid = centroid;
        }
        if(added)
        {
            status = "Point added";
        }
    }
    
    public void addPointArray(Point[] pointArray)
    {
        points.addAll(Arrays.asList(pointArray));
        status = "Added " + pointArray.length + " points from an Array";
    }
    
    public void clearPoints()
    {
        points.clear();
        if (points.isEmpty())
        {
            status = "Cluster cleared!";
            points = new ArrayList<>(10000);
        }
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
        for (int point = 0; point < points.size(); point++)
        {
            points.get(point).attachedCentroid = this.centroid;
        }
    }
    

    public String getStatus() {
        return status;
    }
    
    public int getSize()
    {
        return points.size();
    }

    public Point getCenterOfGravity() {
        double[] cogPoint = new double[points.get(0).attributes.length];
        for (int attr = 0; attr < cogPoint.length; attr++)
        {
            double sum = 0;
            for (int point = 0; point < points.size(); point++)
            {
                sum += points.get(point).attributes[attr];
            }
            cogPoint[attr] = sum / points.size();
        }
        centerOfGravity = new Point(cogPoint);
        return centerOfGravity;
    }
    
    public void addPointResetCentroid(Point point)
    {
        if (centroid==null) {
            return;
        }
        double[] pointAttr = new double[centroid.numAttributes];
        double sum = 0;
        for (int attr = 0; attr < pointAttr.length; attr++)
        {
            pointAttr[attr] = ((centroid.attributes[attr] * points.size()) + point.attributes[attr])/(points.size() + 1);
        }
        addPoint(point);
        setCentroid(new Point(pointAttr));
    }
    
    public void subPointResetCentroid(Point point)
    {
        if (centroid==null) {
            return;
        }
        double[] pointAttr = new double[centroid.numAttributes];
        double sum = 0;
        for (int attr = 0; attr < pointAttr.length; attr++)
        {
            pointAttr[attr] = ((centroid.attributes[attr] * points.size()) - point.attributes[attr])/(points.size() - 1);
        }
        setCentroid(new Point(pointAttr));
        boolean removePoint = removePoint(point);
        if (!removePoint)
        {
            System.err.println("Point not found! Remove Failed!");
        }
    }
    
    public void subPointResetCentroid(int point)
    {
        if (centroid==null) {
            return;
        }
        double[] pointAttr = new double[centroid.numAttributes];
        double sum = 0;
        for (int attr = 0; attr < pointAttr.length; attr++)
        {
            pointAttr[attr] = ((centroid.attributes[attr] * points.size()) - points.get(point).attributes[attr])/(points.size() - 1);
        }
        points.remove(point);
        setCentroid(new Point(pointAttr));
        
    }

    public boolean removePoint(Point point)
    {
        for (int p = 0; p < points.size(); p++)
        {
            if (point.toString().equals(points.get(p).toString()))
            {
                points.remove(p);
                return true;
            }
        }
        return false;
    }
    
    public double getCurrentSSE() {
        currentSSE = 0;
        for (int point = 0; point < points.size(); point++)
        {
            currentSSE += ClusteringOps.getAbsDistance(points.get(point), points.get(point).attachedCentroid);
        }
        return currentSSE;
    }

    @Override
    public String toString() {
        String clusterString = "Cluster \n";
        for (int point = 0; point < points.size(); point++)
        {
            clusterString += points.get(point).toString() + "\n";
        }
        return clusterString;
    }
    
    public Point centroid, centerOfGravity;
    public List<Point> points;
    public String status = "";
    public double currentSSE;
}
