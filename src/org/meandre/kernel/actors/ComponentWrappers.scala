package org.meandre.kernel.actors

import org.meandre.core._
import org.meandre.core.environments.lisp.clojure.ClojureExecutableComponentAdapter;
import org.meandre.core.environments.python.jython.JythonExecutableComponentAdapter;

import com.hp.hpl.jena.rdf.model._
import org.meandre.kernel.rdf.{ComponentDescriptor,EmbeddedContext}
import org.meandre.kernel.logger.LoggerContainer._

class JavaWrapper(uri:String, component:ComponentDescriptor, actor:ComponentActor ) extends ExecutableComponent {
  
  val className = List.fromString(component.resourceLocation,'/').last
  var ecw:ExecutableComponent = null
  
  try {
    Class.forName(className).newInstance match {
      case e:ExecutableComponent => ecw = e
    }
  }
  catch {
    case _ => val msg = "Could not load class "+className+" for component instance "+uri
    		  log warning msg
       		  actor requestAbort msg
  }
  
  def initialize ( ccp:ComponentContextProperties ) =  ecw initialize ccp
  def execute ( cc: ComponentContext ) = ecw execute cc
  def dispose ( ccp:ComponentContextProperties ) = ecw dispose ccp
	
} 

class PythonWrapper (uri:String, component:ComponentDescriptor, actor:ComponentActor )  extends JythonExecutableComponentAdapter {
  for ( ctx <- component.context ) ctx match {
    case EmbeddedContext(payload:Literal) => process(payload.getLexicalForm)
    case _ =>
  }
} 

class LispWrapper (uri:String, component:ComponentDescriptor, actor:ComponentActor )  extends ClojureExecutableComponentAdapter {
  for ( ctx <- component.context ) ctx match {
    case EmbeddedContext(payload:Literal) => process(payload.getLexicalForm)
    case _ =>
  }
} 
