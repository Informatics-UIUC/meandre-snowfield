package org.meandre.kernel.logger

import java.util.Date
import java.text.SimpleDateFormat
import java.util.logging._

object LoggerFormatter extends Formatter {
  
	val NEW_LINE = System getProperty "line.separator"
	val formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS")
  
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

object LoggerContainer {
	
	val log = Logger.getLogger("[EL]")
	log setLevel Level.INFO 

	val parentLogger=log.getParent();
    for ( h<-parentLogger.getHandlers if parentLogger!=null ) h.setFormatter(LoggerFormatter) 
    for ( h<-log.getHandlers ) h.setFormatter(LoggerFormatter)  
	
 }
