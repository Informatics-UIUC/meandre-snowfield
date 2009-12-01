package org.meandre.kernel.logger

import java.util.Date
import java.text.SimpleDateFormat
import java.util.logging._

/** Implements the logger formatter used by the Snowfield 
 *  loggers.
 * 
 * @author Xavier Llora
 */
object LoggerFormatter extends Formatter {
  
	/** The system independent new line */
	protected val NEW_LINE = System getProperty "line.separator"
	/** The formating object */
	protected val formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS")
  
	/** Given a log record creates the string version that will be used.
	 * 
	 * @param record The record to format
	 * @return The string representing the formated record 
	 */
	def format(record:LogRecord) = {
		val tail = if ( record.getLevel==Level.SEVERE ) {
                  		"  [" + record.getSourceClassName+","+
                                record.getSourceMethodName+"]"+NEW_LINE
                  }
                  else { NEW_LINE }
		formatter.format(new Date(record.getMillis()))+"::"+
        		  record.getLevel+":  "+
                  record.getMessage+
                  tail
	}
}

/** The global Snowfield logger container.
 * 
 * @author Xavier Llora
 */
object LoggerContainer {
	
	val log = Logger.getLogger("[EL]")
	log setLevel Level.INFO 

	protected val parentLogger=log.getParent();
    for ( h<-parentLogger.getHandlers if parentLogger!=null ) h.setFormatter(LoggerFormatter) 
    for ( h<-log.getHandlers ) h.setFormatter(LoggerFormatter)  
	
 }
