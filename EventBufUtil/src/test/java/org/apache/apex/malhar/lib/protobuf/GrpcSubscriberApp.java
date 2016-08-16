package org.apache.apex.malhar.lib.protobuf;

import java.io.File;

import javax.net.ssl.SSLException;

import org.junit.Assert;

import com.datatorrent.lib.testbench.CollectorTestSink;
import com.ge.predix.eventhub.HeaderClientInterceptor;
import com.ge.predix.eventhub.SubscriberGrpc;
import com.ge.predix.eventhub.SubscriptionRequest;
import com.ge.predix.eventhub.SubscriberGrpc.SubscriberBlockingStub;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;

/**
 * 
 *
 *
 */
public class GrpcSubscriberApp
{

  public static void main(String[] args) throws InterruptedException {
    if (args.length < 2) {
      System.err.println("Usage: GrpcSubscriberApp <hostname> <port> [timeoutMillis] [PEM file location]");
      System.exit(1);
    }
    boolean tlsCase = args.length > 3;
    
    GrpcInputOperator grpcOpr = new GrpcInputOperator();
    
    Channel channel = null;
    
    // establish channel connection
    int port = Integer.parseInt(args[1]);
    if (tlsCase) {
        File TLSFile = new File(args[3]);  // used to be "/Users/218023396/Desktop/OO/eventhub/eventhub-java-poc/src/main/resources/ca.pem"
        try {
            channel = NettyChannelBuilder.forAddress(args[0], port)
                            .sslContext(GrpcSslContexts.forClient().trustManager(TLSFile).build())
                            .build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
    } else {
        channel = ManagedChannelBuilder.forAddress(args[0], port).usePlaintext(true).build();
    }
        
    // intercept headers for channel                                                                                                                                                       
    ClientInterceptor interceptor = new HeaderClientInterceptor();
    channel = ClientInterceptors.intercept(channel, interceptor);
    
    SubscriberBlockingStub stub = SubscriberGrpc.newBlockingStub(channel);
    
    grpcOpr.setStub(stub);
    grpcOpr.setMethod(com.ge.predix.eventhub.SubscriberGrpc.METHOD_RECEIVE);
    grpcOpr.setRequest(createSubscriptionRequest());
    
    
    CollectorTestSink<Object> sink = 
        new CollectorTestSink<Object>();
    grpcOpr.outputPort.setSink(sink);

    grpcOpr.setup(null);
    grpcOpr.activate(null);

    int timeoutMillis = args.length > 2 ? Integer.parseInt(args[2]) : 3000;
    while (sink.collectedTuples.isEmpty() && timeoutMillis > 0) {
      grpcOpr.emitTuples();
      timeoutMillis -= 20;
      Thread.sleep(20);
    }

    Assert.assertEquals("tuple emitted", 2, sink.collectedTuples.size());

    grpcOpr.deactivate();
    grpcOpr.teardown();   
  }
  
  private static SubscriptionRequest createSubscriptionRequest()
  {
    SubscriptionRequest.Builder builder = SubscriptionRequest.newBuilder();
    builder.setSubscriber("subscriber");
    builder.setInstanceId("alien123");
    builder.setZoneId("a4f435ec-2d3e-4afd-a8f7-2912266a1597");
    return builder.build();
  }
}
