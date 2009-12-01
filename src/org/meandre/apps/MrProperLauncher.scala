package org.meandre.apps

import org.meandre.kernel.rdf._
import org.meandre.kernel.actors._

import org.meandre.kernel.logger.LoggerContainer._


/** This application is the entry point to launch the execution of 
 *  MrProper, the distributed execution coordinator for a given flow. 
 * 
 * @author Xavier Llora
 */
object MrProperLauncher {

	 /** Starts the execution of at MrProper for a flow.
	  *
	  * @param args Requires <port> <repository_url> <flow_uri> 
	  */
	 def main(args: Array[String]) : Unit = {
			if (args.size!=3) {
				println("MrProperLauncher requires <port> <repository_url> <flow_uri>")            
			}
			else try {
			        val    port = args(0).toInt
			        val     url = args(1)
			        val flowUri = args(2) 
           
			        log info "MrProper started on port "+port
			       
			    	log info "Retrieving repository located at "+url
			    	val components = DescriptorsFactory.buildComponentDescriptors(url)
			    	val flows = DescriptorsFactory.buildFlowDescriptors(url)
			    	log info "Starting MrProper for flow "+flowUri 
			    	val mp = new MrProperActor(port,flows.filter(_.uri==flowUri)(0),components)
			    	mp !? Status("NOP")
			    	log info "MrProper for "+flowUri+" ready"
			    	1 
			    	
			   }
			   catch {
			     case e => log severe "Failed to start because of "+e
			   }
    }

}
