/*
 * Copyright (c) 2017 int08h LLC. All rights reserved.
 *
 * int08h LLC licenses Nearenough (the "Software") to you under the Apache License, version 2.0
 * (the "License"); you may not use this Software except in compliance with the License. You may
 * obtain a copy of the License from the LICENSE file included with the Software or at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nearenough.examples;

import static nearenough.util.BytesUtil.hexToBytes;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;
import java.time.Instant;
import nearenough.client.RoughtimeClient;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtWire;

/**
 * Use Netty to send a request to the given Roughtime server and dump the response (if any)
 */
public final class NettyClient {

  // Hostname and port of the public int08h.com Roughtime server
  private static final String INT08H_SERVER_HOST = "roughtime.int08h.com";
  private static final int INT08H_SERVER_PORT = 2002;

  // Long-term public key of the public int08h.com Roughtime server
  private static final byte[] INT08H_SERVER_PUBKEY = hexToBytes(
      "016e6e0284d24c37c6e4d7d8d5b4e1d3c1949ceaa545bf875616c9dce0c9bec1"
  );

  private static final class RequestHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InetSocketAddress addr;
    private final RoughtimeClient client;

    public RequestHandler(InetSocketAddress addr) {
      this.addr = addr;

      // Creates a new RoughtimeClient.
      // Behind the scenes SecureRandom will be used to generate a unique nonce.
      this.client = new RoughtimeClient(INT08H_SERVER_PUBKEY);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      // Creates the client request
      RtMessage msg = client.createRequest();

      // Encodes the request for network transmission
      ByteBuf encodedMsg = RtWire.toWire(msg);

      // Sends the request to the Roughtime server
      ctx.writeAndFlush(new DatagramPacket(encodedMsg, addr))
          .addListener(new ChannelFutureListener() {
                         @Override
                         public void operationComplete(ChannelFuture future) throws Exception {
                           if (!future.isSuccess()) {
                             System.out.println("Send failed " + future.cause().getMessage());
                           }
                         }
                       });
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
      // A reply from the server has been received

      System.out.printf(
          "Read message of %d bytes from %s:\n", msg.content().readableBytes(), msg.sender()
      );

      // Parse the response
      RtMessage response = new RtMessage(msg.content());
      System.out.println(response);

      // Validate the response. Checks that the message is well-formed, all signatures are valid,
      // and our nonce is present in the response.
      client.processResponse(response);

      if (client.isResponseValid()) {
        // Validation passed, the response is good

        // The "midpoint" is the Roughtime server's reported timestamp (in microseconds). And the
        // "radius" is a span of uncertainty around that midpoint. A Roughtime server asserts that
        // its "true time" lies within the span.
        Instant midpoint = Instant.ofEpochMilli(client.midpoint() / 1_000L);
        int radiusSec = client.radius() / 1_000_000;
        System.out.println("midpoint    : " + midpoint + " (radius " + radiusSec + " sec)");

        // For comparison, also print the local clock. If the midpoint and your local time
        // are widely different, check your local machine's time sync!
        Instant local = Instant.now();
        System.out.println("local clock : " + local);

      } else {
        // Validation failed. Print out the reason why.
        System.out.println("Response INVALID: " + client.invalidResponseCause().getMessage());
      }

      ctx.close().addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture unused) throws Exception {
                                  System.exit(0);
                                }
                              });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      ctx.close();

      if (cause instanceof ReadTimeoutException) {
        System.out.println("No reply received from " + addr);
      } else {
        System.out.println("Unexpected exception: " + cause.getMessage());
        throw new RuntimeException(cause);
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    InetSocketAddress addr = new InetSocketAddress(INT08H_SERVER_HOST, INT08H_SERVER_PORT);

    System.out.printf("Sending request to %s\n", addr);

    // Below is Netty boilerplate for setting-up an event loop and registering a handler
    NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
    Bootstrap bootstrap = new Bootstrap()
        .group(nioEventLoopGroup)
        .remoteAddress(addr)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<NioDatagramChannel>() {
          @Override
          protected void initChannel(NioDatagramChannel ch) {
            ch.pipeline()
                .addLast(new ReadTimeoutHandler(5))
                .addLast(new RequestHandler(addr));
          }
        });

    ChannelFuture connectFuture = bootstrap.connect();
    connectFuture.addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) throws Exception {
                                  if (!future.isSuccess()) {
                                    System.out.println("Connect fail:");
                                    System.out.println(future.cause().getMessage());
                                  }
                                }
                              });

    connectFuture.channel().closeFuture().sync();
    nioEventLoopGroup.shutdownGracefully();
  }
}
