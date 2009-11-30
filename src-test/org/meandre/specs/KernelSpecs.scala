package org.meandre.specs

import java.io.File

import org.specs._
import org.meandre.kernel.rdf._
import org.meandre.kernel.actors._

object KernelSpecs extends Specification {
  
  val url = new File("test-data"+File.separator+"test_repository.rdf").toURL.toString
  
  "A description factory" should {
	  "must extract components and flows in "+url in {
	    DescriptorFactory.buildComponentDescriptors(url).size must be equalTo(6)
	    DescriptorFactory.buildFlowDescriptors(url).size must be equalTo(2)
	    DescriptorFactory.buildDescriptors(url).size must be equalTo(8)
	  }
  }
  
  "The descriptors in "+url should {
    
	   val descriptors  = DescriptorFactory.buildDescriptors(url)
	
	  "have a common description and name" in {
	    val names = descriptors.foldLeft(Set[String]())((a:Set[String],b:Descriptor)=>a+b.description.name)
	    names.size must be equalTo(8)
	    names.contains("Push String") must beTrue
	    names.contains("Concatenate Strings") must beTrue
	    names.contains("Print Object") must beTrue
	    names.contains("Fork 2 by reference") must beTrue
	    names.contains("To Uppercase") must beTrue
	    names.contains("Pass Through") must beTrue
	    names.contains("Hello World With Java, Python, and Lisp Components!!!") must beTrue
	  }
   
	  "have 0 or more properties " in {
	    for ( component <- descriptors ) component match {
	      case c:ComponentDescriptor => c.properties.size must beGreaterThanOrEqualTo(0)
	      case _ => 
	    }
	  }
   
	  "have 0 or more ports if they represent a component " in {
	    for ( component <- descriptors ) component match {
	      case c:ComponentDescriptor => c.inputs.size must beGreaterThanOrEqualTo(0)
	      case _ => 
	    }
	  }
   
	  "contain a flow that should have 6 instances and  5 connectors" in {
	    for ( component <- descriptors ) component match {
	      case c:FlowDescriptor if c.instances.size==6 =>  {
	    	  c.instances.size must beEqual(6)
	    	  c.connectors.size must beEqual(5)
	    	 } 
	      case c:FlowDescriptor if c.instances.size==2 =>  {
	    	  c.instances.size must beEqual(2)
	    	  c.connectors.size must beEqual(1)
	    	 }
	      case _ => 
	    }
	  }
   
	  "contain a total of 3 defined properties" in {
	    descriptors.foldLeft(0)(_+_.properties.size) must beEqual(3)
	  }
  }
  
  "Multiqueue instances " should {
    
    "allow to have zero or more slots" in {
      val empty = new MultiQueue(List()) 
      empty.slots.size must be equalTo(0)
      val slots = "a"::"b"::"c"::Nil
      val notEmpty = new MultiQueue(slots)
      notEmpty.slots.size must be equalTo(slots.size)
    }
    
    "allow each slot to have zero or more data elements" in {
      val queue = new MultiQueue("a"::"b"::"c"::Nil)
      queue("a")="1"
      queue("b")="1"
      queue("c")="1"
      for ( s<-queue.size ) s must be equalTo(1) 
      queue("c")="2"
      for ( s<-queue.size ) s must be greaterThan(0) 
      val f = queue.front
      f("a") must be equalTo(Some("1"))
      f("b") must be equalTo(Some("1"))
      f("c") must be equalTo(Some("1"))
      queue.available must be equalTo(3)
    }
    
    "allow unbalance addition and removal" in {
      val queue = new MultiQueue("a"::"b"::"c"::Nil)
      queue.available must be equalTo(0)
      queue("a")="1"
      queue.available must be equalTo(1)
      queue("b")="1"
      queue.available must be equalTo(2)
      queue("c")="1"
      queue.available must be equalTo(3)
      for ( s<-queue.size ) s must be equalTo(1) 
      queue("c")="2"
      for ( s<-queue.size ) s must be greaterThan(0) 
      var f = queue.front
      f("a") must be equalTo(Some("1"))
      f("b") must be equalTo(Some("1"))
      f("c") must be equalTo(Some("1"))
      queue.available must be equalTo(3)
      f = queue.dequeue
      f("a") must be equalTo(Some("1"))
      f("b") must be equalTo(Some("1"))
      f("c") must be equalTo(Some("1"))
      queue.available must be equalTo(1)
      f = queue.front
      f("a") must be equalTo(None)
      f("b") must be equalTo(None)
      f("c") must be equalTo(Some("2"))
      f = queue.dequeue
      queue.available must be equalTo(0)
      f("a") must be equalTo(None)
      f("b") must be equalTo(None)
      f("c") must be equalTo(Some("2"))
      f = queue.front
      f("a") must be equalTo(None)
      f("b") must be equalTo(None)
      f("c") must be equalTo(None)
      for ( s<-queue.size ) s must be equalTo(0) 
    }
    
  }
  
  "MrPropper for the flow in "+url should {
    
    "Create and stop actors " in {
        // Tone down the logger
    	import org.meandre.kernel.logger.LoggerContainer._
        import java.util.logging.Level
    	val logLevel = log.getLevel
    	log.setLevel(Level.WARNING)
    	
    	val components = DescriptorFactory.buildComponentDescriptors(url)
    	val flows = DescriptorFactory.buildFlowDescriptors(url)
    	components.size must be equalTo(6)
    	flows.size must be equalTo(2)
    	//val mrp = new MrProperActor(flows.filter(_.uri=="meandre://test.org/blah/blah/simple-test/")(0),components)
    	// TODO Finish the testing
    	Thread.sleep(1000)
     
        // Set back the logger
    	log.setLevel(logLevel)
    }
    
  }

}
