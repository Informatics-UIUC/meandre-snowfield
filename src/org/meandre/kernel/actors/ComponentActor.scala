package org.meandre.kernel.actors

import scala.actors.AbstractActor
import scala.actors.{Actor,Exit}
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node

import scala.collection.immutable.Queue
import scala.collection.mutable.Map

import org.meandre.core._
import org.meandre.kernel.rdf._
import org.meandre.kernel.logger.LoggerContainer._
import org.meandre.kernel.actors._


/** Implements the componet actor that wraps one component instance.
 *  
 * @param flow The flow description describing the flow being executed
 * @param cid The component instance description
 * @param components The list of component descriptors used in the flow
 * @param mrProperNode The node to with MrProper is binded
 * @param hostName The name of the host to bind this actor to
 * @param hostPort The function returning the port number to bind this actor to
 * 
 * @author Xavier Llora
 */
class ComponentActor ( val flow:FlowDescriptor, val cid:ComponentInstanceDescription, val components:Map[String,ComponentDescriptor], mrProperNode:Node, hostName:String, hostPort:() => Int) extends Actor {

  protected val REGISTRATION_ATTEMPTS = 6

  val currentPort = hostPort()
  var registry:List[(String,String,Int)] = null
  var instances:Map[String,AbstractActor] = Map[String,AbstractActor]()
    
  log info "Initializing instance "+cid.uri+" on "+hostName+":"+currentPort
  //
  // The basic component instnace structural information
  //
  var dataPushed = 0L
  var mrProper:AbstractActor = null
  val component = components(cid.componentUri)
  val uri = cid.uri
  var outputs:Map[String,String] = Map[String,String]()
  val outputMap = Map({for (port<-component.outputs) yield port.name->port.uri}:_*)
  val outputInstancesMap = Map({for (con<-flow.connectors) yield con.sourceInstanceDataPort->con.targetInstance}:_*)
  val outputPortMap = Map({for (con<-flow.connectors if con.sourceInstance==uri) yield con.sourceInstanceDataPort->con.targetInstanceDataPort}:_*)
  val inputMap = Map({for (port<-component.inputs) yield port.uri->port.name}:_*)
  val inputs:MultiQueue = new MultiQueue({for ( port<-component.inputs) yield port.name})
  val reverseInputMap = Map({for (port<-component.inputs) yield port.uri->port.name}:_*)
  val properties:Map[String,String] = Map({{for ( port<-component.properties.toList) yield port._1 -> port._2.value }:::
                                           {for ( port<-cid.properties.toList) yield port._1 -> port._2.value}}:_*)
  //   
  // The function that implements the firing action
  //
  protected def fire = {
	    try { 
	      dataPushed = 0L
	      mrProper ! Fired(uri)
	      this.execute
	    } 
	  	catch { 
	  	  case e => requestAbort(e.toString) 
	  	}
        finally {
          mrProper ! CoolingDown(uri,inputs.available,dataPushed)
          inputs.dequeue
        }
      }
  
  //
  // The constructed conditional firing function based on the component firing policy
  //
  val conditionalFiring = {
	  component.firingPolicy match {
	    case FiringAny() => () => fire
	    case FiringAll() => () => if ( inputs.available==inputs.slots.size ) fire
	  }
  }
  
