package demo.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 几个问题：
 * 1.Netty的事件通知机制是如何实现的？如何保证调用过程中请求和响应的一一对应？
 * 2.Netty使用了哪些设计模式？分别有什么好处？
 * 3.Netty的线程模型是怎样的的？如何保证线程安全？
 * 4.Dubbo是如何集成Netty的？
 * 其他问题：
 * BIO、NIO 和 AIO 的区别？
 * NIO 的组成？
 * Netty 的特点？
 * Netty 的线程模型？
 * TCP 粘包/拆包的原因及解决方法？
 * 了解哪几种序列化协议？
 * 如何选择序列化协议？
 * Netty 的零拷贝实现？
 * Netty 的高性能表现在哪些方面？
 * NIOEventLoopGroup 源码？
 */
public class TimeServer {

    private int port;

    public TimeServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        new TimeServer(8180).run();
    }

    public void run() throws Exception {
        //mainReactor
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //subReactor
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //build模式
            ServerBootstrap b = new ServerBootstrap();
            //设置主从线程池
            b.group(bossGroup, workerGroup)
                    //根据Class创建Factory用于生产Channel实例
                    .channel(NioServerSocketChannel.class)
                    //设置ChannelHandler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TimeEncoder(), new TimeServerHandler());
                        }
                    })
                    //为MainChannel设置参数列表
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //为SubChannel设置参数列表
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            Thread.sleep(1000);
            f.channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("测试");
                }
            });

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
