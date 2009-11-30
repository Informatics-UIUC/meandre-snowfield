package org.meandre.kernel.rdf

import java.util.Date

import java.text._
import com.hp.hpl.jena.rdf.model.{Property=>JProperty}
import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.vocabulary._
import com.hp.hpl.jena.vocabulary.RDF.`type`

case class Context ()
case class URIContext ( uri: String ) extends Context
case class EmbeddedContext ( payload: String ) extends Context

case class Property ( val key: String, val value: String, val other: Map[String,String] )
case class PropertyDescription ( 
		override val key: String, 
		override val value: String, 
		val description:Option[String],
		override val other: Map[String,String]
) extends Property(key,value,other)

case class Port (
		uri: String,
		name: String,
		description: Option[String]
)

case class CommonDescription (
		name: String,
		description: Option[String],
		creator: Option[String],
		creationDate: Date,
		rights: Option[String],
		tags: List[String]
)

case class FiringPolicy()
case class FiringAll() extends FiringPolicy
case class FiringAny() extends FiringPolicy

case class ConnectorDescription (
		sourceInstanceDataPort: String,
		targetInstanceDataPort: String,
		sourceInstance: String,
		targetInstance: String
) 

case class ComponentInstanceDescription (
		uri: String,
		name: String,
		description: Option[String],
		componentUri: String,
		properties: Map[String,Property]
) 


case class Descriptor(
		uri: String,
		description: CommonDescription,
		properties: Map[String,Property]
)
case class ComponentDescriptor (
		override val uri: String,
		override val description: CommonDescription,
		override val properties: Map[String,Property],
		runnable: String,
		format: String,
		firingPolicy: FiringPolicy,
		resourceLocation: String,
		context: List[Context],
		inputs: List[Port],
		outputs: List[Port]
)
extends Descriptor(uri,description,properties)

case class FlowDescriptor (
		override val uri: String,
		override val description: CommonDescription,
		override val properties: Map[String,Property],
		instances: List[ComponentInstanceDescription],
		connectors: List[ConnectorDescription]
)
extends Descriptor(uri,description,properties)

object DescriptorFactory  {
 
	private def readModel ( url:String ) = {
		val model = ModelFactory.createDefaultModel
		try {
			model.read(url.toString)
		} catch  {
			case _ => try { model.read(url.toString,"TTL") } 
                  catch {
                  		case _ => try { model.read(url.toString,"N-TRIPLE") } 
                  				  finally { model }
                  }
                  finally { model }
		}
		finally { model }
	}
  
	private def safeGet ( uri: Resource, prop: JProperty, model: Model ) : Option[String] = {
		val objs = model.listObjectsOfProperty(uri,prop)
		if ( !objs.hasNext ) None else objs.nextNode match {
		  case s:Resource => Some(s.toString)
		  case s:Literal => Some(s.getString)
		}
	}
 
	private def getMultivalueProperty ( uri: Resource, prop: JProperty, model: Model ) : List[RDFNode] = {
		var res: List[RDFNode] = Nil
		val objs = model.listObjectsOfProperty(uri,prop)
		while ( objs.hasNext ) res ::= objs.nextNode
		res
	}
 
	private def getStatementWithSubject ( uri: Resource ) : List[Statement] = {
		var res: List[Statement] = Nil
		val objs = uri.listProperties()
		while ( objs.hasNext ) res ::= objs.nextStatement
		res
	}
 
	private def rdfNodesToString ( lst: List[RDFNode] ) : List[String] = {
		lst.map((s) =>  s match {
		  case s:Resource => s.toString
		  case s:Literal => s.getString
		})
	}

	private def mustGet ( uri: Resource, prop: JProperty, model: Model ) : String = {
		safeGet(uri,prop,model) match {
		  case None => throw new Exception("The property "+prop+" is required")
		  case Some(s) => s 
		}
	}
 
	private val dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
	private def stringToDate ( string:String ) : Date =  dateFormater.parse(string)	

	private def getCommonDescription ( uri: Resource, model: Model ) : CommonDescription = {
		new CommonDescription(
				mustGet(uri,MeandreRepositoryVocabulary.name,model),
				safeGet(uri,DC_11.description,model),
				safeGet(uri,DC_11.creator,model),
				stringToDate(mustGet(uri,DC_11.date,model)),
				safeGet(uri,DC_11.rights,model),
				rdfNodesToString(getMultivalueProperty(uri,MeandreRepositoryVocabulary.tag,model))
		)
	}

	private def getProperties ( uri:Resource, model:Model )  = {
	  
	  def getPairs ( node:Resource ) = 
		  for (stm<-getStatementWithSubject(node)  ) 
			  yield if (stm.getObject.isLiteral) stm.getPredicate.toString ->  stm.getLiteral.getString
                    else stm.getPredicate.toString -> stm.getObject.toString
	  
	  val lm = for { node <- getMultivalueProperty ( uri, MeandreRepositoryVocabulary.property_set, model ) if node.isResource }
	  	  		  yield node match {
	  	  		    	case node:Resource => Map(getPairs(node):_*)
	  	  		  }
      lm.map( (m:Map[String,String]) => PropertyDescription (
    		  		 m.get(MeandreRepositoryVocabulary.key.toString).getOrElse("unknow"),
    		  		 m.get(MeandreRepositoryVocabulary.value.toString).getOrElse("unknow"),
    		  		 m.get(DC_11.description.toString),
    		  		 m - MeandreRepositoryVocabulary.key.toString
    		  		   - MeandreRepositoryVocabulary.value.toString 
    		  		   - DC_11.description.toString 
    		  		   - RDF.`type`.toString
      ))
      
    }
 
