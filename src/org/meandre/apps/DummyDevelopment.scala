package org.meandre.apps

import java.io.File
import com.hp.hpl.jena.rdf.model._

import org.meandre.kernel.rdf._
import org.meandre.kernel.actors._

/** This singleton it is just for development testing purposes. 
 * 
 * @author Xavier Llora
 */
object DummyDevelopment {
  
	val url = new File("test-data"+File.separator+"test_repository.rdf").toURL.toString
  
	def main(args : Array[String]) : Unit = {
		
		import org.meandre.kernel.logger.LoggerContainer._
        import java.util.logging.Level
    	val logLevel = log.getLevel
    	log.setLevel(Level.FINEST)
    	
    	println("Retrieving repository "+url)
    	val components = DescriptorsFactory(url)
    	val flows = DescriptorsFactory(url)
     
    	//new MrProperActor(flows.filter(_.uri=="meandre://test.org/blah/blah/simple-test/")(0),components)
    	// TODO Finish the testing
    	println("************** Sleeping")
    	Thread.sleep(1000)
    	println("************** Awake")
    	//new MrProperActor(flows.filter(_.uri!="meandre://test.org/blah/blah/simple-test/")(0),components)
    	//Thread.sleep(10000)
    	
	}
}
