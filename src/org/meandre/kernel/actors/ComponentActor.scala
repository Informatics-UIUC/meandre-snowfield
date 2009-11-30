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

//
// The component actor
//
class ComponentActor ( val flow:FlowDescriptor, val cid:ComponentInstanceDescription, val components:Map[String,ComponentDescriptor], mrProperNode:Node, hostName:String, hostPort:() => Int) extends Actor {

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
 
  def fire = {
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
  
  val conditionalFiring = {
	  component.firingPolicy match {
	    case FiringAny() => () => fire
	    case FiringAll() => () => if ( inputs.available==inputs.slots.size ) fire
	  }
  }
  
  var eci:ExecutableComponent = null
  var context = new Context(this)
    
   
  // 
  // The main act loop react cycle
  //
  def act () = {
    // Get the link to MrProper up
    RemoteActor.classLoader = getClass().getClassLoader()
    log info "Linking component actor "+uri+" to MrProper on "+flow.uri
    alive(currentPort)     
    register(Symbol(uri), self) 
    mrProper = select(mrProperNode, Symbol(flow.uri)) 
    //link(mrProper)
    log info "Sending register request to MrProper for "+uri
    mrProper !? Register(uri,hostName,currentPort) match {
      case Message(uri,"REGISTER","OK") =>
        	log info "Registration request succeeded for instance "+uri
      case e =>
        	log severe "Could not register because of "+e+". Bailing out..."
        	exit
    }
    trapExit = true
    
    // And loop 
    loop {
      react {
        case Data(port,payload) if inputMap.contains(port) => process(port,payload)
        case Data(port,payload) if !inputMap.contains(port) => 
          	log warning "Component "+component.description.name+" receive message for unknown port "+port+". Available ("+inputs+")"
        
        case Initialize() => initialize
        case Dispose() => dispose
        
        case Status("NOP") => 
          	log info "NOP received by "+uri
          	reply(Message(uri,"NOP","NOP"))
           
        case Status("PING") => 
          	log info "PING received by "+uri
          	reply(Message(uri,"PING","PING"))
           
        case Status(cmd) => 
          	log info "Processing status "+cmd
          	reply(Message(uri,cmd,"Reply for status request "+cmd+" on "+uri))
        
        case Message(uriActor,"REGISTRY",broadcastedRegistry) =>
          	log info "Registry received from MrProper"
          	//println(broadcastedRegistry)
          	broadcastedRegistry match {
          	  case r:List[(String,String,Int)] => initializeRegistry(r)
              case _ => log warning "Received the wrong registry information"
            }
          	
        case Exit(from, target) => 
          	log info "Component "+uri+" exiting by request of "+from+" because "+target
            exit()
            
        case e => 
          	log warning "Received an unknown message "+e+". Discarding it..."
      }
    }
  }
    
  //
  // Initialize the registry
  //
  def initializeRegistry ( registry:List[(String,String,Int)] ) = {
	  for ( (uri,host,port)<-registry if !instances.contains(uri) ) {
	    instances(uri)=select(Node(host,port),Symbol(uri))
	  }
	  //println(instances)
     
    // Prepare the input and output port names if not done before
    if ( outputs.size == 0 ) {
	    val portTranslation = Map({
	    		for (con<-flow.connectors) 
	    			yield if (uri==con.sourceInstance) con.sourceInstanceDataPort -> con.targetInstanceDataPort else ""->"" 
	    }:_*)
	    outputs ++= Map({for ( port<-component.outputs) yield port.name -> portTranslation(port.uri) }:_*)
     }
  }
  
  
  //
  // Process a piece of data
  //
  def process ( port:String, payload:Any ) = {
    log finest component.description.name+" processing data received on port "+port 
//    println(port)
//    println(reverseInputMap)
    inputs(reverseInputMap(port))=payload
//    println(inputs)
    conditionalFiring()
  }
  
  //
  // Push a piece of data to the right actor
  //
  def pushData ( port:String, payload:Any ) = {
//    println("Port:"+port)
//    println(outputMap.toString)
//    println(outputInstancesMap.toString)
//    println(outputPortMap)
    dataPushed += 1L         
    val portUri = outputMap(port)
    val act = instances(outputInstancesMap(portUri))
    act ! Data(outputPortMap(portUri),payload)
  }
  
  //
  // The execute of a component
  //
  def execute = {
    log finest component.description.name+" fired"
    eci execute context
  }
    
  // 
  // Initialize invocation
  //
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
  
 
  //
  // Dispose invocation
  // 
  def dispose = {
    log info component.description.name+" disposing"
    eci dispose context
    reply(Exiting(uri))
    //mrProper ! Exiting(uri)
    //exit(Symbol(uri))
  }
  
  //
  // Other services
  // 
  def requestAbort(msg:String) = {
    log info "Component "+uri+" requresting abort excecution because "+msg
    mrProper ! Abort(uri)
    mrProper ! Exiting(uri)
    //exit(Symbol(uri))
  }
  
  // Start the actor  
  start()
  
}
