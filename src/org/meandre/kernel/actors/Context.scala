package org.meandre.kernel.actors

import org.meandre.kernel.logger.LoggerContainer._

import org.meandre.core.ComponentContext
import org.meandre.webui.WebUIFragmentCallback
import org.meandre.plugins.MeandrePlugin

import javax.servlet.http.HttpServletRequest
import java.util.logging.Logger
import java.net.URL
import java.io.PrintStream
import java.io.File

/** The class that implements the component context trait used by the components.
 * 
 * @author Xavier Llora
 */
class Context(instance:ComponentActor ) extends ComponentContext  {

  val inputNames    = Array(instance.inputs.slots.toList:_*)
  val outputNames   = Array(instance.outputs.keys.toList:_*)
  val propertyNames = instance.properties.keySet.toArray
  val queues        = instance.inputs
    
  // -----------------------------------------
  
  def getInitialURLPath (request: HttpServletRequest) : String = request.getRequestURL.toString
  def getInputNames : Array[String] = inputNames
  def getOutputNames : Array[String] = outputNames
  def getPropertyNames : Array[String] = propertyNames
  def getProperty ( key:String ) : String = instance.properties.getOrElse(key,null)
  def getExecutionInstanceID : String = instance.uri
  def getFlowID : String = instance.flow.uri
  
  // ------------------------------------------
  
  def getDataComponentFromInput ( port:String ) : Object = (queues(port)(0)).get.asInstanceOf[Object] 

  def isInputAvailable ( port:String ) : Boolean = queues availableQueue port
  
  def pushDataComponentToOutput ( port:String, obj:Any ):Unit = instance.pushData(port,obj)
  
  def requestFlowAbortion : Unit = instance requestAbort "Abort requested by component "+instance.uri
  
  // TODO: Not implemented yet
  def isFlowAborting : Boolean = false
  
  // TODO: Not implemented yet 
  def getPlugin (id:String) : MeandrePlugin = null

  //
  // ------------ Not working for a bit ------------
  //
  
  // TODO: Not implemented
  def startWebUIFragment ( callback:WebUIFragmentCallback  ) = {}
  
  // TODO: Not implemented
  def stopWebUIFragment ( callback:WebUIFragmentCallback  ) = {}
  
  // TODO: Not implemented
  def stopAllWebUIFragments = {}
  
  // TODO: Not implemented
  def getWebUIUrl ( name:Boolean ) : URL = null
  
  // TODO: Not implemented
  def getProxyWebUIUrl ( name:Boolean ) : URL = null
  
  // TODO: Not implemented
  def getFlowExecutionInstanceID : String = "The unique flow instance id"
  
 
  //
  // ---- Temporarily hardcoded  -----
  //
    
  // TODO: Not implemented
  def getLogger : Logger = log
  
  // TODO: Not implemented
  def getOutputConsole : PrintStream = System.out
  
  // TODO: Not implemented
  def getPublicResourcesDirectory : String = new File(".").getAbsolutePath+File.separator+"public_resources"
  
  // TODO: Not implemented
  def getRunDirectory : String = new File(".").getAbsolutePath+File.separator+"run"
  
  
}
