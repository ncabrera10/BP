/**
 * This is the main class for the pulse algorithm.
 * 
 * Ref.: Lozano, L. and Medaglia, A. L. (2013). 
 * On an exact method for the constrained shortest path problem. Computers & Operations Research. 40 (1):378-384.
 * DOI: http://dx.doi.org/10.1016/j.cor.2012.07.008 
 * 
 * 
 * @author L. Lozano & D. Duque
 * @affiliation Universidad de los Andes - Centro para la Optimizaci�n y Probabilidad Aplicada (COPA)
 * @url http://copa.uniandes.edu.co/
 * 
 */

package model;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import dataStructures.DataHandler;
import dataStructures.DukqstraDist;
import dataStructures.DukqstraTime;
import dataStructures.PendingPulse;
import dataStructures.PulseGraph;
import dataStructures.VertexPulse;
import threads.ShortestPathTask;
import threads.pulseTask;


/**
 * This class represents a pulse algorithm.
 */
public class PulseAlgorithm {

	/**
	 * The name of the file (network)
	 */
	public String fileName;

	/**
	 * The network where the Original pulse will be running
	 */
	public PulseGraph network;

	/**
	 * Name of the network
	 */
	public String networkName;


	/**
	 * Initial Primal Bound
	 */
	public int InitialPrimalBound;


	/**
	 * Computational Time
	 */
	public double computationalTime;
	
	
	/**
	 * Instance id
	 */
	public int instanc;
	
	/**
	 * Last node id
	 */
	public int destiny;
	
	/**
	 * Initialize the attributes of the pulse
	 */
	public PulseAlgorithm(){

		fileName = "";
		network = null;
		networkName = "";
		InitialPrimalBound = 0;
		computationalTime = 0;
	}
	
	
	
	/**
	 * This method runs the pulse
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	 public void bidirectionalPulse(int depth, int instance, String netPath) throws IOException, InterruptedException{

		// Reads the configuration file of the selected instance:
		 
			File testFile = new File("./instances/Config"+instance+".txt");
			instanc = instance;
			
			@SuppressWarnings("resource")
			BufferedReader bufRedr = new BufferedReader(new FileReader(testFile));
			
			String actLine = null;
			
			String [] information = new String [6];
			
			int rowA = 0;
			while((actLine = bufRedr.readLine()) != null && rowA < 6){	
				String [] info = actLine.split(":");
				information[rowA] = info[1];
				rowA++;
			}

		//Modifies the instance tightness
		
			DataHandler data = new DataHandler(Integer.parseInt(information[2]),Integer.parseInt(information[1]),Integer.parseInt(information[5]),1,2,information[0]);
			PulseGraph network = null;
			destiny = Integer.parseInt(information[5]);
			data.ReadDimacs(netPath);
	
		// Initialization:
			
			//Backward direction network:
							
				//Creates the graph:
					
					network = createGraphB(data);
					network.SetConstraint(Integer.parseInt(information[3]));	
								
				//Finds the bounds to reach the source node:
								
					SPB(data,network);
		
				//Stores information for the backward pulse:
								
					int numNodesAct = network.getNumNodes();
					Integer[][] lista = new Integer[numNodesAct][4];
					for (int i = 0; i < numNodesAct; i++) {
						VertexPulse actVertex = PulseGraph.getVertexes()[i];
						lista[i][0] = actVertex.getMaxDistB();
						lista[i][1] = actVertex.getMaxTimeB();
						lista[i][2] = actVertex.getMinDistB();
						lista[i][3] = actVertex.getMinTimeB();
					}
							
			//Forward direction network:
							
				//Creates the graph:
							
				network = createGraphF(data);
				network.SetConstraint(Integer.parseInt(information[3]));
				
				//Finds the bounds to reach the sink node:
					
				Double ITime2 = (double) System.nanoTime();
				SPF(data,network);
				Double FTime2 = (double) System.nanoTime();
				Double iniTime = (FTime2-ITime2)/1000000000;
						
				// Set the first primal bound
							
				int MD=PulseGraph.getVertexByID(data.getLastNode()-1).getMaxDist();
				network.setDestiny(data.getSource()-1);
				network.setPrimalBound(MD);
				InitialPrimalBound = MD;
				network.setTimeStar(PulseGraph.getVertexByID(data.getLastNode()-1).getMinTime());
							
			// Recovers information for the backward pulse
							
				for (int i = 0; i < numNodesAct; i++) {
					VertexPulse actVertex = PulseGraph.getVertexes()[i];
					actVertex.setEveryBound(lista[i]);	
				}
				
				
	// Pulse procedure: 
				
		// Initializes the depth limit:
				
				PulseGraph.depth = depth; //The queue depth
						
		//Starts the clock
						
				Double ITime = (double) System.nanoTime();
				
		//Bidirectional Pulse !
						
				//Check if we already have found the optimal solution: the path of minimim cost is time feasible.
				
				if(PulseGraph.getVertexByID(data.getLastNode()-1).getMaxTime() <= PulseGraph.TimeC) {

					//Set the primal bound and the time star:
					network.setPrimalBound(PulseGraph.getVertexByID(data.getLastNode()-1).getMinDist());
					PulseGraph.TimeStar = PulseGraph.getVertexByID(data.getLastNode()-1).getMaxTime();

				}else { //Otherwise: run the pulse!
					
					runPulses(data,network);
					
				}
							
		//Ends the clock
							
				Double FTime = (double) System.nanoTime();
				

		/*******************************************************************
		 ************************ RESULTS ******************************
		 *******************************************************************
		 */

