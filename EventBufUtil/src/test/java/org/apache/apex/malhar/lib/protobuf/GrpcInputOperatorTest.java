package org.apache.apex.malhar.lib.protobuf;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.datatorrent.lib.testbench.CollectorTestSink;
import com.ge.predix.eventhub.HeaderClientInterceptor;
import com.ge.predix.eventhub.SubscriberGrpc;
import com.ge.predix.eventhub.SubscriberGrpc.SubscriberBlockingStub;
import com.ge.predix.eventhub.SubscriptionRequest;
import com.google.protobuf.ByteString;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

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
  HeaderServerInterceptor serverInterceptor;
  
  
  private class SubscriberService extends SubscriberGrpc.SubscriberImplBase
  {
    Server server;
    boolean sendResponse = true;  // to pass unit test send the response only once
    
    public SubscriberService(int port) {
      serverInterceptor = new HeaderServerInterceptor();
      
      ServerServiceDefinition serviceDef = ServerInterceptors.intercept(this, serverInterceptor);
      server = ServerBuilder.forPort(port).addService(serviceDef).build();
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

      if (sendResponse) {
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
        sendResponse = false;
      }

      responseObserver.onCompleted();
    }
  }
  
  class HeaderServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
        ServerCallHandler<ReqT, RespT> next)
    { 
      /* 
       * Check headers
       * {:authority=[localhost:9090], 
       * :path=[/predix.eventhub.Subscriber/receive], 
       * :method=[POST], 
       * :scheme=[http], 
       * content-type=[application/grpc, application/grpc], 
       * te=[trailers], 
       * user-agent=[grpc-java-netty/0.15.0], 
       * predix-zone-id=[2b54e5f9-f91b-4f1a-bcaa-bc2a174dec53], 
       * authorization=[eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI0NzljMjk2ZS02M2Q1LTQ5MTUtYWYzMC0yMGU1MmVmMjc0MDYiLCJzdWIiOiJldmVudC1odWItYXV0aC10ZXN0Iiwic2NvcGUiOlsiZXZlbnQtaHViLnpvbmVzLjJiNTRlNWY5LWY5MWItNGYxYS1iY2FhLWJjMmExNzRkZWM1My51c2VyIiwidWFhLnJlc291cmNlIiwiZXZlbnQtaHViLnpvbmVzLjJiNTRlNWY5LWY5MWItNGYxYS1iY2FhLWJjMmExNzRkZWM1My5ncnBjLnB1Ymxpc2giLCJzdWJzY3JpYmUiLCJldmVudC1odWIuem9uZXMuMmI1NGU1ZjktZjkxYi00ZjFhLWJjYWEtYmMyYTE3NGRlYzUzLndzcy5wdWJsaXNoIl0sImNsaWVudF9pZCI6ImV2ZW50LWh1Yi1hdXRoLXRlc3QiLCJjaWQiOiJldmVudC1odWItYXV0aC10ZXN0IiwiYXpwIjoiZXZlbnQtaHViLWF1dGgtdGVzdCIsImdyYW50X3R5cGUiOiJjbGllbnRfY3JlZGVudGlhbHMiLCJyZXZfc2lnIjoiNTc2OTAwYmMiLCJpYXQiOjE0NzAyNjY4NjYsImV4cCI6MTQ3MDMxMDA2NiwiaXNzIjoiaHR0cHM6Ly9wcmVkaXgtZGF0YS5wcmVkaXgtdWFhLXN0YWdpbmcuZ3JjLWFwcHMuc3ZjLmljZS5nZS5jb20vb2F1dGgvdG9rZW4iLCJ6aWQiOiJwcmVkaXgtZGF0YSIsImF1ZCI6WyJldmVudC1odWItYXV0aC10ZXN0IiwiZXZlbnQtaHViLnpvbmVzLjJiNTRlNWY5LWY5MWItNGYxYS1iY2FhLWJjMmExNzRkZWM1MyIsInVhYSIsImV2ZW50LWh1Yi56b25lcy4yYjU0ZTVmOS1mOTFiLTRmMWEtYmNhYS1iYzJhMTc0ZGVjNTMuZ3JwYyIsImV2ZW50LWh1Yi56b25lcy4yYjU0ZTVmOS1mOTFiLTRmMWEtYmNhYS1iYzJhMTc0ZGVjNTMud3NzIl19.RicfsBKk816Q48KrxEDT-C7nITEtdPnMw1QERLgfsHF-nTLHUY3vU882OZfCWeY-_J2RLc0CToDPrIvvbIhym_W9VUtIeCff678SXpSHLw8ybonH48ci-ngKBFd4krNCIqvsbVF9RcKD8N4jlNVp1CH3y0DrXxUq9UGUQCCEjWqGhlaDPgZNIQ2WRbd7RFr72Bjzfsg5NGuRbjtA9r3i1rpzXDr2ka1dyAftkEkRC0B0kKIF9rQGgAd1HOKu_auCemhq9D2IAg_GnQVJHJNJH6SJUyTaMvQ-dUv5mlgzdlm1Rb43XUQaqQn85PS5LrNci9WqW4ImT1wDXqINVENL_Q], 
       * grpc-accept-encoding=[gzip]
       * })
       */
      Assert.assertEquals("2b54e5f9-f91b-4f1a-bcaa-bc2a174dec53", headers.get(HeaderClientInterceptor.zoneIdHeaderKey));
      Assert.assertEquals("eyJhbGciOiJSUz", 
          headers.get(HeaderClientInterceptor.authorizationHeaderKey).substring(0, 14));
      Assert.assertEquals("application/grpc", 
          headers.get(HeaderClientInterceptor.contentTypeHeaderKey));
      return next.startCall(call,headers);
    }
    
  }
  
  @Test
  public void testGrpcInputOperator() throws InterruptedException, IOException
  {
    service = new SubscriberService(9090);

    service.start();
    
    grpcOpr = new GrpcInputOperator();
    
    channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext(true).build();
    
    // intercept headers for channel                                                                                                                                                       
    ClientInterceptor interceptor = new HeaderClientInterceptor();
    channel = ClientInterceptors.intercept(channel, interceptor);
    
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
