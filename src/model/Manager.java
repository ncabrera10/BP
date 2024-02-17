package model;

import java.io.IOException;

public class Manager {

	/**
	 * Creates a new manager, contains all the algorithms.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Manager() throws IOException, InterruptedException{

	}
	
	/**
	 * Runs the bidirectional pulse
	 * @param depth
	 * @param instance
	 * @param netPath
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public PulseAlgorithm runBidirectionalPulse(int depth,int instance, String netPath)throws IOException, InterruptedException {
		PulseAlgorithm pulso = new PulseAlgorithm();
		pulso.bidirectionalPulse(depth,instance,netPath);
		return pulso;
		
	}

}
