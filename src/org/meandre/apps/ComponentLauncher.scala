package org.meandre.apps

import scala.collection.mutable.Map

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node

import org.meandre.kernel.rdf._
import org.meandre.kernel.actors._

import org.meandre.kernel.logger.LoggerContainer._

/** This application is the entry point to launch the execution of at
 *  least one component instance for a given flow. 
 * 
 * @author Xavier Llora
 */
object ComponentLauncher {

	 /** Starts the execution of at least one instance in a flow.
	  *
	  * @param args Requires the following command line arguments <mr_proper_host> <mr_proper_port> <component_host> <component_base_port> <repository_url> <flow_uri> <instance_uri>+
	  */
	 def main(args: Array[String]) : Unit = {
			if (args.size<6) {
				println("ComponentLauncher requires <mr_proper_host> <mr_proper_port> <component_host> <component_base_port> <repository_url> <flow_uri> <instance_uri>+")            
			}
			else try {
				val mrProperAddress = args(0)
				val mrProperPort = args(1).toInt
				var componentHost = args(2)
				var basePort = args(3).toInt
				val url = args(4)
				val flowUri = args(5)
				val instanceUris = args.slice(6,args.size)

				val mpn = Node(mrProperAddress,mrProperPort)

				log info "Retrieving repository located at "+url
			    val components = DescriptorsFactory.buildComponentDescriptors(url)
			    val flows = DescriptorsFactory.buildFlowDescriptors(url)
			    val flow = flows.filter(_.uri==flowUri)(0)
       
			    log info "MrProper for "+flowUri+" should be at "+mrProperAddress+":"+mrProperPort 
			    log info "Base port for this component launcher is set to "+componentHost+":"+basePort 
			    
			    for ( uri <- instanceUris ) log info "Creating component instance "+uri
			    val actors = for ( uri <- instanceUris )  
			    				yield new ComponentActor(
			    						flow, 
			    						flow.instances.filter(_.uri==uri)(0), 
			    						Map({for (c<-components) yield c.uri->c}:_*), 
			    						Node(mrProperAddress,mrProperPort), 
			    						componentHost,
			    						() => basePort )
			   
				for ( a<-actors ) {
			    	log info "Sending a NOP to "+a.cid.uri
			    	a !? Status("NOP")
			    }
			}
			catch {
				case e => log severe "Failed to start because of "+e
			}

	}

}
