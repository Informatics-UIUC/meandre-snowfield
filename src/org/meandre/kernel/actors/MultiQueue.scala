package org.meandre.kernel.actors

import scala.collection.immutable.{Map,Queue}

/** This class implements the basic multique for components */
@serializable class MultiQueue(val slots:List[String]) {
  
  protected val slotsSet = Set(slots:_*)
  
  protected var queues = Map[String,Queue[Any]]({
	  	for ( s<-slots ) yield s -> new Queue[Any]()
  	}:_*)

  def contains ( slot:String ) = slotsSet.contains(slot)
  
  def size = for ( s<-slots ) yield queues(s).size
  
  def availableQueue ( slot:String ) = queues(slot).size>0
  
  def available = size.foldLeft(0)( (a,b) => if (b>0) a+1 else a )
  
  def apply(names:String*) =
    for ( slot<-names ) yield if ( queues(slot).isEmpty ) None else Some(queues(slot).front)
  
  def update(name:String,value:Any) = {
    queues = Map({for (kv<-queues) yield if (kv._1==name) kv._1->kv._2.enqueue(value) else kv}.toList:_*)
    this
  }
    
  def front = 
    Map({for ( kv<-queues ) yield if (kv._2.isEmpty) kv._1 -> None else kv._1 -> Some(kv._2.front)}.toList:_*)
  
  def dequeue = {
    val res = front
    queues = Map({for (kv<-queues) yield if (kv._2.isEmpty) kv else kv._1 -> kv._2.dequeue._2}.toList:_*)
    res
  }
    
  override def toString = "MultiQueue("+slots.foldLeft[String](" ")((a,s) => a+s+"("+queues(s).size+") ")+")"
}