	private def getDataPortForType ( uri:Resource, portType: JProperty, model: Model ) = 
		for ( propURI <- getMultivalueProperty(uri,portType,model ) if propURI.isResource ) 
			yield propURI match {
			  case portURI:Resource => Port(
					  portURI.toString,
					  safeGet(portURI,MeandreRepositoryVocabulary.name,model).getOrElse("unknow"),
					  safeGet(portURI,DC_11.description,model)
			  	)
		}
 
	private def buildComponentDescriptorsModel ( model:Model )   = {
		var res: List[ComponentDescriptor] = Nil
		val uris =  model.listSubjectsWithProperty(`type`, MeandreRepositoryVocabulary.executable_component)
		while ( uris.hasNext ) {
			val uri = uris.nextResource
			res ::= ComponentDescriptor (
					uri.toString,
					getCommonDescription(uri,model),
					Map( getProperties(uri,model) map { s:PropertyDescription=>(s.key,s) } :_* ),
					mustGet(uri,MeandreRepositoryVocabulary.runnable,model).toLowerCase,
					mustGet(uri,DC_11.format,model).toLowerCase,
					if (mustGet(uri,MeandreRepositoryVocabulary.firing_policy,model).toLowerCase=="any") FiringAny() else FiringAll(),
					mustGet(uri,MeandreRepositoryVocabulary.resource_location,model),
					getMultivalueProperty(uri,MeandreRepositoryVocabulary.execution_context,model).map(
					  (s) => s match {
					  		case s:Resource => URIContext(s.toString)
					  		case s:Literal => EmbeddedContext(s.getLexicalForm)
					  }
					),
					getDataPortForType(uri,MeandreRepositoryVocabulary.input_data_port,model),
					getDataPortForType(uri,MeandreRepositoryVocabulary.output_data_port,model)
			)
		}
		res
	}
 
	def buildComponentDescriptors ( url: String ) = buildComponentDescriptorsModel(readModel(url))

	private def getConnectors ( uri:Resource, model:Model ) = {
	  for ( conURI <- getMultivalueProperty(
			  				uri.getProperty(MeandreRepositoryVocabulary.connectors).getResource(),
			  				MeandreRepositoryVocabulary.data_connector, model ) 
	  		if conURI.isResource )
		  yield conURI match {
			  case conURI:Resource => ConnectorDescription(
					  safeGet(conURI,MeandreRepositoryVocabulary.connector_instance_data_port_source,model).get,
					  safeGet(conURI,MeandreRepositoryVocabulary.connector_instance_data_port_target,model).get,
					  safeGet(conURI,MeandreRepositoryVocabulary.connector_instance_source,model).get,
					  safeGet(conURI,MeandreRepositoryVocabulary.connector_instance_target,model).get
			  	)
		}
	}
   
 
	private def getInstanceProperties ( uri:Resource, model:Model )  = {
	  
	  def getPairs ( node:Resource ) = 
		  for (stm<-getStatementWithSubject(node)  ) 
			  yield if (stm.getObject.isLiteral) stm.getPredicate.toString ->  stm.getLiteral.getString
                    else stm.getPredicate.toString -> stm.getObject.toString
	  
      val lm = for { node <- getMultivalueProperty ( uri, MeandreRepositoryVocabulary.property_set, model ) if node.isResource }
	  	  		  yield node match {
	  	  		    	case node:Resource => Map(getPairs(node):_*)
	  	  		  }
          
      lm.map( (m:Map[String,String]) => Property (
    		  		 m.get(MeandreRepositoryVocabulary.key.toString).getOrElse("unknow"),
    		  		 m.get(MeandreRepositoryVocabulary.value.toString).getOrElse("unknow"),
    		  		 m - MeandreRepositoryVocabulary.key.toString
    		  		   - MeandreRepositoryVocabulary.value.toString 
    		  		   - RDF.`type`.toString
      ))
      
    }
 
 	def getInstances ( uri:Resource, model:Model ) = {
	  for ( insURI <- getMultivalueProperty(
			  				uri.getProperty(MeandreRepositoryVocabulary.components_instances).getResource(),
			  				MeandreRepositoryVocabulary.executable_component_instance, model ) 
	  		if insURI.isResource )
		  yield insURI match {
			  case insURI:Resource => ComponentInstanceDescription(
					  insURI.toString,
					  safeGet(insURI,MeandreRepositoryVocabulary.instance_name,model).get,
					  safeGet(insURI,DC_11.description,model),
					  safeGet(insURI,MeandreRepositoryVocabulary.instance_resource,model).get,
					  Map( getInstanceProperties(insURI,model) map { s:Property=>(s.key,s) } :_* )
			  	)
		}
	}
   
	private def buildFlowDescriptorsModel ( model: Model )   = {
	  var res: List[FlowDescriptor] = Nil
		val uris =  model.listSubjectsWithProperty(`type`, MeandreRepositoryVocabulary.flow_component)
		while ( uris.hasNext ) {
			val uri = uris.nextResource
			res ::= FlowDescriptor (
					uri.toString,
					getCommonDescription(uri,model),
					Map( getProperties(uri,model) map { s:PropertyDescription=>(s.key,s) } :_* ),
					getInstances(uri,model),
					getConnectors(uri,model)
			)
		}
		res
	}
 
	def buildFlowDescriptors ( url: String ) = buildFlowDescriptorsModel(readModel(url))
	  

	def buildDescriptors ( url:String ) : List[Descriptor] = {
		val model = readModel(url)
		buildComponentDescriptorsModel(model)++buildFlowDescriptorsModel(model)
	}
}

