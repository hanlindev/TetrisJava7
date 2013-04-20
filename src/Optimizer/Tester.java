package Optimizer;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.apache.commons.math3.stat.*;

public class Tester {
	// The main method takes one argument - the path to the file
	// that contains the weights that are going to be tested.
	// A single set of weights should be a comma separated list of
	// real numbers. E.g
	// 1.0,2.0,3.0,-4.0,5
	// There are no spaces between numbers.
	// The first line in the file should specify how many lines of weights
	// there are in the file.
	// Output is to the standard output, so you can use > file to direct the
	// standard output to a file
	static public void main(String[] args) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		int noLines = Integer.parseInt(br.readLine());
		double[][] weightList = new double[noLines][];
		int weightIndex = 0;
		// Get the weights
		while (br.ready()) {
			String line = br.readLine();
			String[] entries = line.split(",");
			double[] weights = new double[entries.length];
			int index = 0;
			for (String anEntry : entries) {
				weights[index++] = Double.parseDouble(anEntry);
			}
			weightList[weightIndex++] = weights;
		}
		
		// submit tasks
		ArrayList<Future<Long>> futureList = new ArrayList<Future<Long>>();
		ForkJoinPool mainPool = new ForkJoinPool(5 * noLines * TestUnit.numTested);
		for (int i = 0; i < weightList.length; ++i) {
			String fileName = "resultFromWeight" + i;
			futureList.add(mainPool.submit(new TestUnit(fileName, weightList[i])));
		}
		
		for (int i = 0; i < futureList.size(); ++i) {
			Future<Long> aFuture = futureList.get(i);
			System.out.println("Weight1 cleared: " + aFuture.get() + " lines");
		}
		mainPool.shutdown();
		br.close();
	}
}

class TestUnit extends RecursiveTask<Long>{
	String outFileName;
	double[] weights;
	static public int numTested = 200;
	
	public TestUnit(String fileName, double[] weights) {
		this.outFileName = fileName + ".txt";
		this.weights = weights;
	}

	@Override
	protected Long compute() {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFileName));
			ForkJoinPool mainPool = new ForkJoinPool(5 * numTested);
			ArrayList<Future<Long>> futureList = new ArrayList<Future<Long>>();
			// First, print out the weights
			String pv = "";
			String comma = "";
			for (double aWeight : weights) {
				pv += comma + aWeight;
				comma = ",";
			}
			bw.write(pv + "\n");
			
			// Create numTested players and pass into the pool
			for (int i = 0; i < numTested; ++i) {
				futureList.add(mainPool.submit(new PlayerSkeletonTester(weights)));
			}
			
			// Get the results
			long[] results = new long[numTested];
			double[] doubleResults = new double[numTested];
			for (int i = 0; i < numTested; ++i) {
				results[i] = futureList.get(i).get();
				doubleResults[i] = results[i];
				bw.write("Player" + i + " cleared " + results[i] + " lines\n");
				bw.flush();
			}
			
			// Get the median
			long median = (long) StatUtils.percentile(doubleResults, 50);
			long mean = (long) StatUtils.mean(doubleResults);
			double variance = (double) StatUtils.variance(doubleResults);
			bw.write("Median: " + median + "\n");
			bw.write("Mean: " + mean + "\n");
			bw.write("Variance: " + variance + "\n");
			bw.flush();
			bw.close();
			mainPool.shutdown();
			return median;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}