		networkName = information[0];
		computationalTime = (FTime-ITime)/1000000000 + iniTime*2;
		
		
	}
	
	
	/**
	 * This method creates the graph in forward direction
	 * @param data
	 * @return
	 */
	private static PulseGraph createGraphF(DataHandler data) {
		int numNodes = DataHandler.NumNodes;
		PulseGraph Gd = new PulseGraph(numNodes);
		for (int i = 0; i < numNodes; i++) {
				Gd.addVertex(new VertexPulse(i) );
		}
		for(int i = 0; i <data.NumArcs; i ++){
			Gd.addWeightedEdge( PulseGraph.getVertexByID(DataHandler.Arcs[i][0]), PulseGraph.getVertexByID(DataHandler.Arcs[i][1]),DataHandler.Distance[i],DataHandler.Time[i], i);			
		}
		return Gd;
	}
	
	/**
	 * This method creates the graph in backward direction
	 * @param data
	 * @return
	 */
	private static PulseGraph createGraphB(DataHandler data) {
		int numNodes = DataHandler.NumNodes;
		PulseGraph Gd = new PulseGraph(numNodes);
		for (int i = 0; i < numNodes; i++) {
				Gd.addVertex(new VertexPulse(i) ); 
		}
		
		for(int i = 0; i <data.NumArcs; i ++){
			Gd.addWeightedEdge( PulseGraph.getVertexByID(DataHandler.Arcs[i][1]), PulseGraph.getVertexByID(DataHandler.Arcs[i][0]),DataHandler.Distance[i], DataHandler.Time[i], i);			
		}
		return Gd;
	}
	
	/**
	 * This method obtains the bounds for the pulse in the backward direction
	 * @param data
	 * @param network
	 * @throws InterruptedException
	 */
	private static void SPB(DataHandler data, PulseGraph network) throws InterruptedException {
		
		// Create two threads and run parallel SP for the initialization	
		
			Thread tTime = new Thread();
			Thread tDist = new Thread();
			
		// Reverse the network and run SP for distance and time 
			
			DukqstraDist spDist = new DukqstraDist(network, data.getLastNode()-1,2);
			DukqstraTime spTime = new DukqstraTime(network, data.getLastNode()-1,2);
			
		// Creates the shortest path tasks:
			
			tDist = new Thread(new ShortestPathTask(1, spDist, null));
			tTime = new Thread(new ShortestPathTask(0, null,  spTime));
			
		// Starts the threads:
		
			tDist.start();
			tTime.start();
			
		// Joins the threads:
			
			tDist.join();
			tTime.join();
	}
	
	/**
	 *  This method obtains the bounds for the pulse in the forward direction
	 * @param data
	 * @param network
	 * @throws InterruptedException
	 */
	private static void SPF(DataHandler data, PulseGraph network) throws InterruptedException {
		
		// Creates the threads:
		
			Thread tTime2 = new Thread();
			Thread tDist2 = new Thread();
			
		// Reverse the network and run SP for distance and time 
			
			DukqstraDist spDist2 = new DukqstraDist(network, data.getSource()-1,1);
			DukqstraTime spTime2 = new DukqstraTime(network, data.getSource()-1,1);
			
		// Creates the shortest path tasks:
		
			tDist2 = new Thread(new ShortestPathTask(1, spDist2, null));
			tTime2 = new Thread(new ShortestPathTask(0, null,  spTime2));
			
		// Starts the threads:
			
			tDist2.start();
			tTime2.start();
			
		// Joins the threads:
			
			tDist2.join();
			tTime2.join();
	}
	
	/**
	 * This methods triggers the pulse in both the forward and the backward direction
	 * @param data
	 * @param network
	 * @throws InterruptedException
	 */
	public static void runPulses(DataHandler data, PulseGraph network) throws InterruptedException {
		
		// Creates the threads:
		
			Thread tpulse1 = new Thread();
			Thread tpulse2 = new Thread();
			
		// Initializes the pulse tasks:
			
			ArrayList<pulseTask> threads = new ArrayList<pulseTask>();
			pulseTask task1 = new pulseTask(1,network,data.getLastNode(),data.getSource());
			pulseTask task2 = new pulseTask(2,network,data.getLastNode(),data.getSource());
			
		// Adds the tasks:
				
			threads.add(task1);
			threads.add(task2);
			
		// Creates the threads:
		
			tpulse1 = new Thread(task1);
			tpulse2 = new Thread(task2);
			
		// Starts the threads:
		
			tpulse1.start();
			tpulse2.start();
		
		// Joins the threads:
		
			tpulse1.join();
			tpulse2.join();
	}

	
	/**
	 * This method resets the data handler.
	 * @param data
	 * @param network
	 */
	public static void resetAll(DataHandler data, PulseGraph network) {
		data.CvsInput = null;
		DataHandler.Arcs = null;
		DataHandler.Distance = null;
		DataHandler.pendingQueueB = null;
		DataHandler.pendingQueueF = null;
		network = null;
		System.gc();
		
	}
	
	//Additional methods to recover the path (once the algorithm has finished or it has been stopped heuristically):
	
	/**
	 * Recovers the path when it comes from a path completion with the minimum cost path
	 * @param network
	 * @return path
	 */
	public static ArrayList<Integer> returnPathF(PulseGraph network) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = PulseGraph.finalNodeF;
		boolean finished = false;
		double accumCost = PulseGraph.finalCostF;
		double accumTime = PulseGraph.finalTimeF2;
		
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			for(int i = 0; i < PulseGraph.getVertexByID(initialNode).magicIndex.size(); i++) {
				int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(i);
				int a = DataHandler.Arcs[e][1];
				
				if(accumCost + DataHandler.Distance[e] + PulseGraph.getVertexByID(a).minDist == PulseGraph.PrimalBound ) {
					if(accumTime+ DataHandler.Time[e] + PulseGraph.getVertexByID(a).maxTime == PulseGraph.TimeStar) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
						actualNode = a;	
					}
				}
				
			}
		
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
			}else {
				initialNode = actualNode;
			}
		}
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		
		//Go back:
		
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostF2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeF2;
		initialNode = PulseGraph.finalNodeF;
		while(finished == false) {
			int actualNode = 0;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendF;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
							int a = DataHandler.Arcs[e][0];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendF;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == 0) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
					int a = DataHandler.Arcs[e][0];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		
		return path;
	}
	
	/**
	 * Recovers the path when it comes from a path completion with the minimum cost path
	 * @param network
	 * @return path
	 */
	public static ArrayList<Integer> returnPathB(PulseGraph network) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = PulseGraph.finalNodeB;
		boolean finished = false;
		double accumCost = PulseGraph.finalCostB2;
		double accumTime = PulseGraph.finalTimeB2;
		while(finished == false) {
			int actualNode = 0;
			for(int i = 0; i < PulseGraph.getVertexByID(initialNode).magicIndex2.size(); i++) {
				int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(i);
				int a = DataHandler.Arcs[e][0];
				if(accumCost+ DataHandler.Distance[e] + PulseGraph.getVertexByID(a).minDistB == PulseGraph.PrimalBound ) {
					if(accumTime + DataHandler.Time[e] + PulseGraph.getVertexByID(a).maxTimeB == PulseGraph.TimeStar) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
						actualNode = a;	
					}
				}
			}
		
			path.add(actualNode);
			if(actualNode == 0) {
				finished = true;
			}else {
				initialNode = actualNode;
			}
		}
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		
		//Go back:
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostB2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeB2;
		initialNode = PulseGraph.finalNodeB;
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendB;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
							int a = DataHandler.Arcs[e][1];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendB;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
					int a = DataHandler.Arcs[e][1];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
		
		return path;
	}
	
	/**
	 * Recovers the path when it comes from a path completion with the minimum time path
	 * @param network
	 * @return path
	 */
	public static ArrayList<Integer> returnPathF2(PulseGraph network) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = PulseGraph.finalNodeF;
		boolean finished = false;
		double accumCost = PulseGraph.finalCostF2;
		double accumTime = PulseGraph.finalTimeF2;
		
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			for(int i = 0; i < PulseGraph.getVertexByID(initialNode).magicIndex.size(); i++) {
				int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(i);
				int a = DataHandler.Arcs[e][1];
				
				if(accumCost + DataHandler.Distance[e] + PulseGraph.getVertexByID(a).maxDist == PulseGraph.PrimalBound ) {
					if(accumTime + DataHandler.Time[e] + PulseGraph.getVertexByID(a).minTime == PulseGraph.TimeStar) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
						actualNode = a;	
					}
				}
				
			}
		
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
			}else {
				initialNode = actualNode;
			}
		}
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		//Go back:
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostF2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeF2;
		initialNode = PulseGraph.finalNodeF;
		while(finished == false) {
			int actualNode = 0;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendF;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
							int a = DataHandler.Arcs[e][0];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendF;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == 0) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
					int a = DataHandler.Arcs[e][0];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
	
		return path;
	}
	
	/**
	 * Recovers the path when it comes from a path completion with the minimum time path
	 * @param network
	 * @return path
	 */
	public static ArrayList<Integer> returnPathB2(PulseGraph network) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = PulseGraph.finalNodeB;
		boolean finished = false;
		double accumCost = PulseGraph.finalCostB2;
		double accumTime = PulseGraph.finalTimeB2;
		while(finished == false) {
			int actualNode = 0;
			for(int i = 0; i < PulseGraph.getVertexByID(initialNode).magicIndex2.size(); i++) {
				int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(i);
				int a = DataHandler.Arcs[e][0];
				if(accumCost + DataHandler.Distance[e] + PulseGraph.getVertexByID(a).maxDistB == PulseGraph.PrimalBound ) {
					if(accumTime + DataHandler.Time[e] + PulseGraph.getVertexByID(a).minTimeB == PulseGraph.TimeStar) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
						actualNode = a;
					}
				}
			}

			path.add(actualNode);
			if(actualNode == 0) {
				finished = true;
			}else {
				initialNode = actualNode;
			}
		}
	
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		//Go back:
		
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostB2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeB2;
		initialNode = PulseGraph.finalNodeB;
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendB;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
							int a = DataHandler.Arcs[e][1];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendB;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
					int a = DataHandler.Arcs[e][1];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
	
		return path;
	}
	
	
	/**
	 * Recovers the path when it was found by the path joins strategy
	 * @param network
	 * @return path
	 */
	public static ArrayList<Integer> returnPathJP(PulseGraph network) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = PulseGraph.finalNodeB;
		boolean finished = false;
		double accumCost = PulseGraph.finalCostB2;
		double accumTime = PulseGraph.finalTimeB2;
		
	
		//Forward direction
		
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostF2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeF2;
		initialNode = PulseGraph.finalNodeF;
		while(finished == false) {
			int actualNode = 0;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendF;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
							int a = DataHandler.Arcs[e][0];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendF;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == 0) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex2.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex2.get(j);
					int a = DataHandler.Arcs[e][0];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
		
		for (int i = 0; i < path.size() / 2; i++) {
		     Object temp = path.get(i);
		     path.set(i, path.get(path.size() - i - 1));
		     path.set(path.size() - i - 1, (Integer) temp);
		   }
		//Backward direction
		
		finished = false;
		accumCost = PulseGraph.PrimalBound - PulseGraph.finalCostB2;
		accumTime = PulseGraph.TimeStar - PulseGraph.finalTimeB2;
		initialNode = PulseGraph.finalNodeB;
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			boolean found = false;
			ArrayList<PendingPulse> pendingPulses = PulseGraph.getVertexByID(initialNode).pendB;
			for(int i = 0; i < pendingPulses.size() && !found;i++) {
				PendingPulse p = pendingPulses.get(i);
				if(p.getDist() +accumCost == PulseGraph.PrimalBound) {
					if(p.getTime() +  accumTime == PulseGraph.TimeStar) {
						actualNode = p.getPredId();
						for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size() && !found; j++) {
							int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
							int a = DataHandler.Arcs[e][1];
							if(a == actualNode) {
								ArrayList<PendingPulse> pendingPulsesAux = PulseGraph.getVertexByID(actualNode).pendB;
								for(int ii = 0; ii < pendingPulsesAux.size();ii++) {
									PendingPulse pp = pendingPulsesAux.get(ii);
									if(pp.getDist() + accumCost + DataHandler.Distance[e] == PulseGraph.PrimalBound && pp.getTime() + accumTime + DataHandler.Time[e] == PulseGraph.TimeStar ) {
										found = true;
										accumCost+=DataHandler.Distance[e];
										accumTime+=DataHandler.Time[e];
									}
								}
							}
						}
					}
				}
			}
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
				for(int j = 0; j < PulseGraph.getVertexByID(initialNode).magicIndex.size(); j++) {
					int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(j);
					int a = DataHandler.Arcs[e][1];
					if(a == actualNode) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
					}
				}
			}else {
				initialNode = actualNode;
			}
		}		
		
		return path;
	}
	
	/**
	 * Recover the path when it was found in the initialization step: follow the minimum cost path
	 * @param network
	 * @return
	 */
	public static ArrayList<Integer> returnPathIni(PulseGraph network) {
		
		ArrayList<Integer> path = new ArrayList<Integer>();
		int initialNode = 0;
		boolean finished = false;
		double accumCost = 0;
		double accumTime = 0;
		path.add(0);
	
		while(finished == false) {
			int actualNode = PulseGraph.destiny;
			for(int i = 0; i < PulseGraph.getVertexByID(initialNode).magicIndex.size(); i++) {
				int e = (Integer) PulseGraph.getVertexByID(initialNode).magicIndex.get(i);
				int a = DataHandler.Arcs[e][1];
				if(accumCost + DataHandler.Distance[e] + PulseGraph.getVertexByID(a).maxDist == PulseGraph.PrimalBound ) {
					if(accumTime+ DataHandler.Time[e] + PulseGraph.getVertexByID(a).minTime == PulseGraph.TimeStar) {
						accumCost+=DataHandler.Distance[e];
						accumTime+=DataHandler.Time[e];
						actualNode = a;	
					}
				}
				
			}
		
			path.add(actualNode);
			if(actualNode == PulseGraph.destiny) {
				finished = true;
			}else {
				initialNode = actualNode;
			}
		}
		return path;
	}
	
	/**
	 * This method recovers the final path
	 * @param network
	 * @return
	 */
	public static ArrayList<Integer> recoverThePath(PulseGraph network){
		ArrayList<Integer> path = new ArrayList<Integer>();
	
		if(PulseGraph.best == 1) { //The path was found using the minimum cost path completion (Forward)
			path = returnPathF(network);
		}
		else if (PulseGraph.best == 2) { //The path was found using the minimum time path completion (Forward)
			path = returnPathF2(network);
		}
		else if(PulseGraph.best == 3) { //The path was found using the minimum cost path completion (Backward)
			path = returnPathB(network);
		}	
		else if(PulseGraph.best == 4) { //The path was found using the minimum time path completion (Backward)
			path = returnPathB2(network);
		}
		else if(PulseGraph.best == 5){ //The path was found using the join paths strategy
			path = returnPathJP(network);
		}
		else { //The minimum cost path is feasible
			path = returnPathIni(network);
		}
		
		return path;
	}
	
	public static String whoFindThePath(PulseGraph network){
		String rta = "";
	
		if(PulseGraph.best == 1) {
			rta = "Minimum cost path completion in forward direction";
		}
		else if (PulseGraph.best == 2) {
			rta = "Minimum time path completion in forward direction";
		}
		else if(PulseGraph.best == 3) {
			rta = "Minimum cost path completion in backward direction";
		}	
		else if(PulseGraph.best == 4) {
			rta = "Minimum time path completion in backward direction";
		}
		else if(PulseGraph.best == 5) {
			rta = "Join paths!";
		}
		else {
			rta = "The initialization step";
		}
		
		return rta;
	}
	
}
