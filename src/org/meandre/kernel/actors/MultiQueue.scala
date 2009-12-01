package org.meandre.kernel.actors

import scala.collection.immutable.{Map,Queue}

/** This class implements the basic multiqueue for components. The
 *  multiqueueu contatins as many independent queues as slots provided.
 *  It allows adding elements one at a time, as well as removing one 
 *  elements from each slot queue at once (if empty nothing is removed).
 * 
 * @autor Xavier Llora
 */
@serializable class MultiQueue(val slots:List[String]) {
  
  /** The set of available slots */
  protected val slotsSet = Set(slots:_*)
  
  /** The collections of available queues for the provided slots */
  protected var queues = Map[String,Queue[Any]]({
	  	for ( s<-slots ) yield s -> new Queue[Any]()
  	}:_*)

  /** Check if a slots belongs to the multiqueue.
   * 
   * @param slot The name of the slot to check
   * @return True if the slot belongs to the multiqueue, false otherwise
   */
  def contains ( slot:String ) = slotsSet.contains(slot)
  
  /** Returns a tuple containing the size of each queue. The order is the
   *  same as the provided by the slots list.
   * 
   * @return A tuple with the number of available elments in each of queues
   */
  def size = for ( s<-slots ) yield queues(s).size
  
  /** Returns the number of elements available on the requested slots queue.
   * 
   * @param slot The slot queue to check
   * @return The number of elements on the requested slot queue 
   */
  def availableQueue ( slot:String ) = queues(slot).size>0
  
  /** Returns the number of non empty available queues.
   * 
   * @return The number of non empty available queues
   */
  def available = size.foldLeft(0)( (a,b) => if (b>0) a+1 else a )
  
  /** Returns a tuple containing the elements at the front of the requested 
   *  slots.
   * 
   * @param names The slot queues to check for front
   * @return An array containing the front elements for the requested slots
   */
  def apply(names:String*) =
    for ( slot<-names ) yield if ( queues(slot).isEmpty ) None else Some(queues(slot).front)
  
  /** Returns the updated queue after enqueueing the provide value on the given
   *  slot queue.
   * 
   * @param name The slot queues to update
   * @param value The value to add to the slot
   * @return The multiqueue instance
   */
  def update(name:String,value:Any) = {
    queues = Map({for (kv<-queues) yield if (kv._1==name) kv._1->kv._2.enqueue(value) else kv}.toList:_*)
    this
  }
    
  /** Returns a map containing for each slot the front value on the queue.
   * 
   * @return The map containing the front value (None if empty) for each fo the slots
   */
  def front = 
    Map({for ( kv<-queues ) yield if (kv._2.isEmpty) kv._1 -> None else kv._1 -> Some(kv._2.front)}.toList:_*)
  
  /** Dequeues the front element for each of the queue slots
   * 
   * @return A map containing the front value (None if empty) for each fo the slots that has been dequeue
   */
  def dequeue = {
    val res = front
    queues = Map({for (kv<-queues) yield if (kv._2.isEmpty) kv else kv._1 -> kv._2.dequeue._2}.toList:_*)
    res
  }
    
  /** A readable representation of the MultiQueue.
   * 
   * @return A string representing the status of the multiqueues
   */
  override def toString = "MultiQueue("+slots.foldLeft[String](" ")((a,s) => a+s+"("+queues(s).size+") ")+")"
}
