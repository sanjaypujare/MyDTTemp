/**
 * 
 */
package org.apache.apex.malhar.lib.protobuf;

import javax.validation.constraints.NotNull;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.lib.parser.Parser;
import com.datatorrent.netlet.util.Slice;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 *
 */
public class ProtobufProcessor<T extends GeneratedMessage> extends Parser<Slice, String> {

  @NotNull
  private com.google.protobuf.Parser<T> parser;
  
  
  public T convert(Slice tuple)
  {
    try {
      return parser.parsePartialFrom(tuple.buffer, tuple.offset, tuple.length);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  public void setup(OperatorContext context)
  {
    
  }

  public void teardown()
  {
    
  }

  public void beginWindow(long windowId)
  {
    // TODO Auto-generated method stub
    
  }

  public void endWindow()
  {
    
  }

  /**
   * @param parser the parser to set
   */
  public void setParser(com.google.protobuf.Parser<T> parser)
  {
    this.parser = parser;
  }

  @Override
  public String processErrorTuple(Slice input)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
}
