/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.uptime;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ClientBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineFactory;
import io.netty.channel.Channels;
import io.netty.channel.socket.nio.NioClientSocketChannelFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;


/**
 * Connects to a server periodically to measure and print the uptime of the
 * server.  This example demonstrates how to implement reliable reconnection
 * mechanism in Netty.
 */
public class UptimeClient {

    // Sleep 5 seconds before a reconnection attempt.
    static final int RECONNECT_DELAY = 5;

    // Reconnect when the server sends nothing for 10 seconds.
    private static final int READ_TIMEOUT = 10;

    public static void main(String[] args) throws Exception {
        // Print usage if no argument is specified.
        if (args.length != 2) {
            System.err.println(
                    "Usage: " + UptimeClient.class.getSimpleName() +
                    " <host> <port>");
            return;
        }

        // Parse options.
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // Initialize the timer that schedules subsequent reconnection attempts.
        final Timer timer = new HashedWheelTimer();

        // Configure the client.
        final ClientBootstrap bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Configure the pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            private final ChannelHandler timeoutHandler =
                new ReadTimeoutHandler(timer, READ_TIMEOUT);
            private final ChannelHandler uptimeHandler =
                new UptimeClientHandler(bootstrap, timer);

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        timeoutHandler, uptimeHandler);
            }
        });

        bootstrap.setOption(
                "remoteAddress", new InetSocketAddress(host, port));

        // Initiate the first connection attempt - the rest is handled by
        // UptimeClientHandler.
        bootstrap.connect();
    }
}
