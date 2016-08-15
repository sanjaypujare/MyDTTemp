/**
 * 
 */
package org.apache.apex.malhar.lib.io;


import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datatorrent.lib.io.SimpleSinglePortInputOperator;
import com.google.protobuf.GeneratedMessageV3;

import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.ClientCalls;

/**
 * This is a base implementation for a Grpc input operator that allows the caller to supply a request
 * (an instance of GeneratedMessageV3) and the operator uses gRPC to get a stream of responses for
 * that request. Subclasses must provide instances of blocking stub, method-descriptor and the 
 * actual request. 
 * <p></p>
 * @displayName Abstract Grpc Input
 * @category Input
 * @tags http, input operator
 * 
 */
public abstract class AbstractGrpcInputOperator<ReqT extends GeneratedMessageV3, ResT extends GeneratedMessageV3> 
  extends SimpleSinglePortInputOperator<ResT> implements Runnable
{
  
  private static final Logger LOG = LoggerFactory.getLogger(AbstractGrpcInputOperator.class);
  
  private AbstractStub<?> stub;
  private MethodDescriptor<ReqT, ResT> method;
  private ReqT request;

  /**
   * @return the stub
   */
  public AbstractStub<?> getStub()
  {
    return stub;
  }

  /**
   * @param stub the stub to set
   */
  public void setStub(AbstractStub<?> stub)
  {
    this.stub = stub;
  }

  /**
   * @return the method
   */
  public MethodDescriptor<ReqT, ResT> getMethod()
  {
    return method;
  }

  /**
   * @param method the method to set
   */
  public void setMethod(MethodDescriptor<ReqT, ResT> method)
  {
    this.method = method;
  }

  /**
   * @return the request
   */
  public ReqT getRequest()
  {
    return request;
  }

  /**
   * @param request the request to set
   */
  public void setRequest(ReqT request)
  {
    this.request = request;
  }

  /**
   * generate 
   */
  @Override
  public void run()
  {
    while (super.isActive()) {

      try {
        Iterator<ResT> resIterator = ClientCalls.blockingServerStreamingCall(
            stub.getChannel(), method, stub.getCallOptions(), request);

        while (super.isActive() && resIterator.hasNext()) {
          ResT m = resIterator.next();

          outputPort.emit(m);
        }
      } catch (Exception e) {
        LOG.error("Error sending request " + request, e);
      }

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        LOG.info("Exiting IO loop {}.", e.toString());
        break;
      }
    }

  }

}
