package main;

import java.io.IOException;

import model.Manager;
import model.PulseAlgorithm;
import view.ManagerView;

public class Main {

	/**
	 * This class runs the BP procedure. 
	 * The user can select the instance and the depth limit (Pulse queuing strategy).
	 * 
	 * @author nicolas.cabrera-malik
	 *
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		
		/*********************************************************************************************************
		 ************************ Select instance **********************
		 **BAY: 1 - 40
		 **NY: 41 - 80
		 **COL: 81 - 120
		 **FLA: 121 - 160
		 **NE: 161 - 200
		 **CAL: 201 - 240
		 **LKS: 261 - 280
		 **E: 281 - 320
		 **W: 321 - 360
		 **CTR: 361 - 400
		 **USA: 401 - 420
		 *********************************************************************************************************
		 */
		
		int ins = 23;
		
		/*********************************************************************************************************
		 ************************************** Select the pulse depth limit *************************************
		 *********************************************************************************************************
		 */
		
		int depth = 3;  //Pulse depth limit

		//Create the manager's
		
		Manager model = new Manager();
		ManagerView view = new ManagerView();
		
		/*********************************************************************************************************
		 ************************ Modify this line according to the path on your computer ************************
		 ************************ A folder with a .txt file with each of the DIMANCS networks ********************
		 *********************************************************************************************************
		 */
		
		String netPath = "./Networks/";
		
		/*********************************************************************************************************
		 ***************************** The following lines run the selected instances*****************************
		 *********************************************************************************************************
		 */
		
		PulseAlgorithm bidirectionalPulse = model.runBidirectionalPulse(depth, ins, netPath);
		view.printResults(bidirectionalPulse);
				
	}
	
}
