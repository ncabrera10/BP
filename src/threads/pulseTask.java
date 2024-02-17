package threads;

import java.util.concurrent.atomic.AtomicBoolean;
import dataStructures.DataHandler;
import dataStructures.PendingPulse;
import dataStructures.PulseGraph;

public class pulseTask implements Runnable{

	/**
	 * The pulse type
	 */
	private int type;
	
	/**
	 * The pulse graph
	 */
	private PulseGraph network;
	
	/**
	 * This a flag to stop the task
	 */
	private AtomicBoolean running = new AtomicBoolean(false);
	
	/**
	 * Source node
	 */
	private int source;
	
	/**
	 * Sink node
	 */
	private int sink;
	/**
	 * The main method
	 * @param ty Pulse type: 1 forward; 2 backward
	 */
	public pulseTask(int ty, PulseGraph graph, int s, int t){
		type = ty;
		network = graph;
		source = s;
		sink = t;
	}
	
	/**
	 * This is the main method.
	 * 
	 * Runs the pulse algorithm
	 */
	public void run(){
		
		 running.set(true);
	        while (running.get()) {
	        	if(type == 1){
	        		
	        		//Initial pulse weights:
        			
	        			int[] pulseWeights = new int[2];
	        			pulseWeights[0] = 0;
	        			pulseWeights[1] = 0;
	        			
	        		//Sends the initial pulse:
		        		
	        			PulseGraph.getVertexByID(source-1).pulseFWithQueues(pulseWeights, 0 , 0);
	        			
	        			
	        		//When the first pulse is stopped the queue is full:
	        				
	        			int pendingPulses = DataHandler.pendingQueueF.size();
	        		
	        		//While the queue has at least one element, the search must go on!
	        			
	        			
		        		while(pendingPulses > 0) {
		        			

		        			//Recovers the last pulse (and removes it):
		        				
		        				PendingPulse p = DataHandler.pendingQueueF.remove(pendingPulses-1);
		        				p.setNotTreated(false);
		        				
		        			//The pendingPulse weights:
		        				
		        				pulseWeights[0] = p.getTime();
		        				pulseWeights[1] = p.getDist();
		        				
		        			 //Begins the search:
		        				
		        				if(PulseGraph.getVertexByID(p.getNodeID()).getMinDist() + pulseWeights[1] < PulseGraph.PrimalBound) {
		        					PulseGraph.getVertexByID(p.getNodeID()).pulseFWithQueues(pulseWeights, 0, p.getPredId());
		        				}
		        			//Updates the global queue size (How many are left)
			        		
		        				pendingPulses = DataHandler.pendingQueueF.size();
		        		}
		        	
		        	//Final info: Who ended first, stops the other pulse:
		        		
	        		PulseGraph.finish = true;
	        		network.setFirst(1);
	        		this.interrupt();
	        	}
	        	if(type == 2){
	        	
	        		//Initial pulse weights:
	        				
		        		int[] pulseWeights = new int[2];
		        		pulseWeights[0] = 0;
		        		pulseWeights[1] = 0;
		        		
		        	//Sends the initial pulse:
		        		
		        		PulseGraph.getVertexByID(sink-1).pulseBWithQueues(pulseWeights, 0,sink-1);
		        
		        		
		        	//When the first pulse is stopped the queue is full:
		        		
		        		int pendingPulses = DataHandler.pendingQueueB.size();
	        		
		        	//While the queue has at least one element, the search must go on!
			        	
		        		while(pendingPulses > 0) {
		        			
		        			//Recovers the last pulse (and removes it):
		   
			        			PendingPulse p = DataHandler.pendingQueueB.remove(pendingPulses-1);
			        			p.setNotTreated(false);
		        			
			        			
			        		//The pendingPulse weights:	
			        			
			        			pulseWeights[0] = p.getTime();
				        		pulseWeights[1] = p.getDist();
				        	
				        	//Begins the search:
				        		
				        		if(PulseGraph.getVertexByID(p.getNodeID()).getMinDistB() + pulseWeights[1] < PulseGraph.PrimalBound) {
				        			PulseGraph.getVertexByID(p.getNodeID()).pulseBWithQueues(pulseWeights, 0,p.getPredId());
				        		}
				        		
				        	//Updates the global queue size (How many are left)	
				        
				        		pendingPulses = DataHandler.pendingQueueB.size();
		        		}
		        	//Final info: Who ended first, stops the other pulse:	
		        		
	        		PulseGraph.finish = true;
	        		network.setFirst(2);
	        		this.interrupt();
	        	}
	        }
		
	}
	
	/**
	 * This method interrupts a thread
	 */
	public void interrupt() {
        running.set(false);
    }
	
	/**
	 * This method checks if the thread is active
	 * @return true if the thread is active
	 */
    boolean isRunning() {
        return running.get();
    }
 
  
	
}
