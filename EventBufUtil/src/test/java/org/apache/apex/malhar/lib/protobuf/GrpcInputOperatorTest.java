package org.apache.apex.malhar.lib.protobuf;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.datatorrent.lib.testbench.CollectorTestSink;
import com.ge.predix.eventhub.SubscriberGrpc;
import com.ge.predix.eventhub.SubscriberGrpc.SubscriberBlockingStub;
import com.ge.predix.eventhub.SubscriptionRequest;
import com.google.protobuf.ByteString;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Functional test for {
 *
 * @link GrpcInputOperator }.
 *
 */
public class GrpcInputOperatorTest
{

  GrpcInputOperator grpcOpr;
  SubscriberBlockingStub stub;
  Channel channel;
  SubscriberService service;
  
  
  private static class SubscriberService extends SubscriberGrpc.SubscriberImplBase
  {
    Server server;
    
    public SubscriberService(int port) {
      server = ServerBuilder.forPort(port).addService(this).build();
    }
    
    public void start() throws IOException {
      server.start();
    }
    
    public void shutdown() {
      server.shutdown();
    }
    
    @Override
    public void receive(com.ge.predix.eventhub.SubscriptionRequest request,
        io.grpc.stub.StreamObserver<com.ge.predix.eventhub.Message> responseObserver) {
      
      Assert.assertEquals("subscriber", request.getSubscriber());
      Assert.assertEquals("alien123", request.getInstanceId());
      Assert.assertEquals("a4f435ec-2d3e-4afd-a8f7-2912266a1597", request.getZoneId());
      
      com.ge.predix.eventhub.Message.Builder builder 
        = com.ge.predix.eventhub.Message.newBuilder();
      
      builder.setId("id1");
      builder.setZoneId("zoneid1");
      builder.setBody(ByteString.copyFrom("body1".getBytes()));
      builder.putTags("tagkey1", "tagvalue1");
      builder.putTags("tagkey2", "tagvalue2");
      responseObserver.onNext(builder.build());

      builder 
        = com.ge.predix.eventhub.Message.newBuilder();

      builder.setId("id2");
      builder.setZoneId("zoneid2");
      builder.setBody(ByteString.copyFrom("body2".getBytes()));
      builder.putTags("tagkey3", "tagvalue3");
      builder.putTags("tagkey4", "tagvalue4");
      responseObserver.onNext(builder.build());
      
      responseObserver.onCompleted();
    }
  }
  
  @Test
  public void testGrpcInputOperator() throws InterruptedException, IOException
  {
    service = new SubscriberService(9090);
    service.start();
    
    grpcOpr = new GrpcInputOperator();
    
    channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext(true).build();
    stub = SubscriberGrpc.newBlockingStub(channel);
    
    grpcOpr.setStub(stub);
    grpcOpr.setMethod(com.ge.predix.eventhub.SubscriberGrpc.METHOD_RECEIVE);
    grpcOpr.setRequest(createSubscriptionRequest());
    
    
    CollectorTestSink<Object> sink = 
        new CollectorTestSink<Object>();
    grpcOpr.outputPort.setSink(sink);

    grpcOpr.setup(null);
    grpcOpr.activate(null);

    int timeoutMillis = 3000;
    while (sink.collectedTuples.isEmpty() && timeoutMillis > 0) {
      grpcOpr.emitTuples();
      timeoutMillis -= 20;
      Thread.sleep(20);
    }

    Assert.assertEquals("tuple emitted", 2, sink.collectedTuples.size());

    grpcOpr.deactivate();
    grpcOpr.teardown();
    service.shutdown();
  }


  private SubscriptionRequest createSubscriptionRequest()
  {
    SubscriptionRequest.Builder builder = SubscriptionRequest.newBuilder();
    builder.setSubscriber("subscriber");
    builder.setInstanceId("alien123");
    builder.setZoneId("a4f435ec-2d3e-4afd-a8f7-2912266a1597");
    return builder.build();
  }

}
