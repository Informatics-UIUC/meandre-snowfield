package org.meandre.kernel.actors

import scala.actors._
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node

import scala.collection.immutable.Queue
import scala.collection.mutable.Map

import org.meandre.core._
import org.meandre.kernel.rdf._
import org.meandre.kernel.logger.LoggerContainer._

/** Implements MrProper a.k.a the distributed flow execution coordinator.
 *  
 * @param port The port where MrProper will bind
 * @param flow The flow description describing the flow being executed
 * @param components The list of component descriptors used in the flow
 * 
 * @author Xavier Llora
 */
class MrProperActor ( port:Int,flow:FlowDescriptor, components:List[ComponentDescriptor] ) extends Actor {
 
  var dataUnitsCounter = 0L
  
  var fired = 0
  var exited = 0
  
  val connectedInstanceSet = Set({for ( con<-flow.connectors) yield con.targetInstance }:_*)
  var instancesMap = Map[String,AbstractActor]()
  var registry:List[(String,String,Int)] = Nil
  var instancesSet = Set[String]()
  
  def act () = {
    
    RemoteActor.classLoader = getClass().getClassLoader()
    alive(port)  
    register(Symbol(flow.uri), self) 
    trapExit = true
    
    loop {
      react {
        case Register(uri,hostName,currentPort) => 
          			log info "Register request by "+uri+" on "+hostName+":"+currentPort
          			reply(Message(uri,"REGISTER","OK"))
             		registry = (uri,hostName,currentPort)::registry
             		instancesSet = instancesSet+uri
             		if ( flow.instances.size==instancesSet.size ) {
             			// Ready to broadcast all the information
             			log info "Broadcasting information for the "+instancesSet.size+" registered instances"
             			pingAndBroadcast
             		}
          
        case Fired(uri) => 
          			log finest "Component "+uri+" fired" 
          			fired+=1 
             
        case CoolingDown(uri,consumed,pushed) => 
          			dataUnitsCounter = dataUnitsCounter-consumed+pushed
          			log finest "Component "+uri+" cooling down after consuming "+consumed+" and pushing "+pushed+" data units"
          			fired-=1 
          			if ( dataUnitsCounter==0L ) {
          				log info "No data available in any component sending dispose messages and exiting MrProper"
          				instancesMap.foreach( (im) => im._2 !? Dispose() )
          				Thread sleep 100
          				exit('FlowIsDone)
          			}
             
        case Exiting(uri) => 
          			log info "Component "+uri+" exiting" 
          			exited += 1
          			println(">>>"+exited+"=="+instancesSet.size)
          			if ( exited==instancesSet.size ) {
          			  log info "Exiting MrProper for "+flow.uri
          			  exit()
          			}
        
        case Status("NOP") => 
          			log info "NOP received by "+flow.uri
          			reply(Message(flow.uri,"NOP","NOP"))
        
        case e => log warning "Received an unknown message "+e+". Discarding it..."
      }
    }
  }

  def pingAndBroadcast = {
	  // TODO: Need to clean up the registry for multiple entries
	  for ( (uri,hostName,port)<-registry ) {
	     // Select the actor
		 val actor = select( Node(hostName,port), Symbol(uri)) 
		 link(actor)
		 // Check if alive
		 actor !? Status("PING") match {
		   case Message(uriActor,"PING","PONG") if uriActor==uri => 
		     	// Broadcast  the registry
		     	actor ! Message(uriActor,"REGISTRY",registry)
		   case _ => log warning "Failed to connect with instance "+uri
		 }
	     actor ! Initialize()
	     instancesMap(uri) = actor
	  }
	  
  }
  
  start()
  
}

