package org.meandre.kernel.actors

import scala.actors.Actor
import scala.actors.Actor._

import scala.collection.immutable.Queue
import scala.collection.mutable.Map

import org.meandre.core._
import org.meandre.kernel.rdf._
import org.meandre.kernel.logger.LoggerContainer._

/** The base class for all Protocol objects.
 * 
 * @autor Xavier Llora
 */
@serializable abstract class Protocol


/** A component actor is requesting to being registered on a given MrProper
 * 
 * @autor Xavier Llora
 */
@serializable case class Register(uri:String,host:String,port:Int) extends Protocol
/** Initialize the component actor
 * 
 * @autor Xavier Llora
 */
@serializable case class Initialize() extends Protocol
/** Dispose the component actor
 * 
 * @autor Xavier Llora
 */
@serializable case class Dispose() extends Protocol


/** Send data to a component actor 
 * 
 * @autor Xavier Llora
 */
@serializable case class Data(port:String,payload:Any) extends Protocol


/** Request for status of a component actor
 * 
 * @autor Xavier Llora
 */
@serializable case class Status(cmd:String) extends Protocol
/** The response package for a stratus request
 * 
 * @autor Xavier Llora
 */
@serializable case class Message(uri:String,cmd:String,msg:Any) extends Protocol


/** The component just got fired
 * 
 * @autor Xavier Llora
 */
@serializable case class Fired(uri:String) extends Protocol
/** The component finished and is cooling down
 * 
 * @autor Xavier Llora
 */
@serializable case class CoolingDown(uri:String,consumed:Long,pushed:Long) extends Protocol
/** The component is exiting 
 * 
 * @autor Xavier Llora
 */
@serializable case class Exiting(uri:String) extends Protocol
/** The component is requesting to abort flow execution
 * 
 * @autor Xavier Llora
 */
@serializable case class Abort(msg:String) extends Protocol



