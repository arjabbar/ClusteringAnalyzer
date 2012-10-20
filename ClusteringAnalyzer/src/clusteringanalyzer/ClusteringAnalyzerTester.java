/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clusteringanalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author Abdur Jabbar <arjabbar@yahoo.com>
 */
public class ClusteringAnalyzerTester {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        DataSet ds = new DataSet(new File("ecoli.txt"));
//        ds.runClusteringAlgorithm(Method.DHB, 3, 100, 3);
        ds.runClusteringAlgorithm(InitMethod.MACQUEEN, ClusteringMethod.DHB, 3, 100, 1, 10, 1);
//        Scanner sc = new Scanner(System.in);
//        FileFilter filter = new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                if (file.getName().endsWith(".txt"))
//                {
//                    return true;
//                }
//                return false;
//            }
//        };
//        File[] dir = new File(".").listFiles(filter);
//        
//        for (File file: dir)
//        {
//            for (Method method: Method.values())
//            {
//            System.out.println("▬▬▬▬▬▬▬▬▬▬▬" + file.getName() + "▬▬▬▬▬▬▬▬▬▬▬");
//            DataSet ds = new DataSet(file);
//            ds.runClusteringAlgorithm(method, 3, 100, 1);
//            }
//        }
    }
}
