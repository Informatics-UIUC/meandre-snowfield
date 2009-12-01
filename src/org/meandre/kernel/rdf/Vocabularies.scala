package org.meandre.kernel.rdf

import com.hp.hpl.jena.rdf.model._

/** This object contain the basic repository vocabulary used by Meandre.
 *
 * @author Xavier Llora
 */
object MeandreRepositoryVocabulary {

	/** The RDF model that holds the vocabulary terms */
	val m_model = ModelFactory.createDefaultModel

	/** The namespace of the vocabulary as a string  */
	val NS = "http://www.meandre.org/ontology/"

	/** The namespace of the vocabulary as a resource */
	val NAMESPACE = m_model.createResource(NS)

	/** The components instances identifier. */
	val components_instances = m_model.createProperty(NS+"components_instances")

	/** The connector instance data port source. */
	val connector_instance_data_port_source = m_model.createProperty(NS+"connector_instance_data_port_source")

	/** The connector instance data port target. */
	val connector_instance_data_port_target = m_model.createProperty(NS+"connector_instance_data_port_target")

	/** The connector instance source. */
	val connector_instance_source = m_model.createProperty(NS+"connector_instance_source")

	/** The connector instance target. */
	val connector_instance_target = m_model.createProperty(NS+"connector_instance_target")

	/** The connector set resource */
	val connector_set = m_model.createResource(NS+"connector_set")

	/** The connector set. */
	val connectors = m_model.createProperty(NS+"connectors")

	/** The data connector type. */
	val data_connector = m_model.createProperty(NS+"data_connector")

	/** The data connector configuration resource */
	val data_connector_configuration = m_model.createResource(NS+"data_connector_configuration")

	/** The data port type */
	val data_port = m_model.createResource(NS+"data_port")

	/** The executable component type */
	val executable_component = m_model.createResource(NS+"executable_component")

	/** The executable component instance */
	val executable_component_instance = m_model.createProperty(NS+"executable_component_instance")

	/** The execution context */
	val execution_context = m_model.createProperty(NS+"execution_context")

	/** The firing policy */
	val firing_policy = m_model.createProperty(NS+"firing_policy")

	/** The flow component type */
	val flow_component = m_model.createResource(NS+"flow_component")

	/** The input data port  */
	val input_data_port = m_model.createProperty(NS+"input_data_port")

	/** The instance configuration type */
	val instance_configuration = m_model.createResource(NS+"instance_configuration")

	/** The instance name  */
	val instance_name = m_model.createProperty(NS+"instance_name")

	/** The instance resource */
	val instance_resource = m_model.createProperty(NS+"instance_resource")

	/** The instance set type */
	val instance_set = m_model.createResource(NS+"instance_set") 

	/** The key of an element */
	val key = m_model.createProperty(NS+"key")

	/** Name of an element */
	val name = m_model.createProperty(NS+"name")

	/** The output data port type  */
	val output_data_port = m_model.createProperty(NS+"output_data_port")

	/** The property type */
	val property = m_model.createResource(NS+"property")

	/** The property set */
	val property_set = m_model.createProperty(NS+"property_set")

	/** The resource location  */
	val resource_location = m_model.createProperty(NS+"resource_location")

	/** The runnable entry value */
	val runnable = m_model.createProperty(NS+"runnable")

	/** The property tag */
	val tag = m_model.createProperty(NS+"tag")

	/** The property value */
	val value = m_model.createProperty(NS+"value")

	/** The mode property */
	val mode = m_model.createProperty(NS+"mode")
}


/** This object contains the basic probing vocabulary for Meandre.
 * 
 * @author Xavier Llora
 *
 */
object MeandreProbingVocabulary {

       /** The RDF model that holds the vocabulary terms */
       val m_model = ModelFactory.createDefaultModel

       /** The namespace of the vocabulary as a string  */
       val NS = "http://www.meandre.org/probing/"

       /** The namespace of the vocabulary as a resource */
       val NAMESPACE = m_model.createResource(NS)

       /** The components instances identifier. */
       val components_instances = m_model.createProperty(NS+"components_instances")

       /** The flow started. */
       val flow_started = m_model.createProperty(NS+"flow_started") 

       /** The flow finished. */
       val flow_finished = m_model.createProperty(NS+"flow_finished") 

       /** The flow aborted. */
       val flow_aborted = m_model.createProperty(NS+"flow_aborted") 

       /** The executable component initialized. */
       val executable_component_instance_initialized = m_model.createProperty(NS+"executable_component_instance_initialized") 

       /** The executable component initialized. */
       val executable_component_instance_aborted = m_model.createProperty(NS+"executable_component_instance_aborted") 

       /** The executable component initialized. */
       val executable_component_instance_disposed = m_model.createProperty(NS+"executable_component_instance_disposed") 

       /** A XML serialized state. */
       val state = m_model.createProperty(NS+"state") 

       /** The executable component instance pushed data. */
       val executable_component_instance_pushed_data = m_model.createProperty(NS+"executable_component_instance_pushed_data") 

       /** A XML serialized data piece. */
       val data_piece = m_model.createProperty(NS+"data_piece") 

       /** The executable component instance pulled data. */
       val executable_component_instance_pulled_data = m_model.createProperty(NS+"executable_component_instance_pulled_data") 

       /** The executable component instance fired. */
       val executable_component_instance_fired = m_model.createProperty(NS+"executable_component_instance_fired") 

       /** The executable component instance is cooling down. */
       val executable_component_instance_cooling_down = m_model.createProperty(NS+"executable_component_instance_cooling_down") 

       /** The executable component instance is cooling down. */
       val executable_component_instance_get_property = m_model.createProperty(NS+"executable_component_instance_get_property") 

}