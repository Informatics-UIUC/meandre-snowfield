package org.meandre.kernel.actors

import scala.actors.Actor
import scala.actors.Actor._

import scala.collection.immutable.Queue
import scala.collection.mutable.Map

import org.meandre.core._
import org.meandre.kernel.rdf._
import org.meandre.kernel.logger.LoggerContainer._

@serializable abstract class Protocol

// A component actor is requesting to being registered on a given MrProper
@serializable case class Register(uri:String,host:String,port:Int) extends Protocol
// Initialize the component actor
@serializable case class Initialize() extends Protocol
// Dispose the component actor
@serializable case class Dispose() extends Protocol

// Send data to a component actor 
@serializable case class Data(port:String,payload:Any) extends Protocol

// Request for status of a component actor
@serializable case class Status(cmd:String) extends Protocol
// The response package
@serializable case class Message(uri:String,cmd:String,msg:Any) extends Protocol

// The component just got fired
@serializable case class Fired(uri:String) extends Protocol
// The component finished and is cooling down
@serializable case class CoolingDown(uri:String,consumed:Long,pushed:Long) extends Protocol
// The component is exiting 
@serializable case class Exiting(uri:String) extends Protocol
// The component is requesting to abort flow execution
@serializable case class Abort(msg:String) extends Protocol



