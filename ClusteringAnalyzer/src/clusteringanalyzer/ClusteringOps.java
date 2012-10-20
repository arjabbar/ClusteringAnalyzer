/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clusteringanalyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class ClusteringOps {

    public ClusteringOps() {
    }

    public static void initialize(DataSet dataset) throws FileNotFoundException, IOException {

        dataset.status = "Initializing";
        dataset.converged = false;
        dataset.iterations = 0;
        dataset.currentIteration = 1;
        dataset.currentSSE = 0;
        switch (dataset.initMethod) {
            case MACQUEEN: {
                dataset.initRandomCentroids();
                assignPointsUsingKMeans(dataset);
                dataset.runRecords[dataset.runs - 1].grabCentroids(dataset.runRecords[dataset.runs - 1].startingCentroids);
            }
            break;

            case MAXIMIN: {
                dataset.maxRuns = 1;
                dataset.clusters[0] = new Cluster(pointAverage(dataset.points));
                dataset.clusters[1] = new Cluster(dataset.clusters[0].centroid.getFarthestPoint(dataset.points));
                for (int clusterNum = 2; clusterNum < dataset.numClusters; clusterNum++) {
                    dataset.clusters[clusterNum] = new Cluster(greatestMinDistancePoint(dataset, clusterNum - 1));
                    dataset.initialCentroids[clusterNum] = dataset.clusters[clusterNum].centroid;
                }
                assignPointsUsingKMeans(dataset);
                dataset.runRecords[dataset.runs - 1].grabCentroids(dataset.runRecords[dataset.runs - 1].startingCentroids);
            }
            break;

            case FORGY: {
                for (Point point : dataset.points) {
                    int rand = r.nextInt(dataset.numClusters);
                    point.setCluster(rand);
                    dataset.clusters[rand].addPoint(point);
                }
                for (Cluster cluster : dataset.clusters) {
                    cluster.setCentroid(cluster.getCenterOfGravity());
                }
                dataset.runRecords[dataset.runs - 1].grabCentroids(dataset.runRecords[dataset.runs - 1].startingCentroids);
            }
            break;

            case BF: {
                dataset.bestCentroids = new Point[dataset.numClusters];
                dataset.lowSSE = Double.MAX_VALUE;
                DataSet[] subsets = new DataSet[dataset.numSubsets];
                for (int subsetNum = 0; subsetNum < dataset.numSubsets; subsetNum++) {
                    subsets[subsetNum] = new DataSet(createSubsetFile(dataset, dataset.numPoints / dataset.numSubsets));
                    subsets[subsetNum].runClusteringAlgorithm(InitMethod.MACQUEEN, ClusteringMethod.KMEANS, dataset.numClusters, 10, 1, dataset.numSubsets, 1);
                    if (subsets[subsetNum].currentSSE < dataset.lowSSE) {
                        dataset.lowSSE = subsets[subsetNum].currentSSE;
                        for (int centroidNum = 0; centroidNum < dataset.numClusters; centroidNum++) {
                            dataset.bestCentroids[centroidNum] = new Point(subsets[subsetNum].initialCentroids[centroidNum].attributes);
                        }
                    }
                }
                dataset.initialCentroids = dataset.bestCentroids;
                genClustersFromCentroids(dataset, dataset.initialCentroids);
                assignPointsUsingKMeans(dataset);
                dataset.runRecords[dataset.runs - 1].grabCentroids(dataset.runRecords[dataset.runs - 1].startingCentroids);
                dataset.lowSSE = Double.MAX_VALUE;
            }
            return;
        }
        if (dataset.verboseCentroids) {
            printCentroids(dataset);
        }
    }

    public static void assignPointsToClusters(DataSet dataset) {
        if (dataset.converged) {
            return;
        }
        dataset.status = "Assigning points to a centroid";
        switch (dataset.clusteringMethod) {
            case PR:
            case JANCEY:
            case LEE:
            case KMEANS: {
                for (int point = 0; point < dataset.numPoints; point++) {
                    Point thisPoint = dataset.points[point];
                    double distance = 0;
                    double minDistance = Double.MAX_VALUE;
                    for (int clusterNum = 0; clusterNum < dataset.numClusters; clusterNum++) {
                        distance = getAbsDistance(thisPoint, dataset.clusters[clusterNum].centroid);
                        if (distance < minDistance) {
                            minDistance = distance;
                            thisPoint.cluster = clusterNum;
                            thisPoint.attachedCentroid = dataset.clusters[clusterNum].centroid;
                        }
                    }
                }
                addPointsToCluster(dataset);
            }
            break;

            case DHB: {
                for (int oldClusterNum = 0; oldClusterNum < dataset.numClusters; oldClusterNum++) {
                    if (dataset.clusters[oldClusterNum].getSize() != 1) {
                        for (int point = 0; point < dataset.clusters[oldClusterNum].getSize(); point++) {
                            int bestCluster = oldClusterNum;
                            double thisDist = dataset.clusters[oldClusterNum].points.get(point).distanceFromItsCentroid();
                            double oldCluster = (dataset.clusters[oldClusterNum].getSize() * thisDist) / (dataset.clusters[oldClusterNum].getSize() - 1);
                            double newBestCluster = Double.MAX_VALUE;

                            for (int newClusterNum = 0; newClusterNum < dataset.numClusters; newClusterNum++) {
                                if (newClusterNum != oldClusterNum) {
                                    thisDist = dataset.clusters[oldClusterNum].points.get(point).getDistanceFrom(dataset.clusters[newClusterNum].centroid);
                                    double newCluster = (dataset.clusters[newClusterNum].getSize() * thisDist) / (dataset.clusters[newClusterNum].getSize() + 1);
                                    if (newCluster < newBestCluster) {
                                        newBestCluster = newCluster;
                                        bestCluster = newClusterNum;
                                    }
                                }
                            }
                            if ((bestCluster != oldClusterNum) && (newBestCluster < oldCluster)) {
                                dataset.clusters[bestCluster].addPointResetCentroid(dataset.clusters[oldClusterNum].points.get(point));
                                dataset.clusters[oldClusterNum].subPointResetCentroid(point);

                            }
                        }
                    }
                }
            }
            break;

            case DHF: {
                for (int oldClusterNum = 0; oldClusterNum < dataset.numClusters; oldClusterNum++) {
                    if (dataset.clusters[oldClusterNum].getSize() != 1) {
                        for (int point = 0; point < dataset.clusters[oldClusterNum].getSize(); point++) {
                            int bestCluster = oldClusterNum;
                            double thisDist = dataset.clusters[oldClusterNum].points.get(point).distanceFromItsCentroid();
                            double oldCluster = (dataset.clusters[oldClusterNum].getSize() * thisDist) / (dataset.clusters[oldClusterNum].getSize() - 1);
                            {
                                for (int newClusterNum = 0; newClusterNum < dataset.numClusters; newClusterNum++) {
                                    if (newClusterNum != oldClusterNum) {
                                        thisDist = dataset.clusters[oldClusterNum].points.get(point).getDistanceFrom(dataset.clusters[newClusterNum].centroid);
                                        double newCluster = (dataset.clusters[newClusterNum].getSize() * thisDist) / (dataset.clusters[newClusterNum].getSize() + 1);
                                        if (newCluster < oldCluster) {
                                            bestCluster = newClusterNum;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (bestCluster != oldClusterNum) {
                                dataset.clusters[bestCluster].addPointResetCentroid(dataset.clusters[oldClusterNum].points.get(point));
                                dataset.clusters[oldClusterNum].subPointResetCentroid(point);
                            }
                        }
                    }
                }
            }
            break;

            case ABF: {
                if ((dataset.currentIteration % 2) == 0) {
                    dataset.clusteringMethod = ClusteringMethod.DHF;
                } else {
                    dataset.clusteringMethod = ClusteringMethod.DHB;
                }
                assignPointsToClusters(dataset);
                dataset.clusteringMethod = ClusteringMethod.ABF;
            }
            break;

            case AFB: {
                if ((dataset.currentIteration % 2) == 0) {
                    dataset.clusteringMethod = ClusteringMethod.DHB;
                } else {
                    dataset.clusteringMethod = ClusteringMethod.DHF;
                }
                assignPointsToClusters(dataset);
                dataset.clusteringMethod = ClusteringMethod.AFB;
            }
            break;
        }
        dataset.status = "Points Assigned";
    }

    public static void recomputeCentroids(DataSet dataset) {
        if (dataset.converged) {
            return;
        }
        dataset.status = "Recomputing Centroids";
        switch (dataset.clusteringMethod) {

            case ABF:
            case AFB:
            case DHB:
            case DHF:
                for (int clusterNum = 0; clusterNum < dataset.numClusters; clusterNum++) {
                    try {
                        Point centerOfGravity = dataset.clusters[clusterNum].getCenterOfGravity();
                        dataset.clusters[clusterNum].setCentroid(centerOfGravity);
                    } catch (Exception e) {
                        System.err.println("Cluster " + clusterNum + " empty.");
                        dataset.lostCentroid = true;
                    }
                }

                if (dataset.verboseCentroids) {
                    printCentroids(dataset);
                }
                return;
            case JANCEY:
                dataset.scale = 2;
                break;
            case PR:
                dataset.scale = 1 + 1.0 / (1 + dataset.currentIteration);
                break;
            case KMEANS:
                dataset.scale = 1;
                break;
            case LEE:
                break;

        }
        for (int clusterNum = 0; clusterNum < dataset.numClusters; clusterNum++) {
            if (dataset.clusters[clusterNum].getSize() > 0) {
                Point differenceInPoints = getDifference(dataset.clusters[clusterNum].getCenterOfGravity(), dataset.clusters[clusterNum].centroid);
                Point scaledDiff = scalePoint(differenceInPoints, dataset.scale);
                Point newCentroid = addPoints(dataset.clusters[clusterNum].centroid, scaledDiff);
                dataset.clusters[clusterNum].setCentroid(newCentroid);
            }
        }
        dataset.status = "Recomputed Centroids";
        if (dataset.verboseCentroids) {
            printCentroids(dataset);
        }
    }

    public static void recomputeCentroid(DataSet dataset, int cluster) {

        if (dataset.converged) {
            return;
        }
        dataset.status = "Recomputing Centroids";
        switch (dataset.clusteringMethod) {

            case ABF:
            case AFB:
            case DHB:
            case DHF:
                try {
                    Point centerOfGravity = dataset.clusters[cluster].getCenterOfGravity();
                    dataset.clusters[cluster].setCentroid(centerOfGravity);
                } catch (Exception e) {
                    System.err.println("Cluster " + cluster + " empty.");
                    dataset.lostCentroid = true;
                }

                if (dataset.verboseCentroids) {
                    printCentroids(dataset);
                }

                return;

            case JANCEY:
                dataset.scale = 2;
                break;
            case PR:
                dataset.scale = 1 + 1.0 / (1 + dataset.currentIteration);
                break;
            case KMEANS:
                dataset.scale = 1;
                break;
            case LEE:
                break;
        }
        Point differenceInPoints = getDifference(dataset.clusters[cluster].getCenterOfGravity(), dataset.clusters[cluster].centroid);
        Point scaledDiff = scalePoint(differenceInPoints, dataset.scale);
        Point newCentroid = addPoints(dataset.clusters[cluster].centroid, scaledDiff);
        dataset.clusters[cluster].setCentroid(newCentroid);
        dataset.status = "Recomputed Centroids";

        if (dataset.verboseCentroids) {
            printCentroids(dataset);
        }
    }

    public static void computeSSE(DataSet dataset) {
        dataset.status = "Computing SSE";
        dataset.lastSSE = dataset.currentSSE;
        dataset.currentSSE = 0;
        for (int cluster = 0; cluster < dataset.numClusters; cluster++) {
            dataset.currentSSE += dataset.clusters[cluster].getCurrentSSE();
            System.out.println("Cluster " + cluster + " size = " + dataset.clusters[cluster].getSize());
        }
        if (dataset.currentIteration > 1 && ((dataset.lastSSE - dataset.currentSSE) / dataset.currentSSE <= dataset.tolerance)) {
            dataset.converged = true;
        }
        if (dataset.verboseIterations) {
            System.out.println("Run: " + dataset.runs
                    + "\tIteration: " + dataset.iterations
                    + "\tSSE: " + dataset.currentSSE);
        }
        if (dataset.currentSSE < dataset.lowSSE) {
            dataset.lowSSE = dataset.currentSSE;
        }
        if (dataset.currentIteration == 1) {
            dataset.runRecords[dataset.runs - 1].initialSSE = dataset.currentSSE;
        }
        dataset.runRecords[dataset.runs - 1].sseArray.add(dataset.currentSSE);
        if (dataset.converged) {
            return;
        }
        if ((dataset.lastSSE < dataset.currentSSE) && dataset.iterations > 1) {

            dataset.runRecords[dataset.runs - 1].numFluctuated++;
            dataset.runRecords[dataset.runs - 1].fluctuated = true;
            dataset.runRecords[dataset.runs - 1].setFluctuatingInstance(dataset);
        }
    }

    public static double getAbsDistance(Point p1, Point p2) {
        double temp = 0, sum = 0;
        for (int attr = 0; attr < p1.numAttributes; attr++) {
            temp = p1.attributes[attr] - p2.attributes[attr];
            sum += temp * temp;
        }
        return sum;
    }

    public static Point getDifference(Point p1, Point p2) {
        double[] point = new double[p1.numAttributes];
        for (int attr = 0; attr < p1.numAttributes; attr++) {
            point[attr] = p1.attributes[attr] - p2.attributes[attr];
        }
        return new Point(point);
    }

    public static Point addPoints(Point p1, Point p2) {
        double[] point = new double[p1.numAttributes];
        for (int attr = 0; attr < p1.numAttributes; attr++) {
            point[attr] = p1.attributes[attr] + p2.attributes[attr];
        }
        return new Point(point);
    }

    public static Point scalePoint(Point point, double scale) {
        double[] scaledPoint = new double[point.numAttributes];
        for (int attr = 0; attr < point.numAttributes; attr++) {
            scaledPoint[attr] = point.attributes[attr] * scale;
        }
        return new Point(scaledPoint);
    }

    public static Point pointAverage(Point[] pointList) {
        double newPoint[] = new double[pointList[0].numAttributes];

        for (int attr = 0; attr < newPoint.length; attr++) {
            double sum = 0;
            for (int point = 0; point < pointList.length; point++) {
                sum += pointList[point].attributes[attr];
            }
            newPoint[attr] = sum / pointList.length;
        }
        return new Point(newPoint);
    }

    public static Point pointAverage(List<Point> pointList) {
        double newPoint[] = new double[pointList.get(0).numAttributes];

        for (int attr = 0; attr < newPoint.length; attr++) {
            double sum = 0;
            for (int point = 0; point < pointList.size(); point++) {
                sum += pointList.get(point).attributes[attr];
            }
            newPoint[attr] = sum / pointList.size();
        }
        return new Point(newPoint);
    }

    public static Point greatestMinDistancePoint(DataSet dataset, int centroidNum) {
        double minDist = Double.MAX_VALUE, maxDist = 0;
        double[] farPoint = new double[dataset.numAttributes];
        for (int point = 0; point < dataset.numPoints; point++) {
            if (!dataset.points[point].isCentroid(dataset)) {
                for (int clusterNum = 0; clusterNum < centroidNum; clusterNum++) {
                    double dist = getAbsDistance(dataset.points[point], dataset.clusters[clusterNum].centroid);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                if (minDist > maxDist) {
                    maxDist = minDist;
                    farPoint = dataset.points[point].attributes;
                }
            }
        }
        return new Point(farPoint);
    }

    public static Point[] selectRandomPoints(Point[] pointList, int numberOfPoints) {

        Point[] randomPoints = new Point[numberOfPoints];
        for (int point = 0; point < numberOfPoints; point++) {
            randomPoints[point] = pointList[r.nextInt(pointList.length)];
        }
        return randomPoints;
    }

    public static void selectRandomCentroids(DataSet dataset) {
        Point[] randomPoints = selectRandomPoints(dataset.points, dataset.numClusters);
        for (int clusterNum = 0; clusterNum < dataset.numClusters; clusterNum++) {
            dataset.clusters[clusterNum] = new Cluster(randomPoints[clusterNum]);
        }
    }

    public static void assignPointsUsingKMeans(DataSet dataset) {
        ClusteringMethod temp = dataset.clusteringMethod;
        dataset.clusteringMethod = ClusteringMethod.KMEANS;
        assignPointsToClusters(dataset);
        dataset.clusteringMethod = temp;
    }

    public static void addPointsToCluster(DataSet dataset) {
        clearClusters(dataset);
        dataset.status = "Clearing clusters";
        for (int point = 0; point < dataset.numPoints; point++) {
            for (int clusterNum = 0; clusterNum < dataset.numClusters; clusterNum++) {
                if (dataset.points[point].cluster == clusterNum) {
                    dataset.clusters[clusterNum].addPoint(dataset.points[point]);
                    break;
                }
            }
        }
    }

    public static void clearClusters(DataSet dataset) {
        for (Cluster cluster : dataset.clusters) {
            cluster.clearPoints();
        }
    }

    public static void genClustersFromCentroids(DataSet dataset, Point[] centroids) {
        for (int clusterNum = 0; clusterNum < centroids.length; clusterNum++) {
            dataset.clusters[clusterNum] = new Cluster(centroids[clusterNum]);
        }
    }

    public static File createSubsetFile(DataSet dataset, int numPoints) throws FileNotFoundException, IOException {
        dataset.status = "Creating Subset";
        FileReader fr = new FileReader(dataset.file);
        BufferedReader br = new BufferedReader(fr);
        File temp = File.createTempFile(dataset.file.getName() + "_tmp", null, new File("."));
        BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
        String output;
        String[] lines = new String[dataset.numPoints + 2];
        int lineNum = 1;
        while ((output = br.readLine()) != null) {
            lines[lineNum] = output;
            lineNum++;
        }
        bw.write(numPoints + " " + dataset.numAttributes + "\n");
        for (int point = 0; point < numPoints; point++) {
            int rand = r.nextInt(numPoints - 1) + 2;
            if (lines[rand] != null) {
                bw.write(lines[rand] + "\n");
            }
        }
        bw.close();
        temp.deleteOnExit();
        return temp;
    }

    public static void printCentroids(DataSet dataset) {
        System.out.println("_______________________Centroids for Iteration " + dataset.currentIteration + "____________________");
        for (int centroidNum = 0; centroidNum < dataset.numClusters; centroidNum++) {
            dataset.clusters[centroidNum].centroid.print();
        }
        System.out.println("_____________________________________________________________________");
    }

    public static String[] fileArray(File directory, final String extension) {
        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File file) {
                if (file.getName().endsWith(extension)) {
                    return true;
                }
                return false;
            }
        };
        File[] files = directory.listFiles(filter);
        String[] fileNames = new String[files.length];
        for (int x = 0; x < files.length; x++) {
            fileNames[x] = files[x].getName();
        }
        return fileNames;
    }
    
    public static ArrayList<DataSet> cueFileToDatasetList(File cueFile)
    {
        ArrayList<DataSet> datasets = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(cueFile));
            String input;
            while ((input = br.readLine()) != null)
            {
                
                String[] parameters = input.split(" ");
                DataSet ds = new DataSet(new File(parameters[0]));
                ds.initMethod = InitMethod.valueOf(parameters[1].toUpperCase());
                ds.clusteringMethod = ClusteringMethod.valueOf(parameters[2].toUpperCase());
                ds.numClusters = Integer.valueOf(parameters[3]);
                ds.maxIterations = Integer.valueOf(parameters[4]);
                ds.maxRuns = Integer.valueOf(parameters[5]);
                try {
                    ds.scale = Double.valueOf(parameters[6]);
                } catch(ArrayIndexOutOfBoundsException aie) {
                    ds.scale = 1.0;
                }
                    datasets.add(ds);
            }
        } catch (IOException ex) {
            Logger.getLogger(ClusteringOps.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return datasets;
    }
    
    public static Number getMinimum(Number... num)
    {
        Number min = num[0];
        for (Number x : num)
        {
            if (x.doubleValue() < min.doubleValue())
            {
                min = x;
            }
        }
        return min;
    }

    public static double getAvg(double[] array) {
        double avg = 0;
        for (int x = 0; x < array.length; x++) {
            avg += array[x];
        }
        return avg / array.length;
    }

    public static double getStDev(double[] array) {
        double avg = getAvg(array);
        double sum = 0;
        for (int x = 0; x < array.length; x++) {
            sum += Math.pow(array[x] - avg, 2);
        }
        return Math.sqrt(sum / (array.length - 1));
    }

    public static double getMinimum(double[] array) {
        double min = Double.MAX_VALUE;
        for (double x : array) {
            if (x < min) {
                if (x != 0) {
                    min = x;
                }
            }
        }
        return min;
    }

    public static double getMaximum(double[] array) {
        double max = Double.MIN_VALUE;
        for (double x : array) {
            if (x > max) {
                max = x;
            }
        }
        return max;
    }

    public static double getMedian(double[] array) {
        Arrays.sort(array);
        int mid_num = array.length / 2;
        if ((array.length % 2) == 0) {
            return ((array[mid_num]) + array[mid_num - 1]) / 2;
        } else {
            return array[mid_num];
        }
    }
    static Random r = new Random();
}
