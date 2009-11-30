package org.meandre.apps

import org.meandre.kernel.rdf._
import org.meandre.kernel.actors._

import org.meandre.kernel.logger.LoggerContainer._

object MrProperLauncher {

	def main(args: Array[String]) : Unit = {
			if (args.size!=3) {
				println("MrProperLauncher requires <port> <repository_url> <flow_uri>")            
			}
			else try {
			        val    port = args(0).toInt
			        val     url = args(1)
			        val flowUri = args(2) 
           
			        log.info("MrProper started on port "+port)
			       
			        // Just for debug purposes
			        import org.meandre.kernel.logger.LoggerContainer._
			        import java.util.logging.Level
			    	val logLevel = log.getLevel
			    	log.setLevel(Level.FINEST)
			    	
			    	log.info("Retrieving repository located at "+url)
			    	val components = DescriptorFactory.buildComponentDescriptors(url)
			    	val flows = DescriptorFactory.buildFlowDescriptors(url)
			    	log.info("Starting MrProper for flow "+flowUri)
			    	val mp = new MrProperActor(port,flows.filter(_.uri==flowUri)(0),components)
			    	mp !? Status("NOP")
			    	log info "MrProper for "+flowUri+" ready"
			    	1 
			    	
			   }
			   catch {
			     case e => log.severe("Failed to start because of "+e)
			   }
    }

}