  var eci:ExecutableComponent = null
  var context = new Context(this)
    
  
  //
  // Attempts to register
  //
  def reliableRegistration ( attempt:Int ):Unit = attempt match {
       case 0 => log severe "Failed to register with MrPropper. Forcing abort"
                 System exit 1
       case n => mrProper.!?(1000,Register(uri,hostName,currentPort)) match {
                    case Some(Message(uri,"REGISTER","OK")) =>
        	                // Successful registration
                          log info "Registration request succeeded for instance "+uri
                    case None =>
                          // Timeout, try again
                          log warning "Registration time out. Retrying. "+(attempt-1)+" attempts left."
                          reliableRegistration(attempt-1)
                    case Some(e) =>
        	                log severe "Could not register because of "+e+". Bailing out..."
        	                exit
                  }
  }
   
 
  // 
  // The main act loop react cycle
  //
  def act () = {
    // Reach out to MrProper to register as an instance
    RemoteActor.classLoader = getClass().getClassLoader()
    log info "Linking component actor "+uri+" to MrProper on "+flow.uri
    alive(currentPort)     
    register(Symbol(uri), self) 
    mrProper = select(mrProperNode, Symbol(flow.uri)) 
    log info "Sending register request to MrProper for "+uri
    reliableRegistration(REGISTRATION_ATTEMPTS)
    trapExit = true
    
    // And the main react loop 
    loop {
      react {
        // Some data arrived to the component instance
        case Data(port,payload) if inputMap.contains(port) => process(port,payload)
        case Data(port,payload) if !inputMap.contains(port) => 
          	log warning "Component "+component.description.name+" receive message for unknown port "+port+". Available ("+inputs+")"
        
        // Initialization request received
        case Initialize() => initialize
        
        // Dispose request received
        case Dispose() => dispose
        
        // A NOP status request
        case Status("NOP") => 
          	log info "NOP received by "+uri
          	reply(Message(uri,"NOP","NOP"))
           
        // A PING status request
        case Status("PING") => 
          	log info "PING received by "+uri
          	reply(Message(uri,"PING","PONG"))
           
        // An unknow status request
        case Status(cmd) => 
          	log info "Processing status "+cmd
          	reply(Message(uri,cmd,"Reply for status request "+cmd+" on "+uri))
        
        // A message from MrProper bradcasting the collected registry
        case Message(uriActor,"REGISTRY",broadcastedRegistry) =>
          	log info "Registry received from MrProper"
          	initializeRegistry(broadcastedRegistry.asInstanceOf[List[(String,String,Int)]])
          	
        // An exit request generated by the exit request of MrProper
        case Exit(from, target) => 
          	log info "Component "+uri+" exiting by request of "+from+" because "+target
            exit()
            
        // An unknow message was received
        case e => 
          	log warning "Received an unknown message "+e+". Discarding it..."
      }
    }
  }
    
  /** Initialize the registry 
   * 
   * @param registry The list of component intances that form the flow 
   *                 and their locations with tuples (uri,host,port)
   */
  def initializeRegistry ( registry:List[(String,String,Int)] ) = {
	  for ( (uri,host,port)<-registry if !instances.contains(uri) ) {
	    instances(uri)=select(Node(host,port),Symbol(uri))
	  }
	  
     // Prepare the input and output port names if not done before
     if ( outputs.size == 0 ) {
	    val portTranslation = Map({
	    		for (con<-flow.connectors) 
	    			yield if (uri==con.sourceInstance) con.sourceInstanceDataPort -> con.targetInstanceDataPort else ""->"" 
	    }:_*)
	    outputs ++= Map({for ( port<-component.outputs) yield port.name -> portTranslation(port.uri) }:_*)
     }
  }
  
  
  /** Process a piece of data received.
   * 
   * @param port The target port for the received data
   * @param payload The payload received
   */
  def process ( port:String, payload:Any ) = {
     log finest component.description.name+" processing data received on port "+port 
     inputs(reverseInputMap(port))=payload
     conditionalFiring()
  }
  
  /** Push a piece of data to the right actor 
   * 
   * @param The target port name
   * @param payload The payload to deliver
   */
  def pushData ( port:String, payload:Any ) = {
    dataPushed += 1L         
    val portUri = outputMap(port)
    val act = instances(outputInstancesMap(portUri))
    act ! Data(outputPortMap(portUri),payload)
  }
  
  /** Executes a component */
  def execute = {
    log finest component.description.name+" fired"
    eci execute context
  }
    
  /** Invokes the initialization of a component instance actually loading its implementation */
  def initialize = {
    // Instantiate the component
    eci = component.runnable match {
    	case "java"   if component.format=="java/class" => new JavaWrapper(uri,component,this)
    	case "python" if component.format=="jython"     => new PythonWrapper(uri,component,this)
    	case "lisp"   if component.format=="clojure"    => new LispWrapper(uri,component,this)
    	case _ => { this requestAbort "Unknow component language "+component.runnable ; new ExecutableComponent() {
    		def initialize ( ccp:ComponentContextProperties ) = {log.warning("Dummy initialize called for instance "+uri)}
      		def execute ( cc: ComponentContext ) = {log.warning("Dummy execute called for instance "+uri)}
    		def dispose ( ccp:ComponentContextProperties ) = {log.warning("Dummy dispose called for instance "+uri)}
    	}}
    }
    // Call initiliaze
    log info component.description.name+" initializing" 
    eci initialize context
    // Check if it has no inputs so fire only once
    if ( inputs.slots.size==0 ) fire
  }
  
 
  /** Dispose invocation with notification of completion to MrProper */
  def dispose = {
    log info component.description.name+" disposing"
    eci dispose context
    reply(Exiting(uri))
  }
  
  //
  // Other services
  // 
    
  /** Request the flow execution to abort.
   * 
   * @param msg The message attach to the abort requrest
   */
  def requestAbort(msg:String) = {
    log info "Component "+uri+" requresting abort excecution because "+msg
    mrProper ! Abort(uri)
    mrProper ! Exiting(uri)
  }
  
  // Starts the actor  
  start()
  
}